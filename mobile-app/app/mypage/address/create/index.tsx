import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    KeyboardAvoidingView, Platform,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text, TextInput, TouchableOpacity,
    View
} from 'react-native';
import { fetchApi } from '../../../../lib/api';

export default function AddressCreateScreen() {
  const router = useRouter();

  const [zipCode, setZipCode] = useState("");
  const [roadAddress, setRoadAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  const [loading, setLoading] = useState(false);

  // 모든 항목이 입력되었는지 체크
  const isFormValid = zipCode.trim() !== "" && roadAddress.trim() !== "" && 
                      detailAddress.trim() !== "" && recipientName.trim() !== "" && 
                      recipientPhone.trim() !== "";

  const handleSearchAddress = () => {
    Alert.alert(
      "주소 검색", 
      "React Native 환경에서는 'react-native-webview'를 통한 다음 주소 검색 연동이 필요합니다. 임시로 직접 입력해주세요."
    );
  };

  const handleSubmit = async () => {
    if (!isFormValid) return Alert.alert("알림", "모든 배송지 정보를 입력해주세요.");

    setLoading(true);
    try {
      await fetchApi("/user/addresses", {
        method: "POST",
        body: JSON.stringify({
          zipCode, roadAddress, detailAddress, recipientName, recipientPhone
        }),
      });
      Alert.alert("성공", "새로운 배송지가 추가되었습니다.", [
        { text: "확인", onPress: () => router.back() }
      ]);
    } catch (error: any) {
      Alert.alert("추가 실패", error.message || "배송지 추가 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        
        {/* 상단 헤더 */}
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
            <Ionicons name="arrow-back" size={24} color="#111" />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>새 배송지 추가</Text>
          <View style={{ width: 24 }} />
        </View>

        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          <View style={styles.formContainer}>
            
            <View style={styles.inputGroup}>
              <Text style={styles.label}>수령인</Text>
              <TextInput 
                style={styles.input} value={recipientName} 
                onChangeText={setRecipientName} placeholder="받으시는 분 성함" 
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>연락처</Text>
              <TextInput 
                style={styles.input} value={recipientPhone} 
                onChangeText={setRecipientPhone} placeholder="010-XXXX-XXXX" 
                keyboardType="phone-pad" 
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>주소</Text>
              <View style={styles.row}>
                <TextInput 
                  style={[styles.input, { flex: 1 }]} value={zipCode} 
                  onChangeText={setZipCode} placeholder="우편번호" 
                  keyboardType="number-pad" 
                />
                <TouchableOpacity style={styles.searchBtn} onPress={handleSearchAddress}>
                  <Text style={styles.searchBtnText}>우편번호 검색</Text>
                </TouchableOpacity>
              </View>
              <TextInput 
                style={[styles.input, { marginTop: 10 }]} value={roadAddress} 
                onChangeText={setRoadAddress} placeholder="도로명 주소" 
              />
              <TextInput 
                style={[styles.input, { marginTop: 10 }]} value={detailAddress} 
                onChangeText={setDetailAddress} placeholder="상세 주소를 입력하세요" 
              />
            </View>

          </View>
        </ScrollView>

        <View style={styles.bottomContainer}>
          <TouchableOpacity 
            style={[styles.submitBtn, (!isFormValid || loading) && styles.disabledBtn]} 
            onPress={handleSubmit} 
            disabled={!isFormValid || loading}
          >
            {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.submitBtnText}>저장하기</Text>}
          </TouchableOpacity>
        </View>

      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#111' },
  scrollContent: { padding: 24, paddingBottom: 40 },
  formContainer: { gap: 20 },
  inputGroup: { gap: 8 },
  label: { fontSize: 14, fontWeight: 'bold', color: '#374151' },
  input: { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 16, paddingVertical: 14, fontSize: 15, backgroundColor: '#f9fafb' },
  row: { flexDirection: 'row', gap: 10 },
  searchBtn: { backgroundColor: '#1f2937', justifyContent: 'center', alignItems: 'center', paddingHorizontal: 20, borderRadius: 10 },
  searchBtnText: { color: '#fff', fontSize: 14, fontWeight: 'bold' },
  bottomContainer: { padding: 20, paddingBottom: 30, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: '#f3f4f6' },
  submitBtn: { backgroundColor: '#111', paddingVertical: 16, borderRadius: 12, alignItems: 'center' },
  disabledBtn: { backgroundColor: '#d1d5db' },
  submitBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' }
});