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

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    // 인증코드를 저장할 메모리 저장소 (Key: 이메일, Value: 인증코드)
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    // 1. 인증번호 전송
    public void sendVerificationCode(String email) {
        // 6자리 난수 생성
        String code = createCode();

        // 메모리에 저장 (나중에 검증할 때 씀)
        verificationCodes.put(email, code);

        // 이메일 전송 로직
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(email);
            helper.setSubject("[Shop1] 회원가입 인증번호입니다.");
            helper.setText("인증번호: <strong>" + code + "</strong>", true); // HTML 사용 가능

            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }

    // 2. 인증번호 검증
    public boolean verifyCode(String email, String code) {
        String savedCode = verificationCodes.get(email);

        if (savedCode != null && savedCode.equals(code)) {
            verificationCodes.remove(email); // 인증 성공 시 삭제
            return true;
        }
        return false;
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
}
