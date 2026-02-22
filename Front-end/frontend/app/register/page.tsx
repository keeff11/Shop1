"use client";

import { fetchApi } from "@/lib/api";
import Link from "next/link";
import Image from "next/image";
import { useState } from "react";

export default function RegisterPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  
  // 에러 메시지 상태만 남김
  const [errorMessage, setErrorMessage] = useState("");

  /**
   * * 로컬 로그인 핸들러
   * */
  const handleLocalLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(""); // 로그인 재시도 시 기존 메시지 초기화

    try {
      await fetchApi("/auth/local/login", {
        method: "POST",
        credentials: "include", 
        body: JSON.stringify({ email, password }),
      });

      window.location.href = "/home";
    } catch (err: any) {
      console.error("로그인 에러:", err);
      // 에러 발생 시 상태 업데이트하여 폼 내부에 표시
      setErrorMessage(err.message);
    }
  };

  /*
  * * 카카오 로그인 핸들러
  *
  */
  const handleKakaoLogin = () => {
    const REST_API_KEY = process.env.NEXT_PUBLIC_KAKAO_REST_KEY!;
    const REDIRECT_BASE = process.env.NEXT_PUBLIC_API_BASE_URL;
    const REDIRECT_URI = encodeURIComponent(`${REDIRECT_BASE}/auth/kakao/callback`);
    
    const kakaoAuthUrl = `${process.env.NEXT_PUBLIC_KAKAO_AUTH_URL}/oauth/authorize?response_type=code&client_id=${REST_API_KEY}&redirect_uri=${REDIRECT_URI}`;
    window.location.href = kakaoAuthUrl;
  };

  /**
   * * 네이버 로그인 핸들러
   * **/
  const handleNaverLogin = () => {
    const CLIENT_ID = process.env.NEXT_PUBLIC_NAVER_CLIENT_ID!;
    const REDIRECT_BASE = process.env.NEXT_PUBLIC_API_BASE_URL;
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

          {/* ===== 폼 내부에 표시되는 에러 메시지 ===== */}
          {errorMessage && (
            <div className="flex items-center gap-1.5 text-red-500 text-sm font-semibold px-1 animate-in fade-in slide-in-from-top-1 duration-200">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
              </svg>
              <span>{errorMessage}</span>
            </div>
          )}

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