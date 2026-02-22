import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import {
    ActivityIndicator, SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TouchableOpacity,
    View
} from 'react-native';
import { fetchApi } from '../../../lib/api';

interface OrderItemDTO {
  orderItemId: number; itemId: number; itemName: string; quantity: number;
  price: number; couponDiscount: number; finalPrice: number; totalPrice: number; isReviewWritten: boolean; 
}
interface AddressDTO { zipCode: string; roadAddress: string; detailAddress: string; recipientName: string; recipientPhone: string; }
interface OrderDetailDTO { orderId: number; tid: string; status: string; orderDate: string; items: OrderItemDTO[]; totalPrice: number; address: AddressDTO; }

export default function OrderDetailScreen() {
  const { orderId } = useLocalSearchParams<{ orderId: string }>();
  const router = useRouter();
  const [order, setOrder] = useState<OrderDetailDTO | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchOrder = useCallback(async () => {
    if (!orderId) return;
    try {
      const res = await fetchApi<{ data: OrderDetailDTO }>(`/orders/detail/${orderId}?t=${new Date().getTime()}`);
      if (res.data) setOrder(res.data);
    } catch (err) {
      console.error("상세 내역 로드 실패:", err);
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  // 화면이 포커스 될 때마다 데이터를 가져옴 (리뷰 작성 후 돌아올 때 갱신용)
  useFocusEffect(
    useCallback(() => {
      fetchOrder();
    }, [fetchOrder])
  );

  if (loading) return (
    <View style={styles.centerContainer}><ActivityIndicator size="large" color="#111" /></View>
  );

  if (!order) return (
    <View style={styles.centerContainer}>
      <Text style={styles.errorText}>주문 내역을 찾을 수 없습니다.</Text>
      <TouchableOpacity style={styles.backBtn} onPress={() => router.push('/orders' as any)}>
        <Text style={styles.backBtnText}>목록으로 돌아가기</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        
        {/* 헤더 */}
        <View style={styles.header}>
          <Text style={styles.statusText}>{order.status === 'PAID' ? '결제완료' : order.status}</Text>
          <Text style={styles.title}>주문 상세 내역</Text>
          <Text style={styles.subText}>주문번호 {order.orderId} | {new Date(order.orderDate).toLocaleString('ko-KR')}</Text>
        </View>

        {/* 상품 정보 카드 */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>주문 상품 <Text style={{color: '#2563eb'}}>{order.items.length}</Text></Text>
          
          {order.items.map((item, index) => (
            <View key={item.orderItemId} style={[styles.itemBlock, index === order.items.length - 1 && { borderBottomWidth: 0 }]}>
              <View style={styles.itemRow}>
                <View style={{ flex: 1, paddingRight: 10 }}>
                  <Text style={styles.itemName} onPress={() => router.push(`/items/${item.itemId}`)}>
                    {item.itemName}
                  </Text>
                  <Text style={styles.itemDesc}>수량 {item.quantity}개 · {item.price.toLocaleString()}원</Text>
                </View>
                <Text style={styles.itemTotalPrice}>{item.totalPrice.toLocaleString()}원</Text>
              </View>

              {(order.status === "PAID" || order.status === "결제 완료") && (
                <TouchableOpacity 
                  style={[styles.reviewBtn, item.isReviewWritten && styles.reviewBtnDone]}
                  disabled={item.isReviewWritten}
                  onPress={() => router.push(`/review?orderItemId=${item.orderItemId}&itemName=${encodeURIComponent(item.itemName)}` as any)}
                >
                  <Text style={[styles.reviewBtnText, item.isReviewWritten && styles.reviewBtnTextDone]}>
                    {item.isReviewWritten ? "리뷰 작성 완료" : "상품 리뷰 작성하기"}
                  </Text>
                </TouchableOpacity>
              )}
            </View>
          ))}

          <View style={styles.totalBlock}>
            <Text style={styles.totalLabel}>총 결제 금액</Text>
            <Text style={styles.totalPriceValue}>{order.totalPrice.toLocaleString()}<Text style={{fontSize:16}}>원</Text></Text>
          </View>
        </View>

        {/* 배송지 카드 */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>배송지 정보</Text>
          <View style={styles.addressRow}>
            <Text style={styles.addressLabel}>받는사람</Text>
            <Text style={styles.addressValue}>{order.address.recipientName} ({order.address.recipientPhone})</Text>
          </View>
          <View style={styles.addressRow}>
            <Text style={styles.addressLabel}>주소</Text>
            <Text style={styles.addressValue}>
              {order.address.roadAddress}{'\n'}
              <Text style={{color: '#6b7280'}}>{order.address.detailAddress}</Text>
            </Text>
          </View>
        </View>

        {/* 하단 버튼 */}
        <View style={styles.bottomBtns}>
          <TouchableOpacity style={styles.secondaryBtn} onPress={() => router.push('/(tabs)')}>
            <Text style={styles.secondaryBtnText}>쇼핑 계속하기</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/orders' as any)}>
            <Text style={styles.primaryBtnText}>주문 내역 목록</Text>
          </TouchableOpacity>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fcfcfc' },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  errorText: { fontSize: 16, fontWeight: 'bold', marginBottom: 20 },
  backBtn: { backgroundColor: '#111', paddingHorizontal: 20, paddingVertical: 12, borderRadius: 20 },
  backBtnText: { color: '#fff', fontWeight: 'bold' },
  scrollContent: { padding: 20, paddingBottom: 60 },
  header: { alignItems: 'center', marginVertical: 20, marginBottom: 30 },
  statusText: { color: '#2563eb', fontWeight: 'bold', fontSize: 13, marginBottom: 6 },
  title: { fontSize: 26, fontWeight: '900', color: '#111', marginBottom: 6 },
  subText: { fontSize: 13, color: '#9ca3af' },
  card: { backgroundColor: '#fff', borderRadius: 24, padding: 24, marginBottom: 20, borderWidth: 1, borderColor: '#f3f4f6', shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.02, shadowRadius: 10, elevation: 2 },
  cardTitle: { fontSize: 16, fontWeight: 'bold', color: '#111', marginBottom: 20 },
  itemBlock: { paddingVertical: 16, borderBottomWidth: 1, borderBottomColor: '#f9fafb' },
  itemRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 },
  itemName: { fontSize: 16, fontWeight: 'bold', color: '#111', lineHeight: 22 },
  itemDesc: { fontSize: 13, color: '#9ca3af', marginTop: 4 },
  itemTotalPrice: { fontSize: 16, fontWeight: '900', color: '#111' },
  reviewBtn: { backgroundColor: '#f9fafb', borderWidth: 1, borderColor: '#f3f4f6', paddingVertical: 12, borderRadius: 12, alignItems: 'center' },
  reviewBtnDone: { backgroundColor: '#f3f4f6', borderColor: '#e5e7eb' },
  reviewBtnText: { color: '#374151', fontSize: 13, fontWeight: 'bold' },
  reviewBtnTextDone: { color: '#9ca3af' },
  totalBlock: { marginTop: 24, paddingTop: 24, borderTopWidth: 2, borderStyle: 'dashed', borderColor: '#f3f4f6', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  totalLabel: { fontWeight: 'bold', color: '#6b7280', fontSize: 14 },
  totalPriceValue: { fontSize: 26, fontWeight: '900', color: '#111' },
  addressRow: { flexDirection: 'row', marginBottom: 16 },
  addressLabel: { width: 70, color: '#9ca3af', fontSize: 14 },
  addressValue: { flex: 1, color: '#1f2937', fontWeight: 'bold', fontSize: 14, lineHeight: 20 },
  bottomBtns: { flexDirection: 'row', gap: 12, marginTop: 10 },
  secondaryBtn: { flex: 1, backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', paddingVertical: 16, borderRadius: 16, alignItems: 'center' },
  secondaryBtnText: { color: '#1f2937', fontWeight: 'bold', fontSize: 14 },
  primaryBtn: { flex: 1, backgroundColor: '#111', paddingVertical: 16, borderRadius: 16, alignItems: 'center' },
  primaryBtnText: { color: '#fff', fontWeight: 'bold', fontSize: 14 }
});