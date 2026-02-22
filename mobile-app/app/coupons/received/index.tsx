import React, { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { fetchApi } from '../../../lib/api';

interface CouponResponse {
  couponId: number; name: string; discountType: "FIXED" | "RATE"; discountValue: number;
  couponType: "ALL" | "CATEGORY" | "TARGET"; category?: string; itemId?: number; used: boolean;
}

export default function ReceivedCouponsScreen() {
  const [coupons, setCoupons] = useState<CouponResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchApi<{ data: CouponResponse[] }>("/coupons/my")
      .then((data) => setCoupons(data.data || []))
      .catch(err => console.error(err))
      .finally(() => setLoading(false));
  }, []);

  const renderItem = ({ item }: { item: CouponResponse }) => (
    <View style={[styles.card, item.used && styles.cardUsed]}>
      <View style={styles.cardHeader}>
        <Text style={[styles.couponName, item.used && styles.textUsed]}>{item.name}</Text>
        <View style={[styles.badge, item.used && styles.badgeUsed]}>
          <Text style={[styles.badgeText, item.used && styles.textUsed]}>
            {item.used ? "사용 완료" : "사용 가능"}
          </Text>
        </View>
      </View>
      <Text style={[styles.discountText, item.used && styles.textUsed]}>
        {item.discountType === "FIXED" ? `${item.discountValue.toLocaleString()}원 할인` : `${item.discountValue}% 할인`}
      </Text>
      <View style={styles.infoRow}>
        <Text style={styles.infoText}>타입: {item.couponType}</Text>
        {item.couponType === "CATEGORY" && <Text style={styles.infoText}> · 카테고리: {item.category}</Text>}
        {item.couponType === "TARGET" && <Text style={styles.infoText}> · 상품 ID: {item.itemId}</Text>}
      </View>
    </View>
  );

  if (loading) return <View style={styles.center}><ActivityIndicator size="large" color="#111" /></View>;

  return (
    <SafeAreaView style={styles.safeArea}>
      <FlatList
        data={coupons}
        keyExtractor={item => item.couponId.toString()}
        renderItem={renderItem}
        contentContainerStyle={styles.list}
        ListHeaderComponent={<Text style={styles.pageTitle}>내 쿠폰함</Text>}
        ListEmptyComponent={<Text style={styles.emptyText}>보유 중인 쿠폰이 없습니다.</Text>}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#f9fafb' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  list: { padding: 20 },
  pageTitle: { fontSize: 24, fontWeight: 'bold', color: '#111', marginBottom: 20 },
  card: { backgroundColor: '#fff', padding: 20, borderRadius: 16, marginBottom: 16, borderWidth: 1, borderColor: '#e5e7eb', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
  cardUsed: { backgroundColor: '#f3f4f6' },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  couponName: { fontSize: 18, fontWeight: 'bold', color: '#111' },
  badge: { backgroundColor: '#eff6ff', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 8 },
  badgeUsed: { backgroundColor: '#e5e7eb' },
  badgeText: { color: '#2563eb', fontSize: 12, fontWeight: 'bold' },
  textUsed: { color: '#9ca3af' },
  discountText: { fontSize: 24, fontWeight: '900', color: '#dc2626', marginBottom: 12 },
  infoRow: { flexDirection: 'row', alignItems: 'center' },
  infoText: { fontSize: 13, color: '#6b7280' },
  emptyText: { textAlign: 'center', color: '#9ca3af', marginTop: 40 }
});