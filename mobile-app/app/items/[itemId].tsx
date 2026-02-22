import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator, Alert, Dimensions,
  Image, ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View
} from 'react-native';
import { fetchApi } from '../../lib/api';

const { width } = Dimensions.get('window');
const DEFAULT_IMAGE_URI = "https://via.placeholder.com/400?text=No+Image";

const categoryMap: Record<string, string> = {
  ELECTRONICS: "전자기기",
  CLOTHING: "의류",
  HOME: "가전/생활",
  BOOKS: "도서",
  BEAUTY: "뷰티/화장품",
  OTHERS: "기타",
};

// 타입 정의 (웹과 동일)
interface Item {
  id: number;
  name: string;
  price: number;
  discountPrice?: number;
  quantity: number;
  stockStatus: string;
  category: string; 
  description: string;
  thumbnailUrl?: string; 
  images: string[]; 
  status: string;
  sellerId: number;
  sellerNickname?: string;
  createdAt: string;
  updatedAt: string;
  averageRating?: number;
  reviewCount?: number;
  viewCount?: number;
}

interface Review {
  reviewId: number;
  nickname: string;
  rating: number;
  content: string;
  imageUrls: string[];
  createdAt: string;
  isOwner: boolean;
}

interface ApiResponse<T> { data: T; }
interface PageResponse<T> { content: T[]; totalPages: number; totalElements: number; last: boolean; }
interface UserInfo { id?: number; userId?: number; email: string; nickname: string; }

export default function ItemDetailScreen() {
  const router = useRouter();
  const { itemId } = useLocalSearchParams<{ itemId: string }>();
  
  const [item, setItem] = useState<Item | null>(null);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState<string>("");
  const [activeTab, setActiveTab] = useState<"detail" | "reviews">("detail");
  const [reviews, setReviews] = useState<Review[]>([]);

  const scrollViewRef = useRef<ScrollView>(null);

  useEffect(() => {
    if (!itemId) return;

    const fetchData = async () => {
      try {
        setLoading(true);

        const itemRes = await fetchApi<ApiResponse<Item>>(`/items/${itemId}`);
        const itemData = itemRes.data;
        setItem(itemData);

        if (itemData.thumbnailUrl) setSelectedImage(itemData.thumbnailUrl);
        else if (itemData.images && itemData.images.length > 0) setSelectedImage(itemData.images[0]);
        else setSelectedImage(DEFAULT_IMAGE_URI);

        try {
          const reviewRes = await fetchApi<ApiResponse<PageResponse<Review>>>(`/reviews/items/${itemId}`);
          if (reviewRes.data && Array.isArray(reviewRes.data.content)) {
            setReviews(reviewRes.data.content);
          } else {
            setReviews([]);
          }
        } catch (error) {
          setReviews([]);
        }

        try {
          const userRes = await fetchApi<ApiResponse<UserInfo>>("/user/my");
          if (userRes.data) {
            const myId = userRes.data.userId || userRes.data.id;
            setCurrentUserId(myId || null);
          }
        } catch (error) {
          console.log("로그인 필요 상태");
        }
      } catch (err) {
        console.error("데이터 로드 실패:", err);
        setItem(null);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [itemId]);

  const handleAddToCart = async () => {
    if (!item) return;
    try {
      await fetchApi("/cart/add", {
        method: "POST",
        body: JSON.stringify({ itemId: item.id, quantity: 1 }),
      });
      Alert.alert("성공", "장바구니에 추가되었습니다!");
    } catch (err) {
      console.error(err);
      Alert.alert("오류", "장바구니 추가 실패");
    }
  };

  const handleBuyNow = () => {
    if (!item) return;
    const finalPrice = item.discountPrice ? item.discountPrice : item.price;
    const checkoutData = {
      type: "SINGLE",
      itemOrders: [{ 
        itemId: item.id, quantity: 1, itemName: item.name, 
        price: finalPrice, imageUrl: selectedImage || DEFAULT_IMAGE_URI 
      }],
      addressId: null, zipCode: "", roadAddress: "", detailAddress: "", recipientName: "", recipientPhone: "",
    };
    // RN에서는 Params나 별도의 Context, Zustand 등을 사용해 결제창으로 넘기는 것이 좋습니다.
    router.push({
      pathname: '/payments',
      params: { data: JSON.stringify(checkoutData) }
    });
  };

  const handleDeleteItem = () => {
      Alert.alert("상품 삭제", "정말로 이 상품을 삭제하시겠습니까?", [
        { text: "취소", style: "cancel" },
        { text: "삭제", style: "destructive", onPress: async () => {
            try {
              await fetchApi(`/items/${item?.id}`, { method: "DELETE" });
              Alert.alert("성공", "상품이 삭제되었습니다.");
              
              // 변경된 부분: index 생략
              router.replace("/(tabs)"); 
              
            } catch (err) {
              Alert.alert("오류", "삭제 실패");
            }
        }}
      ]);
    };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#000" />
      </View>
    );
  }
  
  if (!item) {
    return (
      <View style={styles.centerContainer}>
        <Text>상품 정보를 찾을 수 없습니다.</Text>
      </View>
    );
  }

  const discountRate = item.discountPrice ? Math.round(((item.price - item.discountPrice) / item.price) * 100) : 0;
  const rating = item.averageRating || 0;
  const isOwner = currentUserId && item.sellerId && (Number(currentUserId) === Number(item.sellerId));

  return (
    <View style={styles.container}>
      <ScrollView ref={scrollViewRef} contentContainerStyle={styles.scrollContent}>
        {/* 이미지 영역 */}
        <Image source={{ uri: selectedImage || DEFAULT_IMAGE_URI }} style={styles.mainImage} />
        {item.images && item.images.length > 1 && (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.thumbnailList}>
            {item.images.map((img, idx) => (
              <TouchableOpacity key={idx} onPress={() => setSelectedImage(img)}>
                <Image 
                  source={{ uri: img }} 
                  style={[styles.thumbnail, selectedImage === img && styles.selectedThumbnail]} 
                />
              </TouchableOpacity>
            ))}
          </ScrollView>
        )}

        {/* 정보 영역 */}
        <View style={styles.infoContainer}>
          <View style={styles.categoryRow}>
            <Text style={styles.sellerName}>{item.sellerNickname || "Official Store"}</Text>
            <Text style={styles.dot}> • </Text>
            <Text style={styles.categoryText}>{categoryMap[item.category] || item.category || "기타"}</Text>
            <View style={styles.flexSpacer} />
            <Ionicons name="eye-outline" size={14} color="#9ca3af" />
            <Text style={styles.viewCount}> {item.viewCount?.toLocaleString() || 0}</Text>
          </View>

          <View style={styles.titleRow}>
            <Text style={styles.title}>{item.name}</Text>
          </View>

          {isOwner && (
            <View style={styles.ownerActions}>
              <TouchableOpacity style={styles.editBtn}>
                <Text style={styles.editBtnText}>수정</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.deleteBtn} onPress={handleDeleteItem}>
                <Text style={styles.deleteBtnText}>삭제</Text>
              </TouchableOpacity>
            </View>
          )}

          <View style={styles.ratingRow}>
            {[...Array(5)].map((_, i) => (
              <Ionicons key={i} name={i < Math.round(rating) ? "star" : "star-outline"} size={16} color="#fbbf24" />
            ))}
            <Text style={styles.ratingText}>{rating.toFixed(1)}</Text>
            <Text style={styles.reviewCountText}>리뷰 {item.reviewCount || 0}개</Text>
          </View>

          <View style={styles.priceContainer}>
            {item.discountPrice ? (
              <View style={styles.priceRow}>
                <Text style={styles.discountRate}>{discountRate}%</Text>
                <Text style={styles.finalPrice}>{item.discountPrice.toLocaleString()}원</Text>
                <Text style={styles.originalPrice}>{item.price.toLocaleString()}원</Text>
              </View>
            ) : (
              <Text style={styles.finalPrice}>{item.price.toLocaleString()}원</Text>
            )}
          </View>
        </View>

        {/* 탭 영역 */}
        <View style={styles.tabContainer}>
          <TouchableOpacity style={[styles.tab, activeTab === "detail" && styles.activeTab]} onPress={() => setActiveTab("detail")}>
            <Text style={[styles.tabText, activeTab === "detail" && styles.activeTabText]}>상품 상세</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.tab, activeTab === "reviews" && styles.activeTab]} onPress={() => setActiveTab("reviews")}>
            <Text style={[styles.tabText, activeTab === "reviews" && styles.activeTabText]}>리뷰 ({item.reviewCount || 0})</Text>
          </TouchableOpacity>
        </View>

        {/* 탭 컨텐츠 */}
        <View style={styles.tabContent}>
          {activeTab === "detail" && (
            <Text style={styles.descriptionText}>{item.description}</Text>
          )}

          {activeTab === "reviews" && (
            <View>
              {reviews.length > 0 ? (
                reviews.map(review => (
                  <View key={review.reviewId} style={styles.reviewItem}>
                    <View style={styles.reviewHeader}>
                      <View style={styles.reviewUser}>
                        <View style={styles.avatar}><Text style={styles.avatarText}>{review.nickname ? review.nickname[0] : "?"}</Text></View>
                        <View>
                          <Text style={styles.reviewNickname}>{review.nickname}</Text>
                          <Text style={styles.reviewDate}>{new Date(review.createdAt).toLocaleDateString()}</Text>
                        </View>
                      </View>
                      <View style={styles.ratingRow}>
                        {[...Array(5)].map((_, i) => (
                          <Ionicons key={i} name={i < review.rating ? "star" : "star-outline"} size={12} color="#fbbf24" />
                        ))}
                      </View>
                    </View>
                    <Text style={styles.reviewContent}>{review.content}</Text>
                  </View>
                ))
              ) : (
                <Text style={styles.emptyText}>작성된 리뷰가 없습니다.</Text>
              )}
            </View>
          )}
        </View>
      </ScrollView>

      {/* 하단 고정 액션 버튼 */}
      <View style={styles.bottomBar}>
        <TouchableOpacity 
          style={[styles.cartBtn, item.stockStatus === "OUT_OF_STOCK" && styles.disabledBtn]} 
          onPress={handleAddToCart}
          disabled={item.stockStatus === "OUT_OF_STOCK"}
        >
          <Text style={styles.cartBtnText}>장바구니</Text>
        </TouchableOpacity>
        <TouchableOpacity 
          style={[styles.buyBtn, item.stockStatus === "OUT_OF_STOCK" && styles.disabledBuyBtn]} 
          onPress={handleBuyNow}
          disabled={item.stockStatus === "OUT_OF_STOCK"}
        >
          <Text style={styles.buyBtnText}>{item.stockStatus === "OUT_OF_STOCK" ? "품절" : "구매하기"}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  scrollContent: { paddingBottom: 100 },
  mainImage: { width: width, height: width, backgroundColor: '#f9fafb' },
  thumbnailList: { flexDirection: 'row', padding: 10 },
  thumbnail: { width: 60, height: 60, borderRadius: 8, marginRight: 8, borderWidth: 2, borderColor: 'transparent' },
  selectedThumbnail: { borderColor: '#000' },
  infoContainer: { padding: 20 },
  categoryRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 10 },
  sellerName: { fontWeight: 'bold', color: '#111' },
  dot: { color: '#ccc' },
  categoryText: { color: '#4b5563', fontSize: 13 },
  flexSpacer: { flex: 1 },
  viewCount: { fontSize: 12, color: '#9ca3af' },
  titleRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 },
  title: { fontSize: 24, fontWeight: '900', color: '#111', flex: 1 },
  ownerActions: { flexDirection: 'row', gap: 8, marginBottom: 10 },
  editBtn: { borderWidth: 1, borderColor: '#e5e7eb', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 6 },
  editBtnText: { fontSize: 12, fontWeight: 'bold', color: '#374151' },
  deleteBtn: { backgroundColor: '#fef2f2', borderWidth: 1, borderColor: '#fecaca', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 6 },
  deleteBtnText: { fontSize: 12, fontWeight: 'bold', color: '#dc2626' },
  ratingRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 20 },
  ratingText: { fontWeight: 'bold', fontSize: 16, marginLeft: 4, marginRight: 8, color: '#111' },
  reviewCountText: { fontSize: 13, color: '#9ca3af', textDecorationLine: 'underline' },
  priceContainer: { backgroundColor: '#f9fafb', padding: 16, borderRadius: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  priceRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 8 },
  discountRate: { fontSize: 24, fontWeight: '900', color: '#dc2626' },
  finalPrice: { fontSize: 28, fontWeight: '900', color: '#111' },
  originalPrice: { fontSize: 14, color: '#9ca3af', textDecorationLine: 'line-through', marginBottom: 4 },
  tabContainer: { flexDirection: 'row', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  tab: { flex: 1, paddingVertical: 16, alignItems: 'center' },
  activeTab: { borderBottomWidth: 2, borderBottomColor: '#000' },
  tabText: { fontSize: 16, fontWeight: 'bold', color: '#9ca3af' },
  activeTabText: { color: '#000' },
  tabContent: { padding: 20 },
  descriptionText: { fontSize: 15, color: '#4b5563', lineHeight: 24 },
  reviewItem: { borderBottomWidth: 1, borderBottomColor: '#f3f4f6', paddingVertical: 16 },
  reviewHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  reviewUser: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  avatar: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#f3f4f6', alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontWeight: 'bold', color: '#9ca3af' },
  reviewNickname: { fontWeight: 'bold', color: '#111' },
  reviewDate: { fontSize: 11, color: '#9ca3af' },
  reviewContent: { color: '#374151', paddingLeft: 46, marginTop: 4 },
  emptyText: { textAlign: 'center', color: '#9ca3af', marginTop: 40 },
  bottomBar: { position: 'absolute', bottom: 0, left: 0, right: 0, backgroundColor: '#fff', flexDirection: 'row', padding: 16, paddingBottom: 30, borderTopWidth: 1, borderTopColor: '#f3f4f6', gap: 12 },
  cartBtn: { flex: 1, borderWidth: 2, borderColor: '#e5e7eb', borderRadius: 12, alignItems: 'center', justifyContent: 'center', height: 56 },
  cartBtnText: { fontWeight: 'bold', fontSize: 16, color: '#374151' },
  buyBtn: { flex: 2, backgroundColor: '#000', borderRadius: 12, alignItems: 'center', justifyContent: 'center', height: 56 },
  buyBtnText: { fontWeight: 'bold', fontSize: 16, color: '#fff' },
  disabledBtn: { opacity: 0.5 },
  disabledBuyBtn: { backgroundColor: '#9ca3af' }
});