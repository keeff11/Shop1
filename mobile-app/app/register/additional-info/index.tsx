import axios from 'axios';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    KeyboardAvoidingView,
    Platform,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from 'react-native';
import { API_BASE } from '../../../config/api';
import { useAuth } from '../../../contexts/AuthContext';

export default function SocialAdditionalInfoScreen() {
  const router = useRouter();
  const { login } = useAuth();
  
  // 이전 콜백 화면에서 넘겨준 파라미터(provider, signUpToken) 받기
  const params = useLocalSearchParams();
  const provider = params.provider as string;
  const signUpToken = (params.kakaoToken || params.naverToken) as string;

  // 폼 상태 관리
  const [nickname, setNickname] = useState('');
  const [recipientName, setRecipientName] = useState('');
  const [recipientPhone, setRecipientPhone] = useState('');
  const [zipCode, setZipCode] = useState('');
  const [roadAddress, setRoadAddress] = useState('');
  const [detailAddress, setDetailAddress] = useState('');
  
  const [loading, setLoading] = useState(false);

  // 회원가입 완료 요청
  const handleSubmit = async () => {
    // 1. 필수 입력값 검증
    if (!nickname || !recipientName || !recipientPhone || !zipCode || !roadAddress || !detailAddress) {
      Alert.alert('알림', '모든 정보를 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      // 2. 백엔드 /auth/{provider}/login 주소로 요청
      const response = await axios.post(
        `${API_BASE}/auth/${provider}/login`,
        {
          nickname,
          recipientName,
          recipientPhone,
          zipCode,
          roadAddress,
          detailAddress,
          userRole: 'USER', // 기본 권한
        },
        {
          headers: {
            'Content-Type': 'application/json',
            // 백엔드 @RequestHeader(HttpHeaders.AUTHORIZATION)에 맞게 토큰 전송
            Authorization: signUpToken, 
          },
        }
      );

      const data = response.data;

      // 3. 성공 처리 로직
      if (data && data.data && data.data.accessToken) {
        // 발급받은 실제 엑세스 토큰으로 로그인 처리
        await login(data.data.accessToken);
        Alert.alert('환영합니다', '소셜 회원가입 및 로그인이 완료되었습니다.');
        router.replace('/(tabs)');
      } else {
        Alert.alert('오류', '토큰을 발급받지 못했습니다.');
      }
    } catch (error: any) {
      console.error('추가 정보 입력 에러:', error.response?.data || error.message);
      Alert.alert('가입 실패', error.response?.data?.message || '회원가입 처리 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          
          <View style={styles.headerArea}>
            <Text style={styles.title}>추가 정보 입력</Text>
            <Text style={styles.subtitle}>
              서비스 이용을 위해 필요한 추가 정보를 입력해주세요.
            </Text>
          </View>

          <View style={styles.formArea}>
            <Text style={styles.label}>닉네임</Text>
            <TextInput
              style={styles.input}
              placeholder="사용하실 닉네임을 입력하세요"
              value={nickname}
              onChangeText={setNickname}
            />

            <Text style={styles.label}>수령인 이름 (배송용)</Text>
            <TextInput
              style={styles.input}
              placeholder="본인 이름을 입력하세요"
              value={recipientName}
              onChangeText={setRecipientName}
            />

            <Text style={styles.label}>연락처 (배송용)</Text>
            <TextInput
              style={styles.input}
              placeholder="010-0000-0000"
              keyboardType="phone-pad"
              value={recipientPhone}
              onChangeText={setRecipientPhone}
            />

            <Text style={styles.label}>우편번호</Text>
            <View style={styles.rowInput}>
              <TextInput
                style={[styles.input, { flex: 1, marginBottom: 0 }]}
                placeholder="우편번호"
                keyboardType="numeric"
                value={zipCode}
                onChangeText={setZipCode}
              />
              {/* 추후 여기에 '우편번호 검색' 버튼과 웹뷰(카카오 우편번호 API 등)를 연동하시면 좋습니다. */}
            </View>

            <Text style={styles.label}>도로명 주소</Text>
            <TextInput
              style={styles.input}
              placeholder="도로명 주소를 입력하세요"
              value={roadAddress}
              onChangeText={setRoadAddress}
            />

            <Text style={styles.label}>상세 주소</Text>
            <TextInput
              style={styles.input}
              placeholder="상세 주소 (동, 호수 등)"
              value={detailAddress}
              onChangeText={setDetailAddress}
            />
          </View>

          <TouchableOpacity 
            style={[styles.submitBtn, loading && styles.disabledBtn]} 
            onPress={handleSubmit}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.submitBtnText}>가입 완료하기</Text>
            )}
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#ffffff' },
  container: { paddingHorizontal: 24, paddingTop: 40, paddingBottom: 60 },
  headerArea: { marginBottom: 30 },
  title: { fontSize: 28, fontWeight: 'bold', color: '#111', marginBottom: 8 },
  subtitle: { fontSize: 14, color: '#6b7280', lineHeight: 20 },
  formArea: { marginBottom: 30 },
  label: { fontSize: 14, fontWeight: '600', color: '#374151', marginBottom: 8, marginTop: 16 },
  input: {
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: 15,
    backgroundColor: '#f9fafb',
  },
  rowInput: { flexDirection: 'row', gap: 10 },
  submitBtn: {
    backgroundColor: '#111',
    paddingVertical: 18,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 10,
  },
  disabledBtn: { backgroundColor: '#9ca3af' },
  submitBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
});