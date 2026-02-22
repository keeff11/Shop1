import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator, Alert,
    Image,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View
} from 'react-native';
import { fetchApi } from '../../lib/api';

interface UserInfo {
  nickname: string;
  email: string;
  userRole: string;
  profileImg?: string;
  loginType: string;
}

interface Address {
  id: number;
  roadAddress: string;
  detailAddress: string;
  recipientName: string;
  recipientPhone: string;
}

export default function MyPageScreen() {
  const router = useRouter();

  const [user, setUser] = useState<UserInfo | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [userRes, addressRes] = await Promise.all([
          fetchApi<{ data: UserInfo }>("/user/my").catch(() => null),
          fetchApi<{ data: Address[] }>("/user/addresses").catch(() => null)
        ]);
        
        if (userRes?.data) setUser(userRes.data);
        if (addressRes?.data) setAddresses(addressRes.data);
      } catch (err) {
        console.error("데이터 로드 실패:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleChangePassword = async () => {
    if (!currentPassword || !newPassword) {
      return Alert.alert("알림", "비밀번호를 모두 입력해주세요.");
    }
    try {
      await fetchApi("/user/password", {
        method: "PATCH",
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      Alert.alert("성공", "비밀번호가 변경되었습니다.");
      setCurrentPassword('');
      setNewPassword('');
    } catch (err) {
      Alert.alert("실패", "비밀번호 변경에 실패했습니다.");
    }
  };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#000" />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        
        {/* 프로필 카드 */}
        <View style={styles.card}>
          <View style={styles.profileHeader}>
            <Image 
              source={{ uri: user?.profileImg || "https://via.placeholder.com/150?text=Profile" }} 
              style={styles.profileImage} 
            />
            <View style={styles.profileInfo}>
              <Text style={styles.nickname}>{user?.nickname || "로그인 필요"}</Text>
              <Text style={styles.email}>{user?.email || "이메일 정보 없음"}</Text>
              
              <View style={styles.badgeRow}>
                {user?.userRole && (
                  <View style={styles.badge}><Text style={styles.badgeText}>{user.userRole.toUpperCase()}</Text></View>
                )}
                {user?.loginType && (
                  <View style={styles.badge}><Text style={styles.badgeText}>{user.loginType}</Text></View>
                )}
              </View>
            </View>
          </View>
          <TouchableOpacity 
            style={styles.editProfileBtn} 
            onPress={() => router.push('/mypage/edit' as any)}
          >
            <Text style={styles.editProfileText}>프로필 수정</Text>
          </TouchableOpacity>
        </View>

        {/* 배송지 관리 */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>배송지</Text>
          {addresses.length === 0 ? (
            <Text style={styles.emptyText}>등록된 배송지가 없습니다.</Text>
          ) : (
            addresses.map((addr) => (
              <View key={addr.id} style={styles.addressItem}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.addressRoad}>{addr.roadAddress}</Text>
                  <Text style={styles.addressDetail}>
                    {addr.detailAddress} / {addr.recipientName} ({addr.recipientPhone})
                  </Text>
                </View>
                <TouchableOpacity onPress={() => router.push(`/mypage/address/${addr.id}/edit` as any)}>
                  <Text style={styles.editText}>수정</Text>
                </TouchableOpacity>
              </View>
            ))
          )}
          <TouchableOpacity 
            style={styles.actionBtn} 
            onPress={() => router.push('/mypage/address/create' as any)}
          >
            <Text style={styles.actionBtnText}>배송지 추가</Text>
          </TouchableOpacity>
        </View>

        {/* 비밀번호 변경 (LOCAL 계정만) */}
        {user?.loginType === "LOCAL" && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>비밀번호 변경</Text>
            <TextInput 
              style={styles.input} placeholder="현재 비밀번호" 
              secureTextEntry value={currentPassword} onChangeText={setCurrentPassword} 
            />
            <TextInput 
              style={styles.input} placeholder="새 비밀번호" 
              secureTextEntry value={newPassword} onChangeText={setNewPassword} 
            />
            <TouchableOpacity style={styles.primaryBtn} onPress={handleChangePassword}>
              <Text style={styles.primaryBtnText}>변경</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* 빠른 메뉴 (주문내역, 쿠폰) */}
        <View style={styles.menuRow}>
          <TouchableOpacity style={styles.menuCard} onPress={() => router.push('/orders' as any)}>
            <Ionicons name="receipt-outline" size={28} color="#111" />
            <Text style={styles.menuText}>주문 내역</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.menuCard} onPress={() => router.push('/coupons/received' as any)}>
            <Ionicons name="ticket-outline" size={28} color="#111" />
            <Text style={styles.menuText}>내 쿠폰함</Text>
          </TouchableOpacity>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#f9fafb' },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  scrollContent: { padding: 16, paddingBottom: 60 },
  card: { backgroundColor: '#fff', borderRadius: 16, padding: 20, marginBottom: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
  profileHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 16 },
  profileImage: { width: 72, height: 72, borderRadius: 36, borderWidth: 2, borderColor: '#f3f4f6' },
  profileInfo: { flex: 1, marginLeft: 16 },
  nickname: { fontSize: 20, fontWeight: 'bold', color: '#111', marginBottom: 2 },
  email: { fontSize: 13, color: '#6b7280', marginBottom: 8 },
  badgeRow: { flexDirection: 'row', gap: 6 },
  badge: { backgroundColor: '#f3f4f6', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 4 },
  badgeText: { fontSize: 11, fontWeight: 'bold', color: '#4b5563' },
  editProfileBtn: { backgroundColor: '#111', paddingVertical: 12, borderRadius: 8, alignItems: 'center' },
  editProfileText: { color: '#fff', fontWeight: 'bold', fontSize: 14 },
  cardTitle: { fontSize: 16, fontWeight: 'bold', color: '#111', marginBottom: 16 },
  emptyText: { color: '#9ca3af', fontSize: 14, marginBottom: 10 },
  addressItem: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', borderWidth: 1, borderColor: '#f3f4f6', padding: 12, borderRadius: 8, marginBottom: 10 },
  addressRoad: { fontSize: 14, color: '#111', fontWeight: '500', marginBottom: 4 },
  addressDetail: { fontSize: 12, color: '#6b7280' },
  editText: { color: '#2563eb', fontWeight: 'bold', fontSize: 13 },
  actionBtn: { borderWidth: 1, borderColor: '#111', paddingVertical: 10, borderRadius: 8, alignItems: 'center', marginTop: 4 },
  actionBtnText: { color: '#111', fontWeight: 'bold', fontSize: 13 },
  input: { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8, paddingHorizontal: 16, paddingVertical: 12, fontSize: 14, marginBottom: 10 },
  primaryBtn: { backgroundColor: '#111', paddingVertical: 14, borderRadius: 8, alignItems: 'center' },
  primaryBtnText: { color: '#fff', fontWeight: 'bold', fontSize: 14 },
  menuRow: { flexDirection: 'row', gap: 16 },
  menuCard: { flex: 1, backgroundColor: '#fff', borderRadius: 16, padding: 20, alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
  menuText: { marginTop: 10, fontSize: 14, fontWeight: 'bold', color: '#111' }
});