import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React from 'react';
import { SafeAreaView, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

export default function SellerDashboardScreen() {
  const router = useRouter();

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.content}>
        
        <View style={styles.header}>
          <View style={styles.headerTitleRow}>
            <View style={styles.iconBox}><Ionicons name="storefront" size={24} color="#fff" /></View>
            <View>
              <Text style={styles.title}>판매자 센터</Text>
              <Text style={styles.subtitle}>비즈니스 성장을 위한 도구</Text>
            </View>
          </View>
          <TouchableOpacity style={styles.quickAddBtn} onPress={() => router.push('/items/create' as any)}>
            <Ionicons name="add" size={20} color="#fff" />
            <Text style={styles.quickAddBtnText}>새 상품 등록</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.menuGrid}>
          {/* 상품 관리 카드 */}
          <TouchableOpacity style={styles.card} activeOpacity={0.8} onPress={() => router.push('/items/mine' as any)}>
            <View style={styles.cardHeader}>
              <View style={[styles.cardIconBox, { backgroundColor: '#eff6ff' }]}>
                <Ionicons name="cube" size={24} color="#2563eb" />
              </View>
              <Text style={styles.cardTitle}>상품 관리</Text>
            </View>
            <Text style={styles.cardDesc}>등록한 상품 목록을 확인하고 수정하거나 삭제합니다.</Text>
            <View style={styles.cardAction}>
              <Text style={styles.cardActionText}>목록 보기</Text>
              <Ionicons name="arrow-forward" size={16} color="#6b7280" />
            </View>
          </TouchableOpacity>

          {/* 쿠폰 관리 카드 */}
          <TouchableOpacity style={styles.card} activeOpacity={0.8} onPress={() => router.push('/coupons/create' as any)}>
            <View style={styles.cardHeader}>
              <View style={[styles.cardIconBox, { backgroundColor: '#f3e8ff' }]}>
                <Ionicons name="ticket" size={24} color="#9333ea" />
              </View>
              <Text style={styles.cardTitle}>쿠폰 관리</Text>
            </View>
            <Text style={styles.cardDesc}>새로운 할인 쿠폰을 생성하고 발행 내역을 관리합니다.</Text>
            <View style={styles.cardAction}>
              <Text style={styles.cardActionText}>쿠폰 생성</Text>
              <Ionicons name="arrow-forward" size={16} color="#6b7280" />
            </View>
          </TouchableOpacity>
        </View>

        <View style={styles.banner}>
          <Text style={styles.bannerEmoji}>💡</Text>
          <Text style={styles.bannerText}>상품 등록 후 <Text style={{fontWeight: 'bold'}}>내 상품 관리</Text> 페이지에서 정상 노출 여부를 확인하실 수 있습니다.</Text>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#f9fafb' },
  content: { padding: 20, paddingBottom: 60 },
  header: { marginBottom: 30, marginTop: 20 },
  headerTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 20 },
  iconBox: { backgroundColor: '#111', padding: 10, borderRadius: 12 },
  title: { fontSize: 26, fontWeight: '900', color: '#111' },
  subtitle: { fontSize: 13, color: '#6b7280', marginTop: 2 },
  quickAddBtn: { backgroundColor: '#2563eb', flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingVertical: 14, borderRadius: 12, gap: 6, shadowColor: '#2563eb', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8, elevation: 4 },
  quickAddBtnText: { color: '#fff', fontWeight: 'bold', fontSize: 15 },
  menuGrid: { gap: 16 },
  card: { backgroundColor: '#fff', borderRadius: 20, padding: 20, borderWidth: 1, borderColor: '#f3f4f6', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
  cardHeader: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 12 },
  cardIconBox: { padding: 10, borderRadius: 12 },
  cardTitle: { fontSize: 18, fontWeight: 'bold', color: '#111' },
  cardDesc: { fontSize: 14, color: '#6b7280', lineHeight: 20, marginBottom: 20 },
  cardAction: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#f9fafb', padding: 12, borderRadius: 10 },
  cardActionText: { fontSize: 14, fontWeight: 'bold', color: '#374151' },
  banner: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#eff6ff', padding: 16, borderRadius: 16, marginTop: 30, borderWidth: 1, borderColor: '#bfdbfe' },
  bannerEmoji: { fontSize: 20, marginRight: 12 },
  bannerText: { flex: 1, color: '#1e40af', fontSize: 13, lineHeight: 20 }
});