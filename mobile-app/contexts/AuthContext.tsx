import axios from "axios";
import * as SecureStore from "expo-secure-store";
import React, { createContext, useContext, useEffect, useState } from "react";
import { API_BASE } from "../config/api";

interface AuthContextType {
  user: any;
  loading: boolean;
  login: (token: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  // 앱 켜질 때 토큰 확인
  useEffect(() => {
    loadUser();
  }, []);

  const loadUser = async () => {
    try {
      const token = await SecureStore.getItemAsync("accessToken");
      if (token) {
        // [수정 완료] /api 제거됨 -> http://IP:8080/auth/me
        const res = await axios.get(`${API_BASE}/auth/me`, {
          headers: { Authorization: token },
        });
        setUser(res.data.data);
      }
    } catch (e) {
      // 토큰이 만료되었거나 서버 오류 시 로그아웃 처리
      console.log("자동 로그인 실패 (토큰 만료 등):", e);
      setUser(null);
      await SecureStore.deleteItemAsync("accessToken"); // 잘못된 토큰 삭제
    } finally {
      setLoading(false);
    }
  };

  const login = async (token: string) => {
    // 1. 토큰 저장
    await SecureStore.setItemAsync("accessToken", token);
    // 2. 내 정보 갱신 (헤더 업데이트 효과)
    await loadUser(); 
  };

  const logout = async () => {
    await SecureStore.deleteItemAsync("accessToken");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
};