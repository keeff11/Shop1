  import axios from "axios";
import { useRouter } from "expo-router";
import React, { useRef, useState } from "react";
import { ActivityIndicator, Alert, StyleSheet, View } from "react-native";
import { WebView } from "react-native-webview";
import { API_BASE } from "../../../../config/api";
import { useAuth } from "../../../../contexts/AuthContext";

  const KAKAO_KEY = process.env.EXPO_PUBLIC_KAKAO_API_KEY;

  // 1. 카카오 서버를 속이기 위한 가짜 주소 (백엔드 application.yml과 동일하게)
  const KAKAO_REDIRECT_URI = "http://localhost:8080/auth/kakao/callback";

  // 2. 실제로 인가 코드를 백엔드로 보낼 진짜 API 주소
  const BACKEND_API_URI = `${API_BASE}/auth/kakao/callback`;

  export default function KakaoLoginScreen() {
    const router = useRouter();
    const { login } = useAuth();
    const [isLoading, setIsLoading] = useState(false);
    const isRequesting = useRef(false);

    const kakaoAuthUrl =
      `https://kauth.kakao.com/oauth/authorize` +
      `?response_type=code` +
      `&client_id=${KAKAO_KEY}` +
      `&redirect_uri=${encodeURIComponent(KAKAO_REDIRECT_URI)}`;

    const handleLogin = async (code: string) => {
      if (isRequesting.current) return;
      isRequesting.current = true;
      setIsLoading(true);

      try {
        const response = await axios.post<any>(
          BACKEND_API_URI,
          { code },
          { headers: { "Content-Type": "application/json" } }
        );

        // [핵심 수정] ApiResponse 껍데기를 벗기고 실제 데이터를 꺼냅니다.
        const data = response.data.data;

        if (!data) {
          Alert.alert("실패", "서버로부터 올바른 응답을 받지 못했습니다.");
          router.back();
          return;
        }

        if (data.registered || data.accessToken) {
          // 기존 가입된 회원
          if (data.accessToken) {
            await login(data.accessToken); 
          }
          Alert.alert("환영합니다", "로그인 되었습니다.");
          router.replace("/(tabs)"); 
        } else if (data.signUpToken) {
          // 신규 회원 -> 추가 정보 입력 페이지로 이동
          router.push({
              // 💡 주의: 추가 정보 페이지 폴더를 어디에 두셨는지에 따라 경로를 확인하세요!
              pathname: "/register/social/additional-info", 
              params: {
                  provider: "kakao",
                  kakaoToken: data.signUpToken
              }
          });
        } else {
          Alert.alert("오류", "회원가입 정보가 부족합니다.");
          router.back();
        }

      } catch (error: any) {
        console.error("Kakao Login Error:", error.response?.data || error.message);
        Alert.alert("오류", "서버 통신 중 오류가 발생했습니다.");
        router.back();
      } finally {
        setIsLoading(false);
        isRequesting.current = false;
      }
    };

    const onShouldStartLoadWithRequest = (request: any) => {
      if (request.url.includes(KAKAO_REDIRECT_URI) && request.url.includes("code=")) {
        const code = request.url.split("code=")[1]?.split("&")[0];
        if (code && !isRequesting.current) {
          setIsLoading(true);
          handleLogin(code);
        }
        return false; 
      }
      return true;
    };

    return (
      <View style={styles.container}>
        <WebView
          style={{ flex: 1, display: isLoading ? 'none' : 'flex' }}
          source={{ uri: kakaoAuthUrl }}
          onShouldStartLoadWithRequest={onShouldStartLoadWithRequest}
          incognito={true}
        />

        {isLoading && (
          <View style={styles.loadingOverlay}>
            <ActivityIndicator size="large" color="#FEE500" />
          </View>
        )}
      </View>
    );
  }

  const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: "#fff" },
    loadingOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: "rgba(255,255,255,0.9)", justifyContent: "center", alignItems: "center", zIndex: 10 },
  });