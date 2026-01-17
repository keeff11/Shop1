"use client";

import { fetchApi } from "@/lib/api";
import { useEffect, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";

interface KakaoCallbackResponse {
  success: boolean;
  isRegistered: boolean;
  signUpToken?: string;
}


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
  }, [code, router]);

  return <p className="text-center mt-20">로그인 처리 중...</p>;
}

export default function OAuthCallbackPage() {
  return (
    <Suspense fallback={<p className="text-center mt-20">페이지 로딩 중...</p>}>
      <OAuthCallbackContent />
    </Suspense>
  );
}