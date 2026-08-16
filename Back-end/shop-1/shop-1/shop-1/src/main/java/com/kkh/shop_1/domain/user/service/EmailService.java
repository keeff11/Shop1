package com.kkh.shop_1.domain.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    private static final long CODE_TTL_MILLIS = 5 * 60 * 1000L; // 인증코드 유효시간: 5분
    private static final int MAX_VERIFY_ATTEMPTS = 5; // 브루트포스 방지용 시도 횟수 제한
    private static final long VERIFIED_TTL_MILLIS = 30 * 60 * 1000L; // 인증 완료 후 회원가입에 사용 가능한 시간: 30분

    // 발송된 인증코드 저장소 (Key: 이메일)
    private final Map<String, VerificationEntry> verificationCodes = new ConcurrentHashMap<>();
    // 인증에 성공한 이메일 저장소 (Key: 이메일, Value: 만료 시각)
    private final Map<String, Long> verifiedEmails = new ConcurrentHashMap<>();

    // 1. 인증번호 전송
    public void sendVerificationCode(String email) {
        String code = createCode();
        verificationCodes.put(email, new VerificationEntry(code, System.currentTimeMillis() + CODE_TTL_MILLIS));

        sendMail(email, "[Shop1] 회원가입 인증번호입니다.", "인증번호: <strong>" + code + "</strong> (5분 이내에 입력해주세요)");
    }

    /**
     *
     * 범용 메일 발송 (인증코드 외에 주문/결제 알림 등에도 재사용)
     *
     */
    public void sendMail(String to, String subject, String htmlContent) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }

    // 2. 인증번호 검증
    public boolean verifyCode(String email, String code) {
        VerificationEntry entry = verificationCodes.get(email);

        if (entry == null || entry.isExpired()) {
            verificationCodes.remove(email);
            return false;
        }

        if (entry.attempts.incrementAndGet() > MAX_VERIFY_ATTEMPTS) {
            verificationCodes.remove(email);
            return false;
        }

        if (!entry.code.equals(code)) {
            return false;
        }

        verificationCodes.remove(email);
        verifiedEmails.put(email, System.currentTimeMillis() + VERIFIED_TTL_MILLIS);
        return true;
    }

    /**
     *
     * 회원가입 직전, 해당 이메일이 실제로 인증을 완료했는지 확인하고 소비(1회용)한다.
     *
     */
    public void consumeVerifiedEmail(String email) {
        Long expiresAt = verifiedEmails.remove(email);
        if (expiresAt == null || expiresAt < System.currentTimeMillis()) {
            throw new IllegalStateException("이메일 인증이 필요합니다.");
        }
    }

    // 랜덤 코드 생성기
    private String createCode() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            key.append(random.nextInt(10));
        }
        return key.toString();
    }

    private static class VerificationEntry {
        private final String code;
        private final long expiresAt;
        private final AtomicInteger attempts = new AtomicInteger(0);

        private VerificationEntry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
