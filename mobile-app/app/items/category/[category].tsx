import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    Dimensions,
    FlatList, Image,
    SafeAreaView,
    StyleSheet,
    Text,
    TouchableOpacity,
    View
} from 'react-native';
import { fetchApi } from '../../../lib/api';

const { width } = Dimensions.get('window');
const ITEM_WIDTH = (width - 48) / 2; 
const DEFAULT_IMAGE_URI = "https://via.placeholder.com/200?text=No+Image";

const CATEGORY_MAP: Record<string, string> = {
  ELECTRONICS: "전자기기",
  CLOTHING: "의류",
  HOME: "가전/생활",
  BOOKS: "도서",
  BEAUTY: "뷰티/화장품",
  OTHERS: "기타",
};

interface Item {
  id: number; name: string; price: number; discountPrice?: number;
  stockStatus: string; thumbnailUrl?: string; images?: string[]; 
  averageRating?: number; reviewCount?: number;
}

interface PageResponse<T> { content: T[]; totalElements: number; }

export default function CategoryItemsScreen() {
  const router = useRouter();
  const { category } = useLocalSearchParams<{ category: string }>();

  const [items, setItems] = useState<Item[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchCategoryItems = async () => {
    if (!category) return;
    try {
      const res = await fetchApi<{ data: PageResponse<Item> }>(`/items?category=${category}&page=0&size=20`);
      if (res.data && Array.isArray(res.data.content)) {
        setItems(res.data.content);
        setTotalCount(res.data.totalElements || res.data.content.length);
      } else {
        setItems([]);
        setTotalCount(0);
      }
    } catch (err) {
      console.error("카테고리 상품 로드 실패:", err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchCategoryItems();
  }, [category]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchCategoryItems();
  };

  const renderItem = ({ item }: { item: Item }) => {
    const imageUrl = item.thumbnailUrl || (item.images && item.images.length > 0 ? item.images[0] : DEFAULT_IMAGE_URI);
    const isSoldOut = item.stockStatus === "OUT_OF_STOCK";

    return (
      <TouchableOpacity style={styles.card} activeOpacity={0.8} onPress={() => router.push(`/items/${item.id}`)}>
        <View style={styles.imageContainer}>
          <Image source={{ uri: imageUrl }} style={styles.image} />
          {isSoldOut && <View style={styles.soldOutOverlay}><Text style={styles.soldOutText}>품절</Text></View>}
        </View>
        
        <View style={styles.infoContainer}>
          <Text style={styles.itemName} numberOfLines={2}>{item.name}</Text>
          <View style={styles.priceRow}>
            {item.discountPrice ? (
              <><Text style={styles.discountPrice}>{item.discountPrice.toLocaleString()}원</Text><Text style={styles.originalPrice}>{item.price.toLocaleString()}원</Text></>
            ) : (
              <Text style={styles.price}>{item.price.toLocaleString()}원</Text>
            )}
          </View>
          <View style={styles.reviewRow}>
            <Ionicons name="star" size={12} color="#fbbf24" />
            <Text style={styles.ratingText}>{(item.averageRating || 0).toFixed(1)}</Text>
            <Text style={styles.reviewCount}>({item.reviewCount || 0})</Text>
          </View>
        </View>
      </TouchableOpacity>
    );
  };

  const categoryName = category ? (CATEGORY_MAP[category.toUpperCase()] || category) : "상품 목록";

  if (loading) {
    return <View style={styles.centerContainer}><ActivityIndicator size="large" color="#111" /></View>;
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Ionicons name="arrow-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{categoryName}</Text>
        <View style={{ width: 24 }} />
      </View>

      <View style={styles.summaryRow}>
        <Text style={styles.summaryText}>총 <Text style={{fontWeight: 'bold', color: '#2563eb'}}>{totalCount}</Text>개의 상품</Text>
      </View>

      <FlatList
        data={items}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderItem}
        numColumns={2}
        contentContainerStyle={styles.listContent}
        columnWrapperStyle={styles.columnWrapper}
        showsVerticalScrollIndicator={false}
        refreshing={refreshing}
        onRefresh={onRefresh}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Ionicons name="file-tray-outline" size={60} color="#d1d5db" />
            <Text style={styles.emptyText}>해당 카테고리에 등록된 상품이 없습니다.</Text>
          </View>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#111' },
  summaryRow: { paddingHorizontal: 20, paddingTop: 16, paddingBottom: 8 },
  summaryText: { fontSize: 13, color: '#4b5563' },
  listContent: { padding: 16, paddingBottom: 60 },
  columnWrapper: { justifyContent: 'space-between', marginBottom: 20 },
  card: { width: ITEM_WIDTH },
  imageContainer: { width: '100%', aspectRatio: 1, borderRadius: 12, backgroundColor: '#f9fafb', overflow: 'hidden', marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  image: { width: '100%', height: '100%' },
  soldOutOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
  soldOutText: { color: '#fff', fontWeight: 'bold', fontSize: 16 },
  infoContainer: { paddingHorizontal: 4 },
  itemName: { fontSize: 14, color: '#374151', lineHeight: 20, marginBottom: 6 },
  priceRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 6, marginBottom: 4 },
  price: { fontSize: 16, fontWeight: 'bold', color: '#111' },
  discountPrice: { fontSize: 16, fontWeight: 'bold', color: '#dc2626' },
  originalPrice: { fontSize: 12, color: '#9ca3af', textDecorationLine: 'line-through', paddingBottom: 2 },
  reviewRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  ratingText: { fontSize: 12, fontWeight: 'bold', color: '#4b5563' },
  reviewCount: { fontSize: 12, color: '#9ca3af' },
  emptyContainer: { paddingVertical: 100, alignItems: 'center' },
  emptyText: { color: '#9ca3af', fontSize: 15, marginTop: 12 }
});