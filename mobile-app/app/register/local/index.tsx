import axios from "axios";
import { useRouter } from "expo-router";
import React, { useState } from "react";
import {
    Alert,
    KeyboardAvoidingView,
    Modal,
    Platform,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from "react-native";
// 모바일용 다음 우편번호 컴포넌트
import Postcode from "@actbase/react-daum-postcode";

// ✅ [추가 1] AuthContext 가져오기
import { useAuth } from "../../../contexts/AuthContext";

// 환경 변수 불러오기
const API_URL = process.env.EXPO_PUBLIC_API_URL;

export default function RegisterLocalScreen() {
  const router = useRouter();
  
  // ✅ [추가 2] login 함수 가져오기 (전역 상태 업데이트용)
  const { login } = useAuth();

  // ===== 입력 상태 관리 =====
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [role, setRole] = useState("CUSTOMER");

  // ===== 주소 상태 관리 =====
  const [zipCode, setZipCode] = useState("");
  const [roadAddress, setRoadAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  
  const [isPostcodeModalVisible, setPostcodeModalVisible] = useState(false);

  // 회원가입 핸들러
  const handleSubmit = async () => {
    // 1. 유효성 검사
    if (!email.trim() || !password.trim() || !nickname.trim()) {
      Alert.alert("알림", "기본 정보를 모두 입력해주세요.");
      return;
    }
    if (!zipCode || !roadAddress || !detailAddress) {
      Alert.alert("알림", "주소를 모두 입력해주세요.");
      return;
    }
    if (!API_URL) {
      Alert.alert("오류", "API URL 환경변수가 설정되지 않았습니다.");
      return;
    }

    try {
      // 2. 서버 요청
      const response = await axios.post(
        `${API_URL}/auth/local/sign-up`,
        {
          email,
          password,
          nickname,
          userRole: role,
          zipCode,
          roadAddress,
          detailAddress,
          recipientName,
          recipientPhone,
        }
      );

      // ============================================================
      // ✅ [추가 3] 회원가입 성공 후, 받은 토큰으로 바로 로그인 처리
      // ============================================================
      // 백엔드에서 ApiResponse.success(tokenDto) 형태로 줍니다.
      // response.data 구조: { status: "SUCCESS", data: { accessToken: "...", ... } }
      const tokenDto = response.data.data; 

      if (tokenDto && tokenDto.accessToken) {
        // 토큰 앞에 "Bearer " 붙여서 로그인 처리 -> 헤더(Header.tsx)가 즉시 바뀜
        await login("Bearer " + tokenDto.accessToken);
        
        Alert.alert("환영합니다!", "회원가입과 동시에 로그인되었습니다.", [
            { text: "확인", onPress: () => router.replace("/") }, // 홈으로 이동
        ]);
      } else {
        // 혹시라도 토큰이 안 왔다면 로그인 페이지로 이동
        Alert.alert("가입 성공", "로그인을 진행해주세요.", [
            { text: "확인", onPress: () => router.replace("/register") }
        ]);
      }

    } catch (error: any) {
      console.error("회원가입 실패:", error.response?.data || error.message);
      const errorMessage =
        error.response?.data?.message || "회원가입 중 오류가 발생했습니다.";
      Alert.alert("실패", errorMessage);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : "height"}
        style={{ flex: 1 }}
      >
        <ScrollView contentContainerStyle={styles.scrollContainer}>
          <View style={styles.header}>
            <Text style={styles.headerTitle}>회원가입</Text>
            <Text style={styles.headerSubtitle}>기본 정보를 입력해주세요</Text>
          </View>

          {/* === 기본 정보 섹션 === */}
          <View style={styles.section}>
            <Text style={styles.label}>이메일</Text>
            <TextInput
              style={styles.input}
              placeholder="example@gmail.com"
              placeholderTextColor="#999"
              keyboardType="email-address"
              autoCapitalize="none"
              value={email}
              onChangeText={setEmail}
            />

            <Text style={styles.label}>비밀번호</Text>
            <TextInput
              style={styles.input}
              placeholder="비밀번호 입력"
              placeholderTextColor="#999"
              secureTextEntry
              value={password}
              onChangeText={setPassword}
            />

            <Text style={styles.label}>닉네임</Text>
            <TextInput
              style={styles.input}
              placeholder="닉네임 입력"
              placeholderTextColor="#999"
              value={nickname}
              onChangeText={setNickname}
            />

            <Text style={styles.label}>역할 선택</Text>
            <View style={styles.roleContainer}>
              <TouchableOpacity
                style={[
                  styles.roleButton,
                  role === "CUSTOMER" && styles.roleButtonActive,
                ]}
                onPress={() => setRole("CUSTOMER")}
              >
                <Text
                  style={[
                    styles.roleText,
                    role === "CUSTOMER" && styles.roleTextActive,
                  ]}
                >
                  구매자 (Customer)
                </Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[
                  styles.roleButton,
                  role === "SELLER" && styles.roleButtonActive,
                ]}
                onPress={() => setRole("SELLER")}
              >
                <Text
                  style={[
                    styles.roleText,
                    role === "SELLER" && styles.roleTextActive,
                  ]}
                >
                  판매자 (Seller)
                </Text>
              </TouchableOpacity>
            </View>
          </View>

          <View style={styles.divider} />

          {/* === 배송지 정보 섹션 === */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>배송지 정보</Text>

            <Text style={styles.label}>주소</Text>
            <TouchableOpacity
              onPress={() => setPostcodeModalVisible(true)}
              style={styles.addressSearchButton}
            >
              <Text style={zipCode ? styles.inputText : styles.placeholderText}>
                {zipCode ? `[${zipCode}] ${roadAddress}` : "주소 검색 (클릭)"}
              </Text>
            </TouchableOpacity>

            <TextInput
              style={styles.input}
              placeholder="상세 주소 (예: 101동 101호)"
              placeholderTextColor="#999"
              value={detailAddress}
              onChangeText={setDetailAddress}
            />

            <View style={styles.row}>
              <View style={{ flex: 1, marginRight: 8 }}>
                <Text style={styles.label}>수령인</Text>
                <TextInput
                  style={styles.input}
                  placeholder="이름"
                  placeholderTextColor="#999"
                  value={recipientName}
                  onChangeText={setRecipientName}
                />
              </View>
              <View style={{ flex: 1.5 }}>
                <Text style={styles.label}>연락처</Text>
                <TextInput
                  style={styles.input}
                  placeholder="010-0000-0000"
                  placeholderTextColor="#999"
                  keyboardType="phone-pad"
                  value={recipientPhone}
                  onChangeText={setRecipientPhone}
                />
              </View>
            </View>
          </View>

          <TouchableOpacity style={styles.submitButton} onPress={handleSubmit}>
            <Text style={styles.submitButtonText}>회원가입 완료</Text>
          </TouchableOpacity>
        </ScrollView>

        {/* === 우편번호 검색 모달 === */}
        <Modal
          visible={isPostcodeModalVisible}
          animationType="slide"
          presentationStyle="pageSheet"
        >
          <SafeAreaView style={{ flex: 1 }}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>주소 검색</Text>
              <TouchableOpacity
                onPress={() => setPostcodeModalVisible(false)}
                style={styles.closeButton}
              >
                <Text style={styles.closeButtonText}>닫기</Text>
              </TouchableOpacity>
            </View>
            <Postcode
              style={{ width: "100%", height: "100%" }}
              jsOptions={{ animation: true }}
              onSelected={(data) => {
                setZipCode(String(data.zonecode)); 
                setRoadAddress(data.address);
                setPostcodeModalVisible(false);
              }}
              onError={(err) => {
                console.error(err);
                setPostcodeModalVisible(false);
              }}
            />
          </SafeAreaView>
        </Modal>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#fff",
  },
  scrollContainer: {
    padding: 24,
    paddingBottom: 50,
  },
  header: {
    marginBottom: 30,
    alignItems: "center",
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: "bold",
    color: "#111",
    marginBottom: 8,
  },
  headerSubtitle: {
    fontSize: 16,
    color: "#666",
  },
  section: {
    marginBottom: 10,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: "bold",
    marginBottom: 16,
    color: "#333",
  },
  label: {
    fontSize: 14,
    fontWeight: "600",
    color: "#333",
    marginBottom: 8,
    marginTop: 12,
  },
  input: {
    height: 50,
    borderWidth: 1,
    borderColor: "#ddd",
    borderRadius: 8,
    paddingHorizontal: 16,
    fontSize: 16,
    backgroundColor: "#f9f9f9",
    color: "#000",
  },
  inputText: {
    fontSize: 16,
    color: "#000",
  },
  placeholderText: {
    fontSize: 16,
    color: "#999",
  },
  roleContainer: {
    flexDirection: "row",
    height: 50,
    backgroundColor: "#f0f0f0",
    borderRadius: 8,
    padding: 4,
  },
  roleButton: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    borderRadius: 6,
  },
  roleButtonActive: {
    backgroundColor: "#fff",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  roleText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#999",
  },
  roleTextActive: {
    color: "#3b82f6", 
  },
  divider: {
    height: 1,
    backgroundColor: "#eee",
    marginVertical: 24,
  },
  addressSearchButton: {
    height: 50,
    borderWidth: 1,
    borderColor: "#3b82f6",
    borderRadius: 8,
    paddingHorizontal: 16,
    justifyContent: "center",
    backgroundColor: "#f0f9ff",
    marginBottom: 12,
  },
  row: {
    flexDirection: "row",
  },
  submitButton: {
    backgroundColor: "#3b82f6",
    height: 56,
    borderRadius: 12,
    justifyContent: "center",
    alignItems: "center",
    marginTop: 30,
    shadowColor: "#3b82f6",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 10,
    elevation: 5,
  },
  submitButtonText: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "bold",
  },
  modalHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: "#eee",
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: "bold",
  },
  closeButton: {
    padding: 8,
  },
  closeButtonText: {
    color: "#3b82f6",
    fontSize: 16,
    fontWeight: "600",
  },
});