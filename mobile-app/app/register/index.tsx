import { useRouter } from "expo-router";
import React, { useState } from "react";
import {
    Alert,
    Image,
    KeyboardAvoidingView,
    Platform,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from "react-native";

// ==================================================================
// 1. .env에서 환경변수 불러오기
// (변수명이 틀리면 undefined가 되니 .env 파일과 철자가 같은지 꼭 확인하세요)
// ==================================================================
const API_URL = process.env.EXPO_PUBLIC_API_URL; // 예: http://192.168.0.69:8080
const KAKAO_KEY = process.env.EXPO_PUBLIC_KAKAO_API_KEY;
const NAVER_ID = process.env.EXPO_PUBLIC_NAVER_CLIENT_ID;

export default function RegisterScreen() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // =========================
  // 소셜 로그인 핸들러
  // =========================

    const handleKakaoLogin = () => {
        // 외부 브라우저(Linking) 대신, 앱 내 웹뷰 페이지로 이동합니다.
        router.push("/register/kakao/callback");
    };

    const handleNaverLogin = () => {
        // 네이버도 마찬가지로 앱 내 웹뷰 페이지로 이동합니다.
        router.push("/register/naver/callback");
    };

//   const handleKakaoLogin = async () => {
//     // 안전장치: 환경변수가 제대로 안 불러와졌으면 경고 띄움
//     if (!API_URL || !KAKAO_KEY) {
//       Alert.alert("설정 오류", "환경변수(API URL 또는 카카오 키)가 설정되지 않았습니다.");
//       console.error("Missing Env Vars:", { API_URL, KAKAO_KEY });
//       return;
//     }

//     const REDIRECT_URI = `${API_URL}/auth/kakao/callback`; 
    
//     const kakaoAuthUrl =
//       `https://kauth.kakao.com/oauth/authorize` +
//       `?response_type=code` +
//       `&client_id=${KAKAO_KEY}` +
//       `&redirect_uri=${encodeURIComponent(REDIRECT_URI)}`;

//     // 모바일 브라우저 열기
//     await Linking.openURL(kakaoAuthUrl);
//   };

//   const handleNaverLogin = async () => {
//     if (!API_URL || !NAVER_ID) {
//       Alert.alert("설정 오류", "환경변수(API URL 또는 네이버 ID)가 설정되지 않았습니다.");
//       console.error("Missing Env Vars:", { API_URL, NAVER_ID });
//       return;
//     }

//     const REDIRECT_URI = `${API_URL}/auth/naver/callback`;
//     const STATE = Math.random().toString(36).substring(2, 15);

//     const naverAuthUrl =
//       `https://nid.naver.com/oauth2.0/authorize` +
//       `?response_type=code` +
//       `&client_id=${NAVER_ID}` +
//       `&redirect_uri=${encodeURIComponent(REDIRECT_URI)}` +
//       `&state=${STATE}`;

//     await Linking.openURL(naverAuthUrl);
//   };

  const handleLocalLogin = () => {
    // 로컬 로그인 구현 시에도 API_URL 사용 가능
    // axios.post(`${API_URL}/auth/login`, { email, password }) ...
    console.log("로그인 시도:", email, password);
    console.log("현재 API 주소:", API_URL); // 디버깅용
    Alert.alert("알림", "로그인 버튼이 눌렸습니다.");
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : "height"}
        style={{ flex: 1 }}
      >
        <ScrollView contentContainerStyle={styles.container}>
          <View style={styles.card}>
            <Text style={styles.title}>shop1</Text>
            <Text style={styles.subtitle}>로그인 / 회원가입</Text>

            {/* 로컬 로그인 폼 */}
            <View style={styles.form}>
              <TextInput
                style={styles.input}
                placeholder="이메일"
                placeholderTextColor="#999"
                keyboardType="email-address"
                autoCapitalize="none"
                value={email}
                onChangeText={setEmail}
              />
              <TextInput
                style={styles.input}
                placeholder="비밀번호"
                placeholderTextColor="#999"
                secureTextEntry
                value={password}
                onChangeText={setPassword}
              />
              <TouchableOpacity style={styles.loginButton} onPress={handleLocalLogin}>
                <Text style={styles.loginButtonText}>로그인</Text>
              </TouchableOpacity>
            </View>

            {/* 구분선 */}
            <View style={styles.divider} />

            {/* 회원가입 & 소셜 로그인 */}
            <View style={styles.socialContainer}>
              {/* 로컬 회원가입 */}
              <TouchableOpacity
                style={styles.signupButton}
                onPress={() => router.push("/register/local")}
              >
                <Text style={styles.signupButtonText}>Shop1 회원가입</Text>
              </TouchableOpacity>

              {/* 카카오 로그인 */}
              <TouchableOpacity onPress={handleKakaoLogin} style={styles.socialButton}>
                {/* assets 폴더 이미지 경로 확인 필수 */}
                <Image
                  source={require("../../assets/kakao_login1.png")} 
                  style={styles.kakaoImage}
                  resizeMode="contain"
                />
              </TouchableOpacity>

              {/* 네이버 로그인 */}
              <TouchableOpacity onPress={handleNaverLogin} style={styles.socialButton}>
                <Image
                  source={require("../../assets/naver_login1.png")}
                  style={styles.naverImage}
                  resizeMode="contain"
                />
              </TouchableOpacity>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#f9fafb",
  },
  container: {
    flexGrow: 1,
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  card: {
    width: "100%",
    maxWidth: 400,
    backgroundColor: "white",
    borderRadius: 10,
    padding: 30,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 5,
  },
  title: {
    fontSize: 30,
    fontWeight: "bold",
    textAlign: "center",
    marginBottom: 24,
    color: "#000",
  },
  subtitle: {
    fontSize: 24,
    fontWeight: "600",
    textAlign: "center",
    marginBottom: 24,
    color: "#333",
  },
  form: {
    gap: 16,
  },
  input: {
    width: "100%",
    height: 50,
    borderWidth: 1,
    borderColor: "#e5e7eb",
    borderRadius: 6,
    paddingHorizontal: 16,
    fontSize: 16,
    backgroundColor: "#fff",
  },
  loginButton: {
    width: "100%",
    height: 50,
    backgroundColor: "#60a5fa",
    borderRadius: 6,
    justifyContent: "center",
    alignItems: "center",
    marginTop: 10,
  },
  loginButtonText: {
    color: "white",
    fontSize: 16,
    fontWeight: "600",
  },
  divider: {
    height: 1,
    backgroundColor: "#e5e7eb",
    marginVertical: 20,
  },
  socialContainer: {
    gap: 12,
  },
  signupButton: {
    width: "100%",
    height: 55,
    backgroundColor: "#3b82f6",
    borderRadius: 6,
    justifyContent: "center",
    alignItems: "center",
  },
  signupButtonText: {
    color: "white",
    fontSize: 16,
    fontWeight: "600",
  },
  socialButton: {
    width: "100%",
    alignItems: "center",
  },
  kakaoImage: {
    width: "100%",
    height: 50,
  },
  naverImage: {
    width: "100%",
    height: 50,
  },
});