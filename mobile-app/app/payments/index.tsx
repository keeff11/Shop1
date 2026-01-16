import Constants from "expo-constants";
import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Dimensions,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

const { width } = Dimensions.get("window");

// 환경변수 기반 API 주소
export const API_BASE =
  Constants.expoConfig?.extra?.apiBaseUrl ?? "http://localhost:8080";

interface CheckoutData {
  type: "SINGLE" | "CART";
  itemOrders: { itemId: number; quantity: number }[];
}

interface CartItem {
  itemId: number;
  itemName: string;
  price: number;
  quantity: number;
  imageUrl?: string;
  finalPrice?: number;
  appliedCouponId?: number;
  couponDiscount?: number;
}

type PaymentType = "KAKAO_PAY" | "NAVER_PAY";

export default function PaymentPage() {
  const [checkoutData, setCheckoutData] = useState<CheckoutData | null>(null);
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(false);

  // sessionStorage → AsyncStorage 모바일 대응 필요, 임시 사용
  useEffect(() => {
    const data = sessionStorage.getItem("checkoutData");
    if (data) setCheckoutData(JSON.parse(data));
  }, []);

  useEffect(() => {
    if (!checkoutData) return;

    const fetchItems = async () => {
      let items: CartItem[] = [];
      try {
        if (checkoutData.type === "SINGLE") {
          const itemOrder = checkoutData.itemOrders[0];
          const res = await fetch(`${API_BASE}/items/${itemOrder.itemId}`, { credentials: "include" });
          const data = await res.json();
          if (data.data) {
            const item = data.data;
            items = [
              {
                itemId: item.id,
                itemName: item.name,
                price: item.price,
                quantity: itemOrder.quantity,
                imageUrl: item.images?.[0] || "",
                finalPrice: item.price,
              },
            ];
          }
        } else {
          const res = await fetch(`${API_BASE}/cart/list`, { credentials: "include" });
          const data = await res.json();
          if (data.data) {
            const selectedIds = checkoutData.itemOrders.map(o => o.itemId);
            items = data.data
              .filter((i: any) => selectedIds.includes(i.itemId))
              .map((i: any) => ({
                itemId: i.itemId,
                itemName: i.itemName,
                price: i.price,
                quantity: i.quantity,
                imageUrl: i.imageUrl || "",
                finalPrice: i.price,
              }));
          }
        }
        setCartItems(items);
      } catch (err) {
        console.error("아이템 불러오기 실패:", err);
      }
    };

    fetchItems();
  }, [checkoutData]);

  const totalPrice = cartItems.reduce(
    (acc, i) => acc + (i.finalPrice ?? i.price) * i.quantity,
    0
  );

  const requestPayment = async (paymentType: PaymentType) => {
    if (cartItems.length === 0) return;
    setLoading(true);
    try {
      const body: any = {
        approvalUrl: `${API_BASE}/orders/{orderId}?status=success`,
        cancelUrl: `${API_BASE}/cart`,
        failUrl: `${API_BASE}/cart`,
        paymentType,
        itemOrders: cartItems.map(i => ({
          itemId: i.itemId,
          quantity: i.quantity,
          couponId: i.appliedCouponId,
        })),
      };

      const res = await fetch(`${API_BASE}/orders`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      const data = await res.json();
      if (data.success) {
        alert(`${paymentType} 결제 페이지로 이동`);
        // 모바일: Linking.openURL(data.data.redirectUrl) 사용 가능
      } else {
        alert(`${paymentType} 결제 실패`);
      }
    } finally {
      setLoading(false);
    }
  };

  if (!checkoutData || cartItems.length === 0) {
    return (
      <View style={[styles.container, styles.center]}>
        <Text style={styles.errorText}>결제할 상품이 없습니다.</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContainer}>
        <Text style={styles.pageTitle}>결제하기</Text>

        {/* 상품 */}
        <View style={styles.cartContainer}>
          {cartItems.map(item => (
            <View key={item.itemId} style={styles.cartItem}>
              <Text>{item.itemName} × {item.quantity}</Text>
              <Text>{(item.finalPrice! * item.quantity).toLocaleString()}원</Text>
            </View>
          ))}
          <View style={styles.totalRow}>
            <Text style={styles.totalText}>총 결제금액</Text>
            <Text style={styles.totalText}>{totalPrice.toLocaleString()}원</Text>
          </View>
        </View>

        {/* 결제 수단 */}
        <View style={styles.paymentContainer}>
          <Text style={styles.paymentTitle}>결제 수단 선택</Text>

          <TouchableOpacity
            style={styles.paymentButton}
            onPress={() => requestPayment("KAKAO_PAY")}
            disabled={loading}
          >
            <Image source={require("../../assets/kakao_pay.png")} style={styles.paymentImage} />
            <Text style={styles.paymentText}>카카오페이</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.paymentButton}
            onPress={() => requestPayment("NAVER_PAY")}
            disabled={loading}
          >
            <Image source={require("../../assets/naver_pay.svg")} style={styles.paymentImage} />
            <Text style={styles.paymentText}>네이버페이</Text>
          </TouchableOpacity>

          {loading && <ActivityIndicator size="large" color="#4F46E5" style={{ marginTop: 10 }} />}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F3F4F6" },
  center: { justifyContent: "center", alignItems: "center" },
  scrollContainer: { padding: 15, gap: 15 },
  pageTitle: { fontSize: 24, fontWeight: "700", textAlign: "center" },
  errorText: { fontSize: 16, fontWeight: "600", color: "red", textAlign: "center" },
  cartContainer: {
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 12,
    gap: 8,
    shadowColor: "#000",
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 4,
    elevation: 2,
  },
  cartItem: { flexDirection: "row", justifyContent: "space-between", paddingVertical: 4 },
  totalRow: { flexDirection: "row", justifyContent: "space-between", marginTop: 6, borderTopWidth: 1, borderTopColor: "#E5E7EB", paddingTop: 6 },
  totalText: { fontSize: 16, fontWeight: "700" },
  paymentContainer: {
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 12,
    gap: 10,
    alignItems: "center",
    shadowColor: "#000",
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 4,
    elevation: 2,
  },
  paymentTitle: { fontSize: 18, fontWeight: "700" },
  paymentButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderWidth: 1,
    borderColor: "#D1D5DB",
    borderRadius: 8,
    width: width * 0.8,
    justifyContent: "center",
    gap: 10,
  },
  paymentImage: { width: 40, height: 40, resizeMode: "contain" },
  paymentText: { fontSize: 16, fontWeight: "600" },
});
