import axios from "axios";
import { useRouter } from "expo-router";
import React, { useState } from "react";
import { ActivityIndicator, Alert, StyleSheet, View } from "react-native";
import { WebView } from "react-native-webview";

// 환경 변수 로드
const API_URL = process.env.EXPO_PUBLIC_API_URL;
const KAKAO_KEY = process.env.EXPO_PUBLIC_KAKAO_API_KEY;

// [중요] 백엔드 설정과 100% 일치해야 합니다.
// 예: http://192.168.0.69:8080/auth/kakao/callback
const REDIRECT_URI = `${API_URL}/auth/kakao/callback`;

interface KakaoCallbackResponse {
  success: boolean;
  isRegistered: boolean;
  signUpToken?: string;
}

export default function KakaoLoginScreen() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);

  // 1. 카카오 로그인 페이지 URL 생성
  const kakaoAuthUrl =
    `https://kauth.kakao.com/oauth/authorize` +
    `?response_type=code` +
    `&client_id=${KAKAO_KEY}` +
    `&redirect_uri=${encodeURIComponent(REDIRECT_URI)}`;

  // 2. 백엔드로 code 전송 및 로그인 처리 함수
  const handleLogin = async (code: string) => {
    setIsLoading(true);
    try {
      // 웹 코드의 fetch → axios로 변경
      const response = await axios.post<KakaoCallbackResponse>(
        REDIRECT_URI, // 웹과 동일한 주소로 POST 요청
        { code },
        {
          headers: { "Content-Type": "application/json" },
          // withCredentials: true, // 세션/쿠키 방식일 경우 필요 (JWT면 생략 가능)
        }
      );

      const data = response.data;

      // 실패 처리
      if (!data.success) {
        Alert.alert("실패", "로그인 처리에 실패했습니다.");
        router.back();
        return;
      }

      // 성공 처리 분기
      if (data.isRegistered) {
        // [A] 기존 회원 -> 홈으로 이동
        // (필요하다면 여기서 받은 JWT 토큰을 SecureStore에 저장하는 로직 추가)
        Alert.alert("환영합니다", "로그인 되었습니다.");
        router.replace("/"); 
      } else if (data.signUpToken) {
        // [B] 신규 회원 -> 추가 정보 입력 페이지로 이동
        // Expo Router의 params 전달 방식
        router.push({
            pathname: "/register/social/additional-info",
            params: {
                provider: "kakao",
                kakaoToken: data.signUpToken
            }
        });
      } else {
        Alert.alert("오류", "회원가입 토큰이 없습니다.");
        router.back();
      }

    } catch (error: any) {
      console.error("Login Error:", error);
      Alert.alert("오류", "서버 통신 중 오류가 발생했습니다.");
      router.back();
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      {/* WebView가 브라우저 역할을 대신합니다. 
        사용자가 로그인을 마치고 REDIRECT_URI로 이동하려는 순간을 감지합니다.
      */}
      <WebView
        style={{ flex: 1 }}
        source={{ uri: kakaoAuthUrl }}
        // 주소가 바뀔 때마다 실행됨
        onNavigationStateChange={(e) => {
          // 우리가 기다리던 그 주소(Redirect URI)가 맞는지 확인
          if (e.url.includes(REDIRECT_URI) && e.url.includes("code=")) {
            // 1. URL에서 code 추출
            const code = e.url.split("code=")[1];
            
            // 2. 더 이상 웹뷰 진행 막기 (흰 화면 방지)
            // (WebView 컴포넌트 자체를 안 보이게 처리하거나 로딩 화면으로 전환)
            setIsLoading(true);

            // 3. 서버로 전송
            handleLogin(code);
          }
        }}
      />

      {/* 로딩 중일 때 전체 화면을 가리는 인디케이터 */}
      {isLoading && (
        <View style={styles.loadingOverlay}>
          <ActivityIndicator size="large" color="#3b82f6" />
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
  },
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject, // 화면 전체 덮기
    backgroundColor: "rgba(255,255,255,0.9)",
    justifyContent: "center",
    alignItems: "center",
    zIndex: 10,
  },
});