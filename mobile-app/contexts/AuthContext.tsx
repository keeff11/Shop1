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

  useEffect(() => {
    loadUser();
  }, []);

  const loadUser = async () => {
    try {
      const token = await SecureStore.getItemAsync("accessToken");
      if (token) {
        // [수정됨] 토큰 앞에 'Bearer '를 붙여서 백엔드 형식에 맞춤
        const res = await axios.get(`${API_BASE}/auth/me`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        setUser(res.data.data);
      }
    } catch (e) {
      console.log("자동 로그인 실패 (토큰 만료 등):", e);
      setUser(null);
      await SecureStore.deleteItemAsync("accessToken"); 
    } finally {
      setLoading(false);
    }
  };

  const login = async (token: string) => {
    await SecureStore.setItemAsync("accessToken", token);
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