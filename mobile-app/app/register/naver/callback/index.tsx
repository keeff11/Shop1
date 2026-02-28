import axios from "axios";
import { useRouter } from "expo-router";
import React, { useRef, useState } from "react";
import { ActivityIndicator, Alert, StyleSheet, View } from "react-native";
import { WebView } from "react-native-webview";
import { API_BASE } from "../../../../config/api";
import { useAuth } from "../../../../contexts/AuthContext";

const NAVER_ID = process.env.EXPO_PUBLIC_NAVER_CLIENT_ID;

// 1. 네이버 서버를 속이기 위한 가짜 주소
const NAVER_REDIRECT_URI = "http://localhost:8080/auth/naver/callback";

// 2. 실제로 코드를 백엔드로 보낼 API 주소
const BACKEND_API_URI = `${API_BASE}/auth/naver/callback`;

export default function NaverLoginScreen() {
  const router = useRouter();
  const { login } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const isRequesting = useRef(false);

  const STATE_STRING = Math.random().toString(36).substring(2, 15);
  
  const naverAuthUrl =
    `https://nid.naver.com/oauth2.0/authorize` +
    `?response_type=code` +
    `&client_id=${NAVER_ID}` +
    `&redirect_uri=${encodeURIComponent(NAVER_REDIRECT_URI)}` +
    `&state=${STATE_STRING}`;

  const handleLogin = async (code: string, state: string) => {
    if (isRequesting.current) return;
    isRequesting.current = true;
    setIsLoading(true);

    try {
      const response = await axios.post<any>(
        BACKEND_API_URI, 
        { code, state },
        { headers: { "Content-Type": "application/json" } }
      );

      // [핵심 수정] ApiResponse 껍데기 제거
      const data = response.data.data;

      if (!data) {
        Alert.alert("실패", "서버로부터 올바른 응답을 받지 못했습니다.");
        router.back();
        return;
      }

      if (data.isRegistered || data.accessToken) {
        if (data.accessToken) {
          await login(data.accessToken); 
        }
        Alert.alert("환영합니다", "로그인 되었습니다.");
        router.replace("/(tabs)");
      } else if (data.signUpToken) {
        router.push({
            pathname: "/register/social/additional-info", 
            params: {
                provider: "naver",
                naverToken: data.signUpToken
            }
        });
      } else {
        Alert.alert("오류", "회원가입 정보가 부족합니다.");
        router.back();
      }

    } catch (error: any) {
      console.error("Naver Login Error:", error.response?.data || error.message);
      Alert.alert("오류", "서버 통신 중 오류가 발생했습니다.");
      router.back();
    } finally {
      setIsLoading(false);
      isRequesting.current = false;
    }
  };

  const onShouldStartLoadWithRequest = (request: any) => {
    if (request.url.includes(NAVER_REDIRECT_URI) && request.url.includes("code=") && request.url.includes("state=")) {
      const urlParts = request.url.split("?");
      if (urlParts.length > 1) {
          const queryParams = urlParts[1].split("&");
          let code = "";
          let state = "";

          // 타입스크립트 에러 방지용 (param: string)
          queryParams.forEach((param: string) => {
              const [key, value] = param.split("=");
              if (key === "code") code = value;
              if (key === "state") state = value;
          });

          if (code && state && !isRequesting.current) {
              handleLogin(code, state);
          }
      }
      return false; 
    }
    return true;
  };

  return (
    <View style={styles.container}>
      <WebView
        style={{ flex: 1, display: isLoading ? 'none' : 'flex' }}
        source={{ uri: naverAuthUrl }}
        userAgent="Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
        onShouldStartLoadWithRequest={onShouldStartLoadWithRequest}
        incognito={true}
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
  container: { flex: 1, backgroundColor: "#fff" },
  loadingOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: "rgba(255,255,255,0.9)", justifyContent: "center", alignItems: "center", zIndex: 10 },
});