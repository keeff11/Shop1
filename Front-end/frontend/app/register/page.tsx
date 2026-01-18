"use client";

import { fetchApi } from "@/lib/api";
import Link from "next/link";
import Image from "next/image";
import { useState } from "react";

export default function RegisterPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  /**
   * * 로컬 로그인 핸들러
   * */
  const handleLocalLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await fetchApi("/auth/local/login", {
        method: "POST",
        credentials: "include", 
        body: JSON.stringify({ email, password }),
      });

      window.location.href = "/home";
    } catch (err) {
      console.error(err);
      alert("로그인 중 오류가 발생했습니다.");
    }
  };

  /**
   * 
   * 카카오 로그인 핸들러
   *
   **/
  const handleKakaoLogin = () => {
    const REST_API_KEY = process.env.NEXT_PUBLIC_KAKAO_REST_KEY!;
    const REDIRECT_BASE = process.env.NEXT_PUBLIC_API_BASE_URL;
    const REDIRECT_URI = encodeURIComponent(`${REDIRECT_BASE}/auth/kakao/callback`);
    
    const kakaoAuthUrl = `${process.env.NEXT_PUBLIC_KAKAO_AUTH_URL}/oauth/authorize?response_type=code&client_id=${REST_API_KEY}&redirect_uri=${REDIRECT_URI}`;
    window.location.href = kakaoAuthUrl;
  };

  /**
   * 
   * 네이버 로그인 핸들러
   * 
   **/
  const handleNaverLogin = () => {
    const CLIENT_ID = process.env.NEXT_PUBLIC_NAVER_CLIENT_ID!;
    const REDIRECT_BASE = process.env.NEXT_PUBLIC_REDIRECT_URI_BASE;
    const REDIRECT_URI = encodeURIComponent(`${REDIRECT_BASE}/auth/naver/callback`);
    const STATE = Math.random().toString(36).substring(2, 15);
    
    const naverAuthUrl = `${process.env.NEXT_PUBLIC_NAVER_AUTH_URL}/oauth2.0/authorize?response_type=code&client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&state=${STATE}`;
    window.location.href = naverAuthUrl;
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-lg">
        <h1 className="text-4xl font-black text-center mb-8 text-primary tracking-tighter">shop1</h1>
        
        <form onSubmit={handleLocalLogin} className="space-y-4">
          <input
            type="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
            required
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
            required
          />
          <button
            type="submit"
            className="w-full bg-primary hover:bg-primary/90 text-white py-3 rounded-xl font-bold text-lg transition shadow-md"
          >
            로그인
          </button>
        </form>

        <div className="my-8 border-t border-gray-200 relative">
          <span className="absolute left-1/2 -translate-x-1/2 -top-3 bg-white px-4 text-sm text-gray-400 font-medium">또는</span>
        </div>

        <div className="flex flex-col gap-4">
          <Link
            href="/register/local"
            className="w-full h-[55px] rounded-xl flex items-center justify-center text-center bg-gray-100 text-gray-700 font-bold hover:bg-gray-200 transition"
          >
            이메일로 회원가입
          </Link>

          <button onClick={handleKakaoLogin} className="w-full border-none cursor-pointer hover:opacity-90 transition">
            <Image src="/kakao_login1.png" alt="카카오 로그인" width={400} height={60} className="w-full h-auto rounded-xl" />
          </button>

          <button onClick={handleNaverLogin} className="w-full border-none cursor-pointer hover:opacity-90 transition">
            <Image src="/naver_login1.png" alt="네이버 로그인" width={400} height={60} className="w-full h-auto rounded-xl" />
          </button>
        </div>
      </div>
    </div>
  );
}