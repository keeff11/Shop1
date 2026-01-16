import axios from "axios";
import { useRouter } from "expo-router";
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
import { API_BASE } from "../../config/api"; // 안전하게 import

const { width } = Dimensions.get("window");

interface Item {
  id: number;
  name: string;
  price: number;
  images: { imageUrl: string }[];
}

export default function ItemsPage() {
  const router = useRouter();
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axios
      .get(`${API_BASE}/items`)
      .then((res) => {
        if (res.data.success) setItems(res.data.data);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }, []);

  if (loading)
    return (
      <View
        style={[styles.container, { justifyContent: "center", alignItems: "center" }]}
      >
        <ActivityIndicator size="large" color="#4F46E5" />
      </View>
    );

  return (
    <View style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContainer}>
        <Text style={styles.title}>상품 전체 조회</Text>

        {items.map((item) => (
          <TouchableOpacity
            key={item.id}
            style={styles.card}
            onPress={() => router.push(`/items/${item.id}`)}
          >
            <Image
              source={{
                uri: item.images[0]?.imageUrl || "https://via.placeholder.com/150",
              }}
              style={styles.image}
            />
            <Text style={styles.itemName}>{item.name}</Text>
            <Text style={styles.price}>{item.price.toLocaleString()}원</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F3F4F6",
  },
  scrollContainer: {
    padding: 15,
    gap: 15,
  },
  title: {
    fontSize: 22,
    fontWeight: "700",
    marginBottom: 15,
    textAlign: "center",
    color: "#111827",
  },
  card: {
    backgroundColor: "#fff",
    borderRadius: 8,
    padding: 10,
    shadowColor: "#000",
    shadowOpacity: 0.1,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 4,
    elevation: 3,
    alignItems: "center",
  },
  image: {
    width: width - 40,
    height: 180,
    borderRadius: 6,
    marginBottom: 8,
  },
  itemName: {
    fontSize: 16,
    fontWeight: "600",
    textAlign: "center",
    marginBottom: 4,
    color: "#111827",
  },
  price: {
    fontSize: 16,
    fontWeight: "700",
    color: "#4F46E5",
  },
});
