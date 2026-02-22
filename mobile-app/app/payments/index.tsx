import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
  Alert,
  Image,
  KeyboardAvoidingView, Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text, TextInput,
  TouchableOpacity,
  View
} from 'react-native';
import { fetchApi } from '../../lib/api';

// 타입 정의
interface OrderItem {
  itemId: number;
  quantity: number;
  itemName: string;
  price: number;
  imageUrl: string;
}

interface CheckoutData {
  type: string;
  itemOrders: OrderItem[];
}

export default function PaymentsScreen() {
  const router = useRouter();
  const params = useLocalSearchParams();
  const [checkoutData, setCheckoutData] = useState<CheckoutData | null>(null);

  // 배송지 상태 관리
  const [recipientName, setRecipientName] = useState('');
  const [recipientPhone, setRecipientPhone] = useState('');
  const [roadAddress, setRoadAddress] = useState('');
  const [detailAddress, setDetailAddress] = useState('');
  
  // 결제 수단 관리 (카카오, 네이버, 토스 등)
  const [paymentMethod, setPaymentMethod] = useState<'KAKAO' | 'NAVER' | 'TOSS'>('KAKAO');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (params.data && typeof params.data === 'string') {
      try {
        const parsed = JSON.parse(params.data);
        setCheckoutData(parsed);
      } catch (err) {
        Alert.alert("오류", "결제 데이터를 불러올 수 없습니다.");
        router.back();
      }
    }
  }, [params.data]);

  const handlePayment = async () => {
    if (!recipientName || !recipientPhone || !roadAddress) {
      Alert.alert("알림", "배송지 정보를 모두 입력해주세요.");
      return;
    }
    if (!checkoutData) return;

    setLoading(true);
    try {
      const orderPayload = {
        type: checkoutData.type,
        itemOrders: checkoutData.itemOrders.map(item => ({
          itemId: item.itemId,
          quantity: item.quantity
        })),
        paymentType: paymentMethod,
        recipientName,
        recipientPhone,
        roadAddress,
        detailAddress,
        zipCode: "12345", // 우편번호 검색 API(다음 주소 등)는 별도의 WebView나 모듈 연동 필요
      };

      // 백엔드 API 호출 (실제 결제 준비 요청)
      // res.data 에 redirect_url 이 담겨오면 웹뷰를 띄우거나 Linking.openURL로 넘겨야 함
      const res = await fetchApi<any>("/order/create", {
        method: "POST",
        body: JSON.stringify(orderPayload)
      });

      // 임시로 성공 처리
      Alert.alert("성공", "주문이 완료되었습니다!", [
        { text: "확인", onPress: () => router.replace('/(tabs)') }
      ]);
      
    } catch (err) {
      Alert.alert("오류", "결제 처리 중 문제가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  if (!checkoutData) return <View style={styles.container} />;

  const totalPrice = checkoutData.itemOrders.reduce((acc, item) => acc + (item.price * item.quantity), 0);
  const totalQuantity = checkoutData.itemOrders.reduce((acc, item) => acc + item.quantity, 0);

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView 
        style={{ flex: 1 }} 
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          {/* 상품 정보 요약 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>주문 상품 ({totalQuantity}개)</Text>
            {checkoutData.itemOrders.map((item, idx) => (
              <View key={idx} style={styles.orderItem}>
                <Image source={{ uri: item.imageUrl }} style={styles.itemImage} />
                <View style={styles.itemInfo}>
                  <Text style={styles.itemName} numberOfLines={2}>{item.itemName}</Text>
                  <View style={styles.itemBottom}>
                    <Text style={styles.itemPrice}>{item.price.toLocaleString()}원</Text>
                    <Text style={styles.itemQty}>수량: {item.quantity}개</Text>
                  </View>
                </View>
              </View>
            ))}
          </View>

          {/* 배송지 입력 (간소화) */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>배송지 정보</Text>
            <TextInput 
              style={styles.input} 
              placeholder="받으시는 분 성함" 
              value={recipientName}
              onChangeText={setRecipientName}
            />
            <TextInput 
              style={styles.input} 
              placeholder="연락처 (010-XXXX-XXXX)" 
              keyboardType="phone-pad"
              value={recipientPhone}
              onChangeText={setRecipientPhone}
            />
            <TextInput 
              style={styles.input} 
              placeholder="도로명 주소" 
              value={roadAddress}
              onChangeText={setRoadAddress}
            />
            <TextInput 
              style={styles.input} 
              placeholder="상세 주소" 
              value={detailAddress}
              onChangeText={setDetailAddress}
            />
          </View>

          {/* 결제 수단 선택 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>결제 수단</Text>
            <View style={styles.paymentMethods}>
              {(['KAKAO', 'NAVER', 'TOSS'] as const).map(method => (
                <TouchableOpacity 
                  key={method}
                  style={[styles.methodBtn, paymentMethod === method && styles.activeMethodBtn]}
                  onPress={() => setPaymentMethod(method)}
                >
                  <Ionicons 
                    name={paymentMethod === method ? "checkmark-circle" : "ellipse-outline"} 
                    size={20} 
                    color={paymentMethod === method ? "#000" : "#d1d5db"} 
                  />
                  <Text style={[styles.methodText, paymentMethod === method && styles.activeMethodText]}>
                    {method === 'KAKAO' ? '카카오페이' : method === 'NAVER' ? '네이버페이' : '토스페이'}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          {/* 최종 결제 금액 확인 */}
          <View style={styles.section}>
            <View style={styles.totalRow}>
              <Text style={styles.totalLabel}>총 결제 금액</Text>
              <Text style={styles.totalPrice}>{totalPrice.toLocaleString()}원</Text>
            </View>
          </View>
        </ScrollView>

        <View style={styles.bottomBar}>
          <TouchableOpacity 
            style={[styles.payBtn, loading && styles.disabledBtn]} 
            onPress={handlePayment}
            disabled={loading}
          >
            <Text style={styles.payBtnText}>
              {loading ? "처리중..." : `${totalPrice.toLocaleString()}원 결제하기`}
            </Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#f9fafb' },
  container: { flex: 1, backgroundColor: '#fff' },
  scrollContent: { paddingBottom: 100 },
  section: { backgroundColor: '#fff', padding: 20, marginBottom: 12 },
  sectionTitle: { fontSize: 18, fontWeight: 'bold', color: '#111', marginBottom: 16 },
  orderItem: { flexDirection: 'row', marginBottom: 16 },
  itemImage: { width: 64, height: 64, borderRadius: 8, backgroundColor: '#f3f4f6' },
  itemInfo: { flex: 1, marginLeft: 12, justifyContent: 'space-between' },
  itemName: { fontSize: 14, color: '#374151', lineHeight: 20 },
  itemBottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end' },
  itemPrice: { fontSize: 16, fontWeight: 'bold', color: '#111' },
  itemQty: { fontSize: 13, color: '#6b7280' },
  input: { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8, padding: 14, fontSize: 15, marginBottom: 12, backgroundColor: '#fff' },
  paymentMethods: { flexDirection: 'row', gap: 10 },
  methodBtn: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8, paddingVertical: 14 },
  activeMethodBtn: { borderColor: '#000', backgroundColor: '#f9fafb' },
  methodText: { fontSize: 13, fontWeight: '600', color: '#6b7280' },
  activeMethodText: { color: '#000' },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  totalLabel: { fontSize: 16, color: '#4b5563', fontWeight: 'bold' },
  totalPrice: { fontSize: 24, fontWeight: '900', color: '#dc2626' },
  bottomBar: { position: 'absolute', bottom: 0, left: 0, right: 0, backgroundColor: '#fff', padding: 20, borderTopWidth: 1, borderTopColor: '#f3f4f6' },
  payBtn: { backgroundColor: '#000', paddingVertical: 16, borderRadius: 12, alignItems: 'center' },
  disabledBtn: { opacity: 0.5 },
  payBtnText: { color: '#fff', fontSize: 18, fontWeight: 'bold' }
});