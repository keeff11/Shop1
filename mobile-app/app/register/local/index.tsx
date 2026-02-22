import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView, Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text, TextInput, TouchableOpacity,
  View
} from 'react-native';
import { fetchApi } from '../../../lib/api';

export default function RegisterLocalScreen() {
  const router = useRouter();

  // ===== 계정 정보 =====
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [role, setRole] = useState("CUSTOMER");

  // ===== 상태 관리 =====
  const [passwordMessage, setPasswordMessage] = useState("");
  const [isPasswordSafe, setIsPasswordSafe] = useState(false);
  const [isPasswordMatch, setIsPasswordMatch] = useState(false);
  const [emailCode, setEmailCode] = useState("");
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [isNicknameChecked, setIsNicknameChecked] = useState(false);

  // ===== 주소 정보 =====
  const [zipCode, setZipCode] = useState("");
  const [roadAddress, setRoadAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");

  // 🔐 비밀번호 유효성 및 일치 검사
  useEffect(() => {
    if (!password) {
      setPasswordMessage("");
      setIsPasswordSafe(false);
    } else {
      const passwordRegex = /^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,20}$/;
      if (!passwordRegex.test(password)) {
        setPasswordMessage("영문, 숫자, 특수문자(!@#$%^&*) 포함 8자 이상");
        setIsPasswordSafe(false);
      } else {
        setPasswordMessage("안전한 비밀번호입니다.");
        setIsPasswordSafe(true);
      }
    }
    setIsPasswordMatch(password !== "" && password === confirmPassword);
  }, [password, confirmPassword]);

  const isFormFilled = 
    email.trim() !== "" && password !== "" && confirmPassword !== "" && 
    nickname.trim() !== "" && zipCode !== "" && roadAddress !== "" && 
    detailAddress.trim() !== "" && recipientName.trim() !== "" && recipientPhone.trim() !== "";

  const isFormValid = isFormFilled && isPasswordSafe && isPasswordMatch && isEmailVerified && isNicknameChecked;

  // 📧 이메일 핸들러
  const sendVerificationCode = async () => {
    if (!email) return Alert.alert("알림", "이메일을 입력해주세요.");
    try {
      await fetchApi(`/auth/email/send-code?email=${encodeURIComponent(email)}`, { method: "POST" });
      setIsCodeSent(true);
      Alert.alert("성공", "인증 번호가 발송되었습니다.");
    } catch (error: any) {
      Alert.alert("발송 실패", error.message || "오류가 발생했습니다.");
    }
  };

  const verifyEmailCode = async () => {
    if (!emailCode) return Alert.alert("알림", "인증번호를 입력해주세요.");
    try {
      const res = await fetchApi<any>(`/auth/email/verify-code?email=${encodeURIComponent(email)}&code=${encodeURIComponent(emailCode)}`, { method: "POST" });
      if (res.data === true) {
        setIsEmailVerified(true);
        Alert.alert("성공", "이메일 인증 완료");
      } else {
        Alert.alert("실패", "인증 번호가 일치하지 않습니다.");
      }
    } catch (error) {
      Alert.alert("오류", "인증 처리 중 오류가 발생했습니다.");
    }
  };

  // 🏷️ 닉네임 핸들러
  const checkNickname = async () => {
    if (!nickname) return Alert.alert("알림", "닉네임을 입력해주세요.");
    try {
      const res = await fetchApi<any>(`/auth/check-nickname?nickname=${encodeURIComponent(nickname)}`);
      if (res.data === false) {
        setIsNicknameChecked(true);
        Alert.alert("성공", "사용 가능한 닉네임입니다.");
      } else {
        Alert.alert("실패", "이미 사용 중인 닉네임입니다.");
      }
    } catch (error) {
      Alert.alert("오류", "중복 확인 중 오류 발생");
    }
  };

  /** 회원가입 제출 */
  const handleSubmit = async () => {
    if (!isFormValid) {
      return Alert.alert("알림", "모든 필수 항목을 입력하고 인증을 완료해주세요.");
    }

    const formData = {
      email, password, nickname, userRole: role,
      zipCode, roadAddress, detailAddress, recipientName, recipientPhone,
    };

    try {
      await fetchApi("/auth/local/sign-up", {
        method: "POST",
        body: JSON.stringify(formData),
      });

      Alert.alert("가입 성공", "성공적으로 가입되었습니다!", [
        { text: "확인", onPress: () => router.replace('/(tabs)') }
      ]);
    } catch (error: any) {
      Alert.alert("가입 실패", error.message || "가입 오류 발생");
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          <Text style={styles.pageTitle}>Shop1 회원가입</Text>

          {/* 이메일 */}
          <View style={styles.inputGroup}>
            <Text style={styles.label}>이메일</Text>
            <View style={styles.row}>
              <TextInput 
                style={[styles.input, styles.flex1]} 
                value={email} 
                onChangeText={(t) => { setEmail(t); setIsEmailVerified(false); setIsCodeSent(false); }} 
                placeholder="example@gmail.com" 
                editable={!isEmailVerified}
                autoCapitalize="none"
              />
              <TouchableOpacity style={[styles.actionBtn, isEmailVerified && styles.disabledBtn]} onPress={sendVerificationCode} disabled={isEmailVerified}>
                <Text style={styles.actionBtnText}>{isEmailVerified ? "인증완료" : (isCodeSent ? "재발송" : "번호전송")}</Text>
              </TouchableOpacity>
            </View>
            {isCodeSent && !isEmailVerified && (
              <View style={[styles.row, { marginTop: 8 }]}>
                <TextInput style={[styles.input, styles.flex1]} value={emailCode} onChangeText={setEmailCode} placeholder="인증번호 6자리" keyboardType="number-pad" />
                <TouchableOpacity style={styles.verifyBtn} onPress={verifyEmailCode}>
                  <Text style={styles.verifyBtnText}>확인</Text>
                </TouchableOpacity>
              </View>
            )}
          </View>

          {/* 비밀번호 */}
          <View style={styles.inputGroup}>
            <Text style={styles.label}>비밀번호</Text>
            <TextInput 
              style={[styles.input, password.length > 0 && (isPasswordSafe ? styles.inputSuccess : styles.inputError)]} 
              value={password} onChangeText={setPassword} placeholder="영문, 숫자, 특수문자 포함 8~20자" secureTextEntry 
            />
            {password.length > 0 && <Text style={[styles.helperText, isPasswordSafe ? styles.successText : styles.errorText]}>{passwordMessage}</Text>}
          </View>

          {/* 비밀번호 확인 */}
          <View style={styles.inputGroup}>
            <Text style={styles.label}>비밀번호 확인</Text>
            <TextInput 
              style={[styles.input, confirmPassword.length > 0 && (isPasswordMatch ? styles.inputSuccess : styles.inputError)]} 
              value={confirmPassword} onChangeText={setConfirmPassword} placeholder="비밀번호 재입력" secureTextEntry 
            />
            {confirmPassword.length > 0 && <Text style={[styles.helperText, isPasswordMatch ? styles.successText : styles.errorText]}>{isPasswordMatch ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다."}</Text>}
          </View>

          {/* 닉네임 */}
          <View style={styles.inputGroup}>
            <Text style={styles.label}>닉네임</Text>
            <View style={styles.row}>
              <TextInput style={[styles.input, styles.flex1]} value={nickname} onChangeText={(t) => { setNickname(t); setIsNicknameChecked(false); }} placeholder="사용할 닉네임" />
              <TouchableOpacity style={[styles.actionBtn, isNicknameChecked && styles.disabledBtn]} onPress={checkNickname} disabled={isNicknameChecked}>
                <Text style={styles.actionBtnText}>{isNicknameChecked ? "확인완료" : "중복확인"}</Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* 배송지 정보 */}
          <View style={styles.inputGroup}>
            <View style={styles.divider} />
            <Text style={styles.label}>배송지 정보</Text>
            <View style={styles.row}>
              <TextInput style={[styles.input, { flex: 0.4 }]} value={zipCode} onChangeText={setZipCode} placeholder="우편번호" keyboardType="number-pad" />
              <TouchableOpacity style={styles.darkBtn} onPress={() => Alert.alert("안내", "RN 환경에서는 웹뷰를 통한 주소 검색 연동이 필요합니다. 임시로 직접 입력해주세요.")}>
                <Text style={styles.darkBtnText}>주소 검색</Text>
              </TouchableOpacity>
            </View>
            <TextInput style={[styles.input, { marginTop: 8 }]} value={roadAddress} onChangeText={setRoadAddress} placeholder="도로명 주소" />
            <TextInput style={[styles.input, { marginTop: 8 }]} value={detailAddress} onChangeText={setDetailAddress} placeholder="상세 주소를 입력하세요" />
            <View style={[styles.row, { marginTop: 8 }]}>
              <TextInput style={[styles.input, styles.flex1]} value={recipientName} onChangeText={setRecipientName} placeholder="수령인 성함" />
              <TextInput style={[styles.input, styles.flex1]} value={recipientPhone} onChangeText={setRecipientPhone} placeholder="연락처" keyboardType="phone-pad" />
            </View>
          </View>

          <TouchableOpacity 
            style={[styles.submitBtn, !isFormValid && styles.disabledSubmitBtn]} 
            onPress={handleSubmit} disabled={!isFormValid}
          >
            <Text style={styles.submitBtnText}>가입 및 로그인하기</Text>
          </TouchableOpacity>

        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  scrollContent: { padding: 24, paddingBottom: 60 },
  pageTitle: { fontSize: 24, fontWeight: 'bold', color: '#111', textAlign: 'center', marginBottom: 30 },
  inputGroup: { marginBottom: 20 },
  label: { fontSize: 14, fontWeight: 'bold', color: '#4b5563', marginBottom: 8 },
  input: { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8, paddingHorizontal: 16, paddingVertical: 14, fontSize: 15, backgroundColor: '#fff' },
  inputSuccess: { borderColor: '#10b981' },
  inputError: { borderColor: '#ef4444' },
  flex1: { flex: 1 },
  row: { flexDirection: 'row', gap: 8 },
  actionBtn: { justifyContent: 'center', alignItems: 'center', paddingHorizontal: 16, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8, backgroundColor: '#f9fafb' },
  actionBtnText: { fontSize: 13, fontWeight: 'bold', color: '#374151' },
  verifyBtn: { justifyContent: 'center', alignItems: 'center', paddingHorizontal: 16, borderRadius: 8, backgroundColor: '#2563eb' },
  verifyBtnText: { fontSize: 14, fontWeight: 'bold', color: '#fff' },
  darkBtn: { flex: 0.6, justifyContent: 'center', alignItems: 'center', borderRadius: 8, backgroundColor: '#1f2937' },
  darkBtnText: { fontSize: 14, fontWeight: 'bold', color: '#fff' },
  disabledBtn: { opacity: 0.5 },
  helperText: { fontSize: 12, marginTop: 4, fontWeight: '500' },
  successText: { color: '#10b981' },
  errorText: { color: '#ef4444' },
  divider: { height: 1, backgroundColor: '#e5e7eb', marginVertical: 16 },
  submitBtn: { backgroundColor: '#000', paddingVertical: 18, borderRadius: 12, alignItems: 'center', marginTop: 10 },
  disabledSubmitBtn: { backgroundColor: '#d1d5db' },
  submitBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' }
});