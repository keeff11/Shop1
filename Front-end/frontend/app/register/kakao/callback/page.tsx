"use client";

/**
 * * 카카오 소셜 로그인 콜백 처리를 위한 컴포넌트
 * useSearchParams 사용으로 인한 빌드 에러 방지를 위해 Suspense를 적용함
 * */

import { fetchApi } from "@/lib/api";
import { useEffect, Suspense } from "react"; // Suspense 추가
import { useSearchParams, useRouter } from "next/navigation";

interface KakaoCallbackResponse {
  success: boolean;
  isRegistered: boolean;
  signUpToken?: string;
}

// 1. 실제 로직이 담긴 컴포넌트
function OAuthCallbackContent() {
  const params = useSearchParams();
  const router = useRouter();
  const code = params.get("code");

  useEffect(() => {
    if (!code) return;

    const fetchLogin = async () => {
      try {
        const data: KakaoCallbackResponse = await fetchApi(
          "/auth/kakao/callback",
          {
            method: "POST",
            credentials: "include",
            body: JSON.stringify({ code }),
          }
        );

        if (!data.success) {
          alert("로그인 처리 실패");
          return;
        }

        if (data.isRegistered) {
          router.push("/home");
        } else if (data.signUpToken) {
          router.push(
            `/register/social/additional-info?provider=kakao&kakaoToken=${data.signUpToken}`
          );
        } else {
          alert("회원가입 토큰이 없습니다.");
        }
      } catch (err) {
        console.error(err);
        alert("로그인 처리 중 오류 발생");
      }
    };

    fetchLogin();
  }, [code, router]); // router 의존성 추가

  return <p className="text-center mt-20">로그인 처리 중...</p>;
}

// 2. 외부에서 호출하는 기본 페이지 (Suspense로 감싸기)
export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={<p className="text-center mt-20">페이지 로딩 중...</p>}>
      <OAuthCallbackContent />
    </Suspense>
  );
}