import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Image,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TouchableOpacity,
    View
} from 'react-native';
import { fetchApi } from '../../lib/api';

const DEFAULT_IMAGE_URI = "https://via.placeholder.com/150?text=No+Image";

// API 응답 타입 정의 (웹 버전을 바탕으로 유추된 구조)
interface CartItem {
  cartItemId: number; // 장바구니 항목의 고유 ID
  itemId: number;     // 실제 상품 ID
  itemName: string;
  price: number;
  quantity: number;
  imageUrl?: string;
}

interface ApiResponse<T> {
  data: T;
}

export default function CartScreen() {
  const router = useRouter();
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);

  // 장바구니 데이터 불러오기
  const fetchCartItems = async () => {
    try {
      setLoading(true);
      // 백엔드 API 경로가 다를 경우 여기에 맞게 수정하세요 (예: /cart/items 또는 /cart)
      const res = await fetchApi<ApiResponse<CartItem[]>>("/cart"); 
      
      if (res && res.data) {
        setCartItems(res.data);
      } else {
        setCartItems([]);
      }
    } catch (err) {
      console.error("장바구니 로드 실패:", err);
      // 서버에서 인증 에러가 난 경우(비로그인 상태)
      setCartItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCartItems();
  }, []);

  // 장바구니 항목 삭제
  const handleRemoveItem = (cartItemId: number) => {
    Alert.alert("상품 삭제", "장바구니에서 이 상품을 삭제하시겠습니까?", [
      { text: "취소", style: "cancel" },
      { 
        text: "삭제", 
        style: "destructive", 
        onPress: async () => {
          try {
            await fetchApi(`/cart/${cartItemId}`, { method: "DELETE" });
            setCartItems(prev => prev.filter(item => item.cartItemId !== cartItemId));
          } catch (err) {
            Alert.alert("오류", "상품 삭제에 실패했습니다.");
          }
        }
      }
    ]);
  };

  // 수량 변경 로직 (+, -)
  const handleQuantityChange = async (cartItemId: number, currentQuantity: number, change: number) => {
    const newQuantity = currentQuantity + change;
    if (newQuantity < 1) return;

    // UI 먼저 업데이트 (Optimistic Update)
    setCartItems(prev => 
      prev.map(item => item.cartItemId === cartItemId ? { ...item, quantity: newQuantity } : item)
    );

    try {
      // 서버에 수량 업데이트 반영 (API 경로 및 데이터 형식은 백엔드에 맞게 조정 필요)
      await fetchApi(`/cart/${cartItemId}/update`, {
        method: "PUT",
        body: JSON.stringify({ quantity: newQuantity })
      });
    } catch (err) {
      console.error(err);
      Alert.alert("오류", "수량 변경에 실패했습니다.");
      // 실패 시 원래 데이터로 롤백하려면 다시 fetch 호출
      fetchCartItems();
    }
  };

  // 결제창으로 이동
  const handleCheckout = () => {
    if (cartItems.length === 0) {
      Alert.alert("알림", "장바구니가 비어 있습니다.");
      return;
    }

    // 결제창에 넘길 데이터 구성
    const checkoutData = {
      type: "CART",
      itemOrders: cartItems.map(item => ({
        itemId: item.itemId,
        quantity: item.quantity,
        itemName: item.itemName,
        price: item.price,
        imageUrl: item.imageUrl || DEFAULT_IMAGE_URI
      })),
      addressId: null, zipCode: "", roadAddress: "", detailAddress: "", recipientName: "", recipientPhone: "",
    };

    router.push({
      pathname: '/payments',
      params: { data: JSON.stringify(checkoutData) }
    });
  };

  // 총 결제 금액 계산
  const totalPrice = cartItems.reduce((acc, item) => acc + (item.price * item.quantity), 0);

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#000" />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>장바구니</Text>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {cartItems.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Ionicons name="cart-outline" size={80} color="#d1d5db" />
            <Text style={styles.emptyText}>장바구니에 담긴 상품이 없습니다.</Text>
            <TouchableOpacity 
              style={styles.goShoppingBtn}
              onPress={() => router.push('/(tabs)')}
            >
              <Text style={styles.goShoppingBtnText}>쇼핑 계속하기</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <View style={styles.cartList}>
            {cartItems.map((item) => (
              <View key={item.cartItemId} style={styles.cartItem}>
                <Image 
                  source={{ uri: item.imageUrl || DEFAULT_IMAGE_URI }} 
                  style={styles.itemImage} 
                />
                
                <View style={styles.itemDetails}>
                  <View style={styles.itemHeader}>
                    <Text style={styles.itemName} numberOfLines={2}>{item.itemName}</Text>
                    <TouchableOpacity onPress={() => handleRemoveItem(item.cartItemId)}>
                      <Ionicons name="close" size={22} color="#9ca3af" />
                    </TouchableOpacity>
                  </View>
                  
                  <Text style={styles.itemPrice}>{item.price.toLocaleString()}원</Text>
                  
                  <View style={styles.quantityContainer}>
                    <TouchableOpacity 
                      style={styles.qtyBtn} 
                      onPress={() => handleQuantityChange(item.cartItemId, item.quantity, -1)}
                      disabled={item.quantity <= 1}
                    >
                      <Ionicons name="remove" size={16} color={item.quantity <= 1 ? "#d1d5db" : "#111"} />
                    </TouchableOpacity>
                    <Text style={styles.qtyText}>{item.quantity}</Text>
                    <TouchableOpacity 
                      style={styles.qtyBtn}
                      onPress={() => handleQuantityChange(item.cartItemId, item.quantity, 1)}
                    >
                      <Ionicons name="add" size={16} color="#111" />
                    </TouchableOpacity>
                  </View>
                </View>
              </View>
            ))}
          </View>
        )}
      </ScrollView>

      {/* 하단 총 결제 금액 및 결제 버튼 바 */}
      {cartItems.length > 0 && (
        <View style={styles.bottomBar}>
          <View style={styles.totalContainer}>
            <Text style={styles.totalLabel}>총 결제 금액</Text>
            <Text style={styles.totalPrice}>{totalPrice.toLocaleString()}원</Text>
          </View>
          <TouchableOpacity style={styles.checkoutBtn} onPress={handleCheckout}>
            <Text style={styles.checkoutBtnText}>
              {cartItems.length}건 결제하기
            </Text>
          </TouchableOpacity>
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#f9fafb' },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#fff' },
  header: {
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  headerTitle: { fontSize: 20, fontWeight: 'bold', color: '#111' },
  scrollContent: { flexGrow: 1, padding: 16, paddingBottom: 100 },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 100,
  },
  emptyText: {
    fontSize: 16,
    color: '#6b7280',
    marginTop: 16,
    marginBottom: 24,
  },
  goShoppingBtn: {
    paddingVertical: 12,
    paddingHorizontal: 24,
    backgroundColor: '#111',
    borderRadius: 8,
  },
  goShoppingBtnText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 15,
  },
  cartList: { gap: 16 },
  cartItem: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
  },
  itemImage: {
    width: 80,
    height: 80,
    borderRadius: 8,
    backgroundColor: '#f3f4f6',
  },
  itemDetails: {
    flex: 1,
    marginLeft: 16,
    justifyContent: 'space-between',
  },
  itemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  itemName: {
    fontSize: 15,
    fontWeight: '600',
    color: '#111',
    flex: 1,
    marginRight: 10,
    lineHeight: 20,
  },
  itemPrice: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#111',
    marginTop: 4,
  },
  quantityContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 10,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 6,
    alignSelf: 'flex-start',
  },
  qtyBtn: {
    padding: 6,
    paddingHorizontal: 12,
  },
  qtyText: {
    fontSize: 14,
    fontWeight: 'bold',
    paddingHorizontal: 8,
    color: '#111',
  },
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: '#fff',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 10,
  },
  totalContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  totalLabel: {
    fontSize: 15,
    color: '#6b7280',
    fontWeight: '500',
  },
  totalPrice: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#111',
  },
  checkoutBtn: {
    backgroundColor: '#000',
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  checkoutBtnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});