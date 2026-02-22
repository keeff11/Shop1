import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
    Alert,
    KeyboardAvoidingView, Platform,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text, TextInput, TouchableOpacity,
    View
} from 'react-native';
import { fetchApi } from '../../../lib/api';

export default function CouponCreateScreen() {
  const router = useRouter();

  const [name, setName] = useState("");
  const [discountType, setDiscountType] = useState<"FIXED" | "RATE">("FIXED");
  const [discountValue, setDiscountValue] = useState("");
  const [couponType, setCouponType] = useState<"ALL" | "CATEGORY" | "TARGET">("ALL");
  const [category, setCategory] = useState("");
  const [itemId, setItemId] = useState("");
  const [expiredAt, setExpiredAt] = useState(""); // 단순 텍스트 입력 처리

  const handleSubmit = async () => {
    if (!name || !discountValue || !expiredAt) return Alert.alert("알림", "필수 항목을 모두 입력해주세요.");

    try {
      await fetchApi("/coupons", {
        method: "POST",
        body: JSON.stringify({
          name, discountType, discountValue: Number(discountValue),
          couponType, category: category || undefined, itemId: itemId ? Number(itemId) : undefined,
          expiredAt
        }),
      });
      Alert.alert("성공", "쿠폰 생성 완료!", [{ text: "확인", onPress: () => router.push('/coupons/received' as any) }]);
    } catch (err: any) {
      Alert.alert("실패", err.message);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}><Ionicons name="close" size={24} color="#111" /></TouchableOpacity>
          <Text style={styles.headerTitle}>새 쿠폰 발행</Text>
          <View style={{ width: 24 }} />
        </View>

        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>쿠폰 이름</Text>
            <TextInput style={styles.input} value={name} onChangeText={setName} placeholder="예) 봄맞이 할인 쿠폰" />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>할인 방식</Text>
            <View style={styles.toggleRow}>
              <TouchableOpacity style={[styles.toggleBtn, discountType === "FIXED" && styles.toggleActive]} onPress={() => setDiscountType("FIXED")}>
                <Text style={[styles.toggleText, discountType === "FIXED" && styles.toggleActiveText]}>금액 할인</Text>
              </TouchableOpacity>
              <TouchableOpacity style={[styles.toggleBtn, discountType === "RATE" && styles.toggleActive]} onPress={() => setDiscountType("RATE")}>
                <Text style={[styles.toggleText, discountType === "RATE" && styles.toggleActiveText]}>% 할인</Text>
              </TouchableOpacity>
            </View>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>할인 값</Text>
            <TextInput style={styles.input} value={discountValue} onChangeText={setDiscountValue} placeholder="숫자만 입력" keyboardType="numeric" />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>적용 범위</Text>
            <View style={styles.toggleRow}>
              {(["ALL", "CATEGORY", "TARGET"] as const).map((type) => (
                <TouchableOpacity key={type} style={[styles.toggleBtn, couponType === type && styles.toggleActive]} onPress={() => setCouponType(type)}>
                  <Text style={[styles.toggleText, couponType === type && styles.toggleActiveText]}>
                    {type === "ALL" ? "전체" : type === "CATEGORY" ? "카테고리" : "특정상품"}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          {couponType === "CATEGORY" && (
            <View style={styles.inputGroup}>
              <Text style={styles.label}>카테고리명 (영문)</Text>
              <TextInput style={styles.input} value={category} onChangeText={setCategory} placeholder="예: ELECTRONICS" autoCapitalize="characters" />
            </View>
          )}

          {couponType === "TARGET" && (
            <View style={styles.inputGroup}>
              <Text style={styles.label}>상품 ID</Text>
              <TextInput style={styles.input} value={itemId} onChangeText={setItemId} placeholder="적용할 상품 고유 ID 번호" keyboardType="numeric" />
            </View>
          )}

          <View style={styles.inputGroup}>
            <Text style={styles.label}>만료일시 (YYYY-MM-DDTHH:mm:ss)</Text>
            <TextInput style={styles.input} value={expiredAt} onChangeText={setExpiredAt} placeholder="예: 2026-12-31T23:59:59" />
          </View>
        </ScrollView>

        <View style={styles.bottomBar}>
          <TouchableOpacity style={styles.submitBtn} onPress={handleSubmit}>
            <Text style={styles.submitBtnText}>쿠폰 발행하기</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#111' },
  content: { padding: 24, paddingBottom: 60 },
  inputGroup: { marginBottom: 20 },
  label: { fontSize: 14, fontWeight: 'bold', color: '#374151', marginBottom: 8 },
  input: { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingHorizontal: 16, paddingVertical: 14, fontSize: 15, backgroundColor: '#f9fafb' },
  toggleRow: { flexDirection: 'row', gap: 8 },
  toggleBtn: { flex: 1, paddingVertical: 14, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, alignItems: 'center', backgroundColor: '#f9fafb' },
  toggleActive: { backgroundColor: '#111', borderColor: '#111' },
  toggleText: { fontSize: 14, fontWeight: 'bold', color: '#6b7280' },
  toggleActiveText: { color: '#fff' },
  bottomBar: { padding: 20, borderTopWidth: 1, borderTopColor: '#f3f4f6', backgroundColor: '#fff' },
  submitBtn: { backgroundColor: '#9333ea', paddingVertical: 16, borderRadius: 12, alignItems: 'center' },
  submitBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' }
});