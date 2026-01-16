import axios from "axios";
import { useRouter } from "expo-router";
import * as SecureStore from "expo-secure-store"; // 토큰 가져오기용
import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Image,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

import { API_BASE } from "../config/api";
import { useAuth } from "../contexts/AuthContext"; // ✅ Context import

export default function Header() {
  const router = useRouter();
  
  // ✅ 1. Context에서 로그인 정보(user)와 로그아웃 함수를 가져옴
  // 이제 Header가 스스로 로딩할 필요 없이, AuthProvider가 준 정보를 보여주기만 하면 됨
  const { user, loading: authLoading, logout } = useAuth(); 
  
  const [cartCount, setCartCount] = useState(0);

  // ✅ 2. 유저가 있을 때만 장바구니 개수 갱신
  useEffect(() => {
    if (user) {
      fetchCartCount();
    } else {
      setCartCount(0);
    }
  }, [user]); // user 상태가 변할 때마다 실행됨

  const fetchCartCount = async () => {
    try {
      // 저장된 토큰 꺼내기
      const token = await SecureStore.getItemAsync("accessToken");
      if (!token) return;

      // ✅ 3. 헤더에 토큰을 실어서 장바구니 조회 (withCredentials 삭제)
      const res = await axios.get(`${API_BASE}/cart/list`, {
        headers: { Authorization: token },
      });

      const count =
        res.data.data?.reduce((acc: number, item: any) => acc + item.quantity, 0) || 0;
      setCartCount(count);
    } catch (err) {
      console.error("장바구니 조회 실패:", err);
    }
  };

  // ✅ 4. 로그아웃 처리
  const handleLogout = async () => {
    try {
        // (선택) 서버에도 로그아웃 요청을 보내고 싶다면 여기서 수행
        // await axios.post(`${API_BASE}/auth/logout`, {}, { headers: { Authorization: token } });
        
        await logout(); // 앱 내부 로그아웃 (토큰 삭제 & 상태 초기화)
        router.replace("/");
    } catch (e) {
        console.error(e);
    }
  };

  return (
    <View style={styles.headerContainer}>
      {/* ===== 로고 / 뒤로가기 ===== */}
      <View style={styles.left}>
        <TouchableOpacity onPress={() => router.push("/")}>
          <Image
            source={require("../assets/Shop1.png")}
            style={styles.logo}
            resizeMode="contain"
          />
        </TouchableOpacity>
      </View>

      {/* ===== 우측 버튼 영역 ===== */}
      <View style={styles.right}>
        {authLoading ? (
          // 로딩 중일 때
          <ActivityIndicator color="#555" />
        ) : user ? (
          // ✅ 로그인 상태일 때
          <>
            <View style={styles.userInfo}>
              <Text style={styles.nickname}>{user.nickname}님</Text>
              <TouchableOpacity onPress={handleLogout}>
                <Text style={styles.logout}>로그아웃</Text>
              </TouchableOpacity>
            </View>

            {/* 장바구니 아이콘 */}
            <TouchableOpacity
              style={styles.iconButton}
              onPress={() => router.push("/")}
            >
              <Image
                source={require("../assets/cart.png")}
                style={styles.icon}
              />
              {cartCount > 0 && (
                <View style={styles.cartBadge}>
                  <Text style={styles.cartBadgeText}>{cartCount}</Text>
                </View>
              )}
            </TouchableOpacity>
          </>
        ) : (
          // ✅ 비로그인 상태일 때
          <TouchableOpacity
            style={styles.loginButton}
            onPress={() => router.push("/register")}
          >
            <Text style={styles.loginText}>로그인 / 회원가입</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  headerContainer: {
    height: 100, // 상태바 영역 고려하여 높이 조정 (필요 시 수정)
    paddingTop: 40, // iOS 노치 영역 대응 (SafeAreaView를 안 쓸 경우)
    backgroundColor: "#E6F4FE",
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 15,
    borderBottomWidth: 1,
    borderBottomColor: "#ccc",
  },
  left: {
    flexDirection: "row",
    alignItems: "center",
  },
  logo: {
    width: 100,
    height: 40,
  },
  right: {
    flexDirection: "row",
    alignItems: "center",
  },
  userInfo: {
    flexDirection: "row",
    alignItems: "center",
    marginRight: 10,
  },
  nickname: {
    fontSize: 14,
    fontWeight: "600",
    marginRight: 8,
    color: "#333",
  },
  logout: {
    fontSize: 12,
    color: "#FF3B30",
    fontWeight: "500",
  },
  iconButton: {
    marginLeft: 5,
    padding: 5,
  },
  icon: {
    width: 28,
    height: 28,
  },
  cartBadge: {
    position: "absolute",
    top: 0,
    right: 0,
    backgroundColor: "#007AFF",
    width: 16,
    height: 16,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "#E6F4FE",
  },
  cartBadgeText: {
    color: "#fff",
    fontSize: 9,
    fontWeight: "700",
  },
  loginButton: {
    backgroundColor: "#007AFF",
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 6,
  },
  loginText: {
    color: "#fff",
    fontSize: 14,
    fontWeight: "600",
  },
});