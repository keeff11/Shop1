import axios from "axios";
import { useLocalSearchParams, useRouter } from "expo-router";
import React, { useState } from "react";
import {
    ActivityIndicator,
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
// 우편번호 검색 라이브러리
import Postcode from "@actbase/react-daum-postcode";

// 환경 변수 (.env)
const API_URL = process.env.EXPO_PUBLIC_API_URL;

export default function AdditionalInfoScreen() {
  const router = useRouter();
  
  // 1. URL 파라미터 받기 (provider, token)
  const params = useLocalSearchParams();
  const provider = params.provider as string; // 'kakao' or 'naver'
  const signUpToken =
    provider === "kakao"
      ? (params.kakaoToken as string)
      : (params.naverToken as string);

  // ===== 상태 관리 =====
  const [nickname, setNickname] = useState("");
  const [userRole, setUserRole] = useState("CUSTOMER");
  const [loading, setLoading] = useState(false);

  // 주소 관련 상태
  const [zipCode, setZipCode] = useState("");
  const [roadAddress, setRoadAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  
  // 모달 표시 여부
  const [isPostcodeModalVisible, setPostcodeModalVisible] = useState(false);

  // 2. 제출 핸들러
  const handleSubmit = async () => {
    // 유효성 검사
    if (!signUpToken || !provider) {
      Alert.alert("오류", "유효하지 않은 접근입니다. (토큰 누락)");
      return;
    }
    if (!nickname.trim()) {
      Alert.alert("알림", "닉네임을 입력해주세요.");
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

    setLoading(true);

    try {
      const endpoint =
        provider === "kakao"
          ? `${API_URL}/auth/kakao/login`
          : `${API_URL}/auth/naver/login`;

      // 서버 요청
      await axios.post(
        endpoint,
        {
          userRole,
          nickname,
          zipCode,
          roadAddress,
          detailAddress,
          recipientName,
          recipientPhone,
        },
        {
          headers: {
            "Content-Type": "application/json",
            // [중요] 백엔드가 요구하는 헤더 키값에 맞춰서 토큰 전송
            Authorization: signUpToken, 
          },
        }
      );

      Alert.alert("성공", "회원가입이 완료되었습니다!", [
        { text: "확인", onPress: () => router.replace("/") },
      ]);
    } catch (error: any) {
      console.error("추가 정보 입력 실패:", error);
      const msg = error.response?.data?.message || "처리 중 오류가 발생했습니다.";
      Alert.alert("실패", msg);
    } finally {
      setLoading(false);
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
            <Text style={styles.headerTitle}>추가 정보 입력</Text>
            <Text style={styles.headerSubtitle}>
              {provider === "kakao" ? "카카오" : "네이버"} 계정으로 가입을 진행합니다.
            </Text>
          </View>

          {/* 닉네임 */}
          <View style={styles.formGroup}>
            <Text style={styles.label}>닉네임</Text>
            <TextInput
              style={styles.input}
              placeholder="닉네임을 입력하세요"
              placeholderTextColor="#999"
              value={nickname}
              onChangeText={setNickname}
            />
          </View>

          {/* 역할 선택 (탭 버튼 스타일) */}
          <View style={styles.formGroup}>
            <Text style={styles.label}>역할</Text>
            <View style={styles.roleContainer}>
              <TouchableOpacity
                style={[
                  styles.roleButton,
                  userRole === "CUSTOMER" && styles.roleButtonActive,
                ]}
                onPress={() => setUserRole("CUSTOMER")}
              >
                <Text
                  style={[
                    styles.roleText,
                    userRole === "CUSTOMER" && styles.roleTextActive,
                  ]}
                >
                  구매자
                </Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[
                  styles.roleButton,
                  userRole === "SELLER" && styles.roleButtonActive,
                ]}
                onPress={() => setUserRole("SELLER")}
              >
                <Text
                  style={[
                    styles.roleText,
                    userRole === "SELLER" && styles.roleTextActive,
                  ]}
                >
                  판매자
                </Text>
              </TouchableOpacity>
            </View>
          </View>

          <View style={styles.divider} />

          {/* 배송지 정보 */}
          <View style={styles.formGroup}>
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
              style={[styles.input, { marginBottom: 12 }]}
              placeholder="상세 주소 입력"
              placeholderTextColor="#999"
              value={detailAddress}
              onChangeText={setDetailAddress}
            />

            <Text style={styles.label}>수령인 이름</Text>
            <TextInput
              style={[styles.input, { marginBottom: 12 }]}
              placeholder="이름"
              placeholderTextColor="#999"
              value={recipientName}
              onChangeText={setRecipientName}
            />

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

          {/* 완료 버튼 */}
          <TouchableOpacity
            style={[styles.submitButton, loading && styles.submitButtonDisabled]}
            onPress={handleSubmit}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.submitButtonText}>가입 완료</Text>
            )}
          </TouchableOpacity>
        </ScrollView>

        {/* 우편번호 모달 */}
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
                setZipCode(String(data.zonecode)); // 문자열 변환 필수
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
    fontSize: 24,
    fontWeight: "bold",
    color: "#111",
    marginBottom: 8,
  },
  headerSubtitle: {
    fontSize: 14,
    color: "#666",
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: "bold",
    marginBottom: 12,
    marginTop: 10,
    color: "#333",
  },
  formGroup: {
    marginBottom: 16,
  },
  label: {
    fontSize: 14,
    fontWeight: "600",
    color: "#333",
    marginBottom: 8,
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
    fontWeight: "bold",
  },
  divider: {
    height: 1,
    backgroundColor: "#eee",
    marginVertical: 20,
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
  submitButton: {
    backgroundColor: "#3b82f6",
    height: 56,
    borderRadius: 12,
    justifyContent: "center",
    alignItems: "center",
    marginTop: 20,
  },
  submitButtonDisabled: {
    backgroundColor: "#93c5fd",
  },
  submitButtonText: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "bold",
  },
  // 모달 스타일
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