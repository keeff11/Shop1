import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  KeyboardAvoidingView, Platform,
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View
} from 'react-native';
import { useAuth } from '../../contexts/AuthContext';
import { fetchApi } from '../../lib/api';

export default function RegisterMainScreen() {
  const router = useRouter();
  const { login } = useAuth();

  // 로그인 폼 상태 관리
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  // 1. 이메일 로그인 처리 로직
  const handleLocalLogin = async () => {
    if (!email || !password) {
      Alert.alert('알림', '이메일과 비밀번호를 모두 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      // 백엔드의 LoginResponseDTO 형태에 맞게 통신
      const res = await fetchApi<any>('/auth/local/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });

      // 백엔드 응답에서 토큰 추출 (응답 구조에 따라 data.accessToken 등 수정 필요)
      const token = res.data?.accessToken || res.accessToken || res.data || res;
      
      if (token && typeof token === 'string') {
        // AuthContext의 login 함수를 호출하여 토큰을 저장하고 유저 정보를 불러옴
        await login(token);
        
        // 로그인 성공 시 메인 홈(탭)으로 이동
        router.replace('/(tabs)');
      } else {
        Alert.alert('오류', '로그인 토큰을 받아오지 못했습니다.');
      }
    } catch (err: any) {
      console.error('로그인 에러:', err);
      Alert.alert('로그인 실패', err.message || '이메일 또는 비밀번호가 일치하지 않습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 2. 소셜 로그인 처리 로직 (웹뷰 연동 뼈대)
  const handleKakaoLogin = async () => {
    // 실제 작동을 위해서는 app.json에 'scheme'을 등록하고 백엔드 리다이렉트 주소를 맞춰야 합니다.
    // 예: WebBrowser.openAuthSessionAsync('http://백엔드IP:8080/oauth2/authorization/kakao', 'shop1://');
    Alert.alert('안내', '앱에서 카카오 로그인을 하려면 백엔드 OAuth 리다이렉트 및 앱 딥링크(scheme) 설정이 필요합니다.');
  };

  const handleNaverLogin = async () => {
    Alert.alert('안내', '앱에서 네이버 로그인을 하려면 백엔드 OAuth 리다이렉트 및 앱 딥링크(scheme) 설정이 필요합니다.');
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.container}>
          
          {/* 상단 로고 및 타이틀 영역 */}
          <View style={styles.headerArea}>
            <Text style={styles.logoText}>Shop1</Text>
            <Text style={styles.subtitle}>
              모든 가치를 한 곳에,{'\n'}가장 트렌디한 쇼핑의 시작
            </Text>
          </View>

          <View style={{ flex: 1 }} />

          {/* 이메일 로그인 입력 영역 (추가된 부분) */}
          <View style={styles.inputArea}>
            <TextInput
              style={styles.input}
              placeholder="이메일"
              value={email}
              onChangeText={setEmail}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
            />
            <TextInput
              style={styles.input}
              placeholder="비밀번호"
              value={password}
              onChangeText={setPassword}
              secureTextEntry
            />
            <TouchableOpacity 
              style={[styles.loginBtn, loading && styles.disabledBtn]} 
              onPress={handleLocalLogin}
              disabled={loading}
            >
              {loading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.loginBtnText}>로그인</Text>
              )}
            </TouchableOpacity>

            <View style={styles.signupRow}>
              <Text style={styles.signupText}>아직 계정이 없으신가요?</Text>
              <TouchableOpacity onPress={() => router.push('/register/local')}>
                <Text style={styles.signupLink}>이메일로 회원가입</Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* 소셜 로그인 및 구분선 영역 */}
          <View style={styles.dividerRow}>
            <View style={styles.dividerLine} />
            <Text style={styles.dividerText}>또는 SNS로 시작하기</Text>
            <View style={styles.dividerLine} />
          </View>

          <View style={styles.socialArea}>
            <TouchableOpacity activeOpacity={0.8} onPress={handleKakaoLogin} style={styles.socialBtn}>
              <Image source={require('../../assets/kakao_login1.png')} style={styles.socialImage} resizeMode="contain" />
            </TouchableOpacity>

            <TouchableOpacity activeOpacity={0.8} onPress={handleNaverLogin} style={styles.socialBtn}>
              <Image source={require('../../assets/naver_login.png')} style={styles.socialImage} resizeMode="contain" />
            </TouchableOpacity>
          </View>

          <View style={styles.footerArea}>
            <Text style={styles.footerText}>
              로그인 시 Shop1의 <Text style={styles.linkText}>이용약관</Text> 및 <Text style={styles.linkText}>개인정보처리방침</Text>에 동의하게 됩니다.
            </Text>
          </View>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#ffffff' },
  container: { flex: 1, paddingHorizontal: 24, paddingTop: 40, paddingBottom: 30 },
  headerArea: { alignItems: 'center', marginTop: 20 },
  logoText: { fontSize: 42, fontWeight: '900', color: '#111', marginBottom: 12, letterSpacing: -1 },
  subtitle: { fontSize: 15, color: '#6b7280', textAlign: 'center', lineHeight: 22 },
  
  inputArea: { width: '100%', marginBottom: 30 },
  input: {
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 16,
    fontSize: 15,
    backgroundColor: '#f9fafb',
    marginBottom: 12,
  },
  loginBtn: {
    backgroundColor: '#111',
    paddingVertical: 18,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 8,
  },
  disabledBtn: { backgroundColor: '#9ca3af' },
  loginBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  signupRow: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', marginTop: 16, gap: 6 },
  signupText: { color: '#6b7280', fontSize: 14 },
  signupLink: { color: '#2563eb', fontSize: 14, fontWeight: 'bold' },

  dividerRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 20 },
  dividerLine: { flex: 1, height: 1, backgroundColor: '#e5e7eb' },
  dividerText: { marginHorizontal: 12, fontSize: 13, color: '#9ca3af', fontWeight: '500' },

  socialArea: { width: '100%', gap: 12, marginBottom: 30 },
  socialBtn: { width: '100%', height: 52, borderRadius: 12, overflow: 'hidden', justifyContent: 'center', alignItems: 'center' },
  socialImage: { width: '100%', height: '100%' },
  
  footerArea: { alignItems: 'center', paddingHorizontal: 20 },
  footerText: { fontSize: 12, color: '#9ca3af', textAlign: 'center', lineHeight: 18 },
  linkText: { textDecorationLine: 'underline', color: '#6b7280' },
});