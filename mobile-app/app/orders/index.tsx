import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    SafeAreaView,
    StyleSheet,
    Text,
    TouchableOpacity,
    View
} from 'react-native';
import { fetchApi } from '../../lib/api';

interface OrderItemDTO {
  itemId: number;
  itemName: string;
  quantity: number;
  price: number;
  finalPrice: number;
}

interface OrderDetailDTO {
  orderId: number;
  status: string;
  orderDate: string;
  items: OrderItemDTO[];
  totalPrice: number;
}

export default function OrdersScreen() {
  const router = useRouter();
  const [orders, setOrders] = useState<OrderDetailDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        setLoading(true);
        const response = await fetchApi<{ data: OrderDetailDTO[] }>("/orders/list");
        if (response && response.data) setOrders(response.data);
      } catch (err) {
        console.error("주문 내역 로드 실패:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchOrders();
  }, []);

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PAID": case "결제완료": return { bg: '#eff6ff', text: '#2563eb', border: '#dbeafe' };
      case "CANCELLED": case "취소": return { bg: '#fef2f2', text: '#ef4444', border: '#fee2e2' };
      case "PAYMENT_PENDING": case "결제대기": return { bg: '#fffbeb', text: '#d97706', border: '#fef3c7' };
      default: return { bg: '#f9fafb', text: '#4b5563', border: '#f3f4f6' };
    }
  };

  const renderItem = ({ item }: { item: OrderDetailDTO }) => {
    const statusColors = getStatusColor(item.status);
    
    return (
      <View style={styles.orderCard}>
        {/* 카드 헤더 */}
        <View style={styles.cardHeader}>
          <View>
            <Text style={styles.headerLabel}>ORDER DATE</Text>
            <Text style={styles.orderDate}>{new Date(item.orderDate).toLocaleDateString('ko-KR')}</Text>
          </View>
          <View style={{ alignItems: 'flex-end' }}>
            <Text style={styles.headerLabel}>NO.</Text>
            <Text style={styles.orderId}>#{item.orderId}</Text>
          </View>
        </View>

        {/* 상품 리스트 */}
        <View style={styles.cardBody}>
          {item.items?.map((orderItem, idx) => (
            <View key={`${item.orderId}-${idx}`} style={styles.itemRow}>
              <View style={styles.itemNameContainer}>
                <View style={styles.dot} />
                <Text style={styles.itemName} numberOfLines={1}>{orderItem.itemName}</Text>
                <Text style={styles.itemQty}>x{orderItem.quantity}</Text>
              </View>
              <Text style={styles.itemPrice}>{orderItem.finalPrice.toLocaleString()}원</Text>
            </View>
          ))}

          {/* 총액 및 상태 */}
          <View style={styles.totalRow}>
            <View>
              <Text style={styles.totalLabel}>STATUS</Text>
              <View style={[styles.statusBadge, { backgroundColor: statusColors.bg, borderColor: statusColors.border }]}>
                <Text style={[styles.statusText, { color: statusColors.text }]}>
                  {item.status === 'PAID' ? '결제완료' : item.status === 'CANCELLED' ? '취소됨' : item.status === 'PAYMENT_PENDING' ? '결제대기' : item.status}
                </Text>
              </View>
            </View>
            <View style={{ alignItems: 'flex-end' }}>
              <Text style={styles.totalLabel}>TOTAL AMOUNT</Text>
              <Text style={styles.totalPrice}>{item.totalPrice.toLocaleString()}<Text style={styles.totalPriceWon}>원</Text></Text>
            </View>
          </View>

          {/* 버튼 */}
          <TouchableOpacity 
            style={styles.detailBtn}
            onPress={() => router.push(`/orders/detail/${item.orderId}` as any)}
          >
            <Text style={styles.detailBtnText}>상세 내역 확인하기</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#3b82f6" />
        <Text style={styles.loadingText}>주문 내역을 불러오고 있습니다...</Text>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <FlatList
        data={orders}
        keyExtractor={item => item.orderId.toString()}
        renderItem={renderItem}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        ListHeaderComponent={
          <View style={styles.pageHeader}>
            <Text style={styles.pageTitle}>주문 내역</Text>
            <Text style={styles.pageSubtitle}>최근 고객님이 주문하신 내역입니다.</Text>
          </View>
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyEmoji}>📦</Text>
            <Text style={styles.emptyTitle}>주문 내역이 없습니다.</Text>
            <Text style={styles.emptyDesc}>새로운 상품을 담아보세요!</Text>
            <TouchableOpacity style={styles.shoppingBtn} onPress={() => router.push('/(tabs)')}>
              <Text style={styles.shoppingBtnText}>쇼핑하러 가기</Text>
            </TouchableOpacity>
          </View>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fcfcfc' },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  loadingText: { marginTop: 12, color: '#9ca3af', fontWeight: '500' },
  listContent: { padding: 20, paddingBottom: 60 },
  pageHeader: { alignItems: 'center', marginBottom: 24, marginTop: 16 },
  pageTitle: { fontSize: 28, fontWeight: '900', color: '#111', marginBottom: 6 },
  pageSubtitle: { fontSize: 13, color: '#9ca3af', fontWeight: '500' },
  orderCard: { backgroundColor: '#fff', borderRadius: 24, borderWidth: 1, borderColor: '#f3f4f6', marginBottom: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.03, shadowRadius: 10, elevation: 3 },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', paddingHorizontal: 20, paddingVertical: 16, borderBottomWidth: 1, borderBottomColor: '#f9fafb', backgroundColor: '#fafafa', borderTopLeftRadius: 24, borderTopRightRadius: 24 },
  headerLabel: { fontSize: 10, fontWeight: '900', color: '#d1d5db', marginBottom: 2 },
  orderDate: { fontSize: 14, fontWeight: 'bold', color: '#374151' },
  orderId: { fontSize: 14, fontWeight: '900', color: '#111' },
  cardBody: { padding: 20 },
  itemRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  itemNameContainer: { flexDirection: 'row', alignItems: 'center', flex: 1, paddingRight: 10 },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: '#3b82f6', marginRight: 8, opacity: 0.7 },
  itemName: { fontSize: 15, fontWeight: 'bold', color: '#1f2937', flexShrink: 1 },
  itemQty: { fontSize: 12, fontWeight: '900', color: '#d1d5db', marginLeft: 6 },
  itemPrice: { fontSize: 15, fontWeight: '900', color: '#111' },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: 20, paddingTop: 16, borderTopWidth: 1, borderTopColor: '#f9fafb' },
  totalLabel: { fontSize: 10, fontWeight: '900', color: '#9ca3af', marginBottom: 6 },
  statusBadge: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: 8, borderWidth: 1 },
  statusText: { fontSize: 12, fontWeight: '900' },
  totalPrice: { fontSize: 24, fontWeight: '900', color: '#111' },
  totalPriceWon: { fontSize: 14, fontWeight: 'bold', marginLeft: 2 },
  detailBtn: { marginTop: 20, backgroundColor: '#f9fafb', paddingVertical: 14, borderRadius: 12, borderWidth: 1, borderColor: '#f3f4f6', alignItems: 'center' },
  detailBtnText: { color: '#1f2937', fontSize: 13, fontWeight: '900' },
  emptyContainer: { alignItems: 'center', marginTop: 60 },
  emptyEmoji: { fontSize: 60, marginBottom: 16 },
  emptyTitle: { fontSize: 18, fontWeight: 'bold', color: '#1f2937', marginBottom: 6 },
  emptyDesc: { fontSize: 14, color: '#9ca3af', marginBottom: 24 },
  shoppingBtn: { backgroundColor: '#111', paddingHorizontal: 32, paddingVertical: 14, borderRadius: 24 },
  shoppingBtnText: { color: '#fff', fontWeight: 'bold', fontSize: 15 }
});