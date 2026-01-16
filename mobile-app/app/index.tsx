import { useRouter } from "expo-router";
import React from "react";
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";

export default function Home() {
  const router = useRouter();

  return (
    <View style={styles.container}>
      <ScrollView contentContainerStyle={styles.mainContent}>
        <Text style={styles.title}>
          환영합니다!{"\n"}Shop1에 오신 것을 환영합니다.
        </Text>

        <TouchableOpacity
          style={styles.button}
          onPress={() => router.push("/items")}
        >
          <Text style={styles.buttonText}>전체 상품 보러가기</Text>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F3F4F6", // 웹 bg-gray-50
  },
  mainContent: {
    flexGrow: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: 20,
    gap: 20,
  },
  title: {
    fontSize: 28, // 웹 text-4xl
    fontWeight: "700",
    color: "#374151", // text-gray-700
    textAlign: "center",
  },
  button: {
    backgroundColor: "#4F46E5", // bg-indigo-500
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    shadowColor: "#000",
    shadowOpacity: 0.2,
    shadowOffset: { width: 0, height: 4 },
    shadowRadius: 4,
    elevation: 5, // 안드로이드 그림자
  },
  buttonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
    textAlign: "center",
  },
});
