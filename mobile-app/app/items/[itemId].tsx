import axios from "axios";
import Constants from "expo-constants";
import { useLocalSearchParams, useRouter } from "expo-router";
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
import Header from "../../components/Header";

const API_BASE = Constants.expoConfig?.extra?.apiBaseUrl || "";

interface Item {
  id: number;
  name: string;
  price: number;
  discountPrice?: number;
  quantity: number;
  stockStatus: string;
  itemCategory?: string;
  description: string;
  images: string[];
  status: string;
  sellerNickname?: string;
  createdAt: string;
  updatedAt: string;
}

export default function ItemDetailPage() {
  const router = useRouter();
  const params = useLocalSearchParams<{ itemId: string }>();
  const [item, setItem] = useState<Item | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!params.itemId) return;

    axios
      .get(`${API_BASE}/items/${params.itemId}`)
      .then((res) => {
        if (res.data.success) setItem(res.data.data);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }, [params.itemId]);

  const handleAddToCart = async () => {
    if (!item) return;
    try {
      await axios.post(
        `${API_BASE}/cart/add`,
        { itemId: item.id, quantity: 1 },
        { withCredentials: true }
      );
      alert("장바구니에 추가되었습니다!");
    } catch (err) {
      console.error(err);
      alert("장바구니 추가 중 오류가 발생했습니다.");
    }
  };

  const handleBuyNow = () => {
    if (!item) return alert("아이템 정보를 불러오는 중입니다.");
    const checkoutData = {
      type: "SINGLE",
      itemOrders: [{ itemId: item.id, quantity: 1 }],
      addressId: null,
      zipCode: "",
      roadAddress: "",
      detailAddress: "",
      recipientName: "",
      recipientPhone: "",
    };
    sessionStorage.setItem("checkoutData", JSON.stringify(checkoutData));
    router.push("/payments");
  };

  if (loading)
    return (
      <View style={[styles.container, styles.center]}>
        <ActivityIndicator size="large" color="#4F46E5" />
      </View>
    );

  if (!item)
    return (
      <View style={[styles.container, styles.center]}>
        <Text style={styles.errorText}>아이템을 찾을 수 없습니다.</Text>
      </View>
    );

  return (
    <View style={styles.container}>
      <Header />
      <ScrollView contentContainerStyle={styles.scrollContainer}>
        {/* 상단 */}
        <View style={styles.headerRow}>
          <View>
            <Text style={styles.title}>{item.name}</Text>
            <Text style={styles.sellerText}>
              판매자: {item.sellerNickname || "알 수 없음"}
            </Text>
          </View>
          <Text style={styles.dateText}>
            등록일: {new Date(item.createdAt).toLocaleDateString()}
          </Text>
        </View>

        {/* 이미지 */}
        {item.images.length > 0 && (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.imageRow}>
            {item.images.map((img, i) => (
              <Image
                key={i}
                source={{ uri: img }}
                style={styles.image}
              />
            ))}
          </ScrollView>
        )}

        {/* 가격 & 버튼 */}
        <View style={styles.priceContainer}>
          <View>
            <Text style={styles.price}>{item.price.toLocaleString()}원</Text>
            {item.discountPrice && (
              <Text style={styles.discountPrice}>
                할인: {item.discountPrice.toLocaleString()}원
              </Text>
            )}
          </View>

          <View style={styles.buttonRow}>
            <TouchableOpacity style={styles.cartButton} onPress={handleAddToCart}>
              <Text style={styles.buttonText}>장바구니</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.buyButton, !item && styles.disabledButton]}
              onPress={handleBuyNow}
              disabled={!item}
            >
              <Text style={styles.buttonText}>구매하기</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* 설명 */}
        <View style={styles.descriptionContainer}>
          <Text style={styles.descriptionTitle}>상품 설명</Text>
          <Text style={styles.descriptionText}>{item.description}</Text>
        </View>
      </ScrollView>
    </View>
  );
}

const { width } = Dimensions.get("window");

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F3F4F6",
  },
  center: {
    justifyContent: "center",
    alignItems: "center",
  },
  scrollContainer: {
    padding: 15,
    gap: 15,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    flexWrap: "wrap",
  },
  title: {
    fontSize: 24,
    fontWeight: "700",
    color: "#111827",
  },
  sellerText: {
    color: "#6B7280",
    marginTop: 4,
  },
  dateText: {
    color: "#9CA3AF",
    marginTop: 4,
  },
  imageRow: {
    flexDirection: "row",
    marginVertical: 10,
  },
  image: {
    width: width * 0.6,
    height: width * 0.6,
    borderRadius: 8,
    marginRight: 10,
  },
  priceContainer: {
    backgroundColor: "#F9FAFB",
    padding: 12,
    borderRadius: 8,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  price: {
    fontSize: 20,
    fontWeight: "700",
    color: "#4F46E5",
  },
  discountPrice: {
    fontSize: 16,
    fontWeight: "600",
    color: "#EF4444",
  },
  buttonRow: {
    flexDirection: "row",
    gap: 10,
  },
  cartButton: {
    backgroundColor: "#3B82F6",
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 6,
  },
  buyButton: {
    backgroundColor: "#10B981",
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 6,
  },
  disabledButton: {
    backgroundColor: "#9CA3AF",
  },
  buttonText: {
    color: "#fff",
    fontWeight: "600",
    textAlign: "center",
  },
  descriptionContainer: {
    backgroundColor: "#fff",
    padding: 12,
    borderRadius: 8,
    shadowColor: "#000",
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 4,
    elevation: 2,
  },
  descriptionTitle: {
    fontSize: 18,
    fontWeight: "700",
    marginBottom: 6,
  },
  descriptionText: {
    fontSize: 16,
    color: "#111827",
  },

  errorText: { // ✅ 추가
    fontSize: 16,
    fontWeight: "600",
    color: "red",
    textAlign: "center",
  },
});
