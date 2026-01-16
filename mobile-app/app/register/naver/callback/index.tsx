import axios from "axios";
import { useRouter } from "expo-router";
import React, { useRef, useState } from "react";
import { ActivityIndicator, Alert, StyleSheet, View } from "react-native";
import { WebView } from "react-native-webview";

// 환경 변수 로드 (.env 확인)
const API_URL = process.env.EXPO_PUBLIC_API_URL;
const NAVER_ID = process.env.EXPO_PUBLIC_NAVER_CLIENT_ID;

// [중요] 백엔드 & 네이버 개발자 센터 설정과 100% 일치해야 함
// 예: http://192.168.0.69:8080/auth/naver/callback
const REDIRECT_URI = `${API_URL}/auth/naver/callback`;

interface NaverCallbackResponse {
  success: boolean;
  isRegistered: boolean;
  signUpToken?: string;
}

export default function NaverLoginScreen() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  
  // 중복 요청 방지를 위한 플래그
  const isRequesting = useRef(false);

  // 1. 네이버 로그인 URL 생성
  // (보안을 위해 state는 랜덤 문자열을 쓰는 것이 정석이나, 여기선 간단히 처리)
  const STATE_STRING = Math.random().toString(36).substring(2, 15);
  
  const naverAuthUrl =
    `https://nid.naver.com/oauth2.0/authorize` +
    `?response_type=code` +
    `&client_id=${NAVER_ID}` +
    `&redirect_uri=${encodeURIComponent(REDIRECT_URI)}` +
    `&state=${STATE_STRING}`;

  // 2. 백엔드 로그인 처리 함수
  const handleLogin = async (code: string, state: string) => {
    if (isRequesting.current) return; // 이미 요청 중이면 무시
    isRequesting.current = true;
    setIsLoading(true);

    try {
      // Axios POST 요청
      const response = await axios.post<NaverCallbackResponse>(
        REDIRECT_URI, 
        { code, state },
        { headers: { "Content-Type": "application/json" } }
      );

      const data = response.data;

      if (!data.success) {
        Alert.alert("실패", "네이버 로그인 처리에 실패했습니다.");
        router.back();
        return;
      }

      // 성공 분기 처리
      if (data.isRegistered) {
        // [A] 기존 회원
        Alert.alert("환영합니다", "로그인 되었습니다.");
        router.replace("/");
      } else if (data.signUpToken) {
        // [B] 신규 회원 -> 추가 정보 페이지로 토큰 전달
        router.push({
            pathname: "/register/social/additional-info",
            params: {
                provider: "naver",
                naverToken: data.signUpToken
            }
        });
      } else {
        Alert.alert("오류", "회원가입 토큰이 없습니다.");
        router.back();
      }

    } catch (error: any) {
      console.error("Naver Login Error:", error);
      Alert.alert("오류", "서버 통신 중 오류가 발생했습니다.");
      router.back();
    } finally {
      setIsLoading(false);
      isRequesting.current = false;
    }
  };

  return (
    <View style={styles.container}>
      <WebView
        style={{ flex: 1 }}
        source={{ uri: naverAuthUrl }}
        // userAgent 설정은 구글/네이버 등에서 보안상 모바일 웹뷰를 차단할 때 유용함
        userAgent="Mozilla/5.0 (content-shell-mobile-use-only)"
        onNavigationStateChange={(e) => {
          // 리다이렉트 URI 감지
          if (e.url.includes(REDIRECT_URI) && e.url.includes("code=") && e.url.includes("state=")) {
            
            // URL 파싱 (code, state 추출)
            // URLSearchParams는 RN 일부 버전에서 지원 안될 수 있어 문자열 split 사용 권장
            const urlParts = e.url.split("?");
            if (urlParts.length > 1) {
                const queryParams = urlParts[1].split("&");
                let code = "";
                let state = "";

                queryParams.forEach(param => {
                    const [key, value] = param.split("=");
                    if (key === "code") code = value;
                    if (key === "state") state = value;
                });

                if (code && state && !isRequesting.current) {
                    handleLogin(code, state);
                }
            }
          }
        }}
      />

      {isLoading && (
        <View style={styles.loadingOverlay}>
          <ActivityIndicator size="large" color="#03C75A" />
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
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(255,255,255,0.9)",
    justifyContent: "center",
    alignItems: "center",
    zIndex: 10,
  },
});