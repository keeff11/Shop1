"use client";

/**
 * * 네이버 소셜 로그인 콜백 처리를 위한 컴포넌트
 * 빌드 에러(prerender error) 방지를 위해 useSearchParams 로직을 Suspense로 감싸 처리함
 * */

import { fetchApi } from "@/lib/api";
import { useEffect, Suspense } from "react"; // Suspense 추가
import { useSearchParams, useRouter } from "next/navigation";

interface NaverCallbackResponse {
  success: boolean;
  isRegistered: boolean;
  signUpToken?: string;
}

// 1. 실제 로직이 담긴 컴포넌트
function NaverCallbackContent() {
  const params = useSearchParams();
  const router = useRouter();

  const code = params.get("code");
  const state = params.get("state"); // 네이버는 state 필수

  useEffect(() => {
    if (!code || !state) return;

    const fetchLogin = async () => {
      try {
        const data: NaverCallbackResponse = await fetchApi(
          "/auth/naver/callback",
          {
            method: "POST",
            credentials: "include",
            body: JSON.stringify({ code, state }),
          }
        );

        if (!data.success) {
          alert("네이버 로그인 처리 실패");
          return;
        }

        if (data.isRegistered) {
          router.push("/home");
        } else if (data.signUpToken) {
          router.push(
            `/register/social/additional-info?provider=naver&naverToken=${data.signUpToken}`
          );
        } else {
          alert("회원가입 토큰이 없습니다.");
        }
      } catch (error) {
        console.error(error);
        alert("네이버 로그인 처리 중 오류 발생");
      }
    };

    fetchLogin();
  }, [code, state, router]);

  return <p className="text-center mt-20">네이버 로그인 처리 중...</p>;
}

// 2. 외부에서 호출하는 기본 페이지 (Suspense 적용)
export default function NaverOAuthCallbackPage() {
  return (
    <Suspense fallback={<p className="text-center mt-20">네이버 연동 확인 중...</p>}>
      <NaverCallbackContent />
    </Suspense>
  );
}