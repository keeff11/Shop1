"use client";

import { fetchApi } from "@/lib/api";
import Link from "next/link";
import Image from "next/image";
import { useState } from "react";

/**
 *
 * 로그인 및 회원가입 페이지 컴포넌트
 *
 */
export default function RegisterPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  /**
   *
   * 로컬 로그인 처리 핸들러
   *
   */
  const handleLocalLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await fetchApi("/auth/local/login", {
        method: "POST",
        credentials: "include", 
        body: JSON.stringify({ email, password }),
      });

      // 쿠키 갱신을 위해 브라우저 수준에서 이동
      window.location.href = "/home";

    } catch (err) {
      console.error(err);
      alert("로그인 중 오류가 발생했습니다.");
    }
  };

  /**
   *
   * 카카오 소셜 로그인 핸들러
   * 통합된 백엔드 콜백 주소(/auth/kakao/callback)를 REDIRECT_URI로 사용
   *
   */
  const handleKakaoLogin = () => {
    const REST_API_KEY = process.env.NEXT_PUBLIC_KAKAO_REST_KEY!;
    // 백엔드 리팩토링 주소와 일치 확인
    const REDIRECT_URI = encodeURIComponent(`${process.env.NEXT_PUBLIC_API_BASE_URL}/auth/kakao/callback`);
    
    const kakaoAuthUrl = `${process.env.NEXT_PUBLIC_KAKAO_AUTH_URL}/oauth/authorize?response_type=code&client_id=${REST_API_KEY}&redirect_uri=${REDIRECT_URI}`;
    window.location.href = kakaoAuthUrl;
  };

  /**
   *
   * 네이버 소셜 로그인 핸들러
   * 통합된 백엔드 콜백 주소(/auth/naver/callback)를 REDIRECT_URI로 사용
   *
   */
  const handleNaverLogin = () => {
    const CLIENT_ID = process.env.NEXT_PUBLIC_NAVER_CLIENT_ID!;
    // 백엔드 리팩토링 주소와 일치 확인
    const REDIRECT_URI = encodeURIComponent(`${process.env.NEXT_PUBLIC_API_BASE_URL}/auth/naver/callback`);
    const STATE = Math.random().toString(36).substring(2, 15);
    
    const naverAuthUrl = `${process.env.NEXT_PUBLIC_NAVER_AUTH_URL}/oauth2.0/authorize?response_type=code&client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&state=${STATE}`;
    window.location.href = naverAuthUrl;
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-lg">
        <h1 className="text-3xl font-bold text-center mb-6 text-primary">shop1</h1>
        <h2 className="text-2xl font-semibold text-center mb-6">로그인 / 회원가입</h2>

        <form onSubmit={handleLocalLogin} className="space-y-4">
          <input
            type="email"
            placeholder="이메일"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            required
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            required
          />
          <button
            type="submit"
            className="w-full bg-primary hover:bg-primary/90 text-white py-2 rounded-lg font-semibold transition shadow-sm"
          >
            로그인
          </button>
        </form>

        <div className="my-6 border-t border-gray-200"></div>

        <div className="flex flex-col gap-3">
          <Link
            href="/register/local"
            className="w-full h-[50px] rounded-lg flex items-center justify-center text-center bg-gray-100 text-gray-700 font-semibold hover:bg-gray-200 transition"
          >
            이메일로 회원가입
          </Link>

          <button onClick={handleKakaoLogin} className="w-full flex justify-center border-none cursor-pointer hover:opacity-90 transition">
            <Image src="/kakao_login1.png" alt="카카오 로그인" width={300} height={50} className="w-full h-auto rounded-lg object-contain" />
          </button>

          <button onClick={handleNaverLogin} className="w-full flex justify-center border-none cursor-pointer hover:opacity-90 transition">
            <Image src="/naver_login1.png" alt="네이버 로그인" width={300} height={50} className="w-full h-auto rounded-lg object-contain" />
          </button>
        </div>
      </div>
    </div>
  );
}