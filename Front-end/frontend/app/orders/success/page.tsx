"use client";

import { useEffect, useState, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { fetchApi } from "@/lib/api";
import { toast } from "sonner"; // toast 라이브러리 확인 필요

function OrderSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const processedRef = useRef(false);

  useEffect(() => {
    if (processedRef.current) return;

    // URL 파라미터 읽기
    const orderId = searchParams.get("orderId");
    
    // 카카오/네이버
    const pgToken = searchParams.get("pg_token");
    
    // 토스페이먼츠
    const paymentKey = searchParams.get("paymentKey");
    const amount = searchParams.get("amount");

    // [수정] 유효성 검사: pgToken이나 paymentKey 중 하나라도 있으면 OK
    if (!orderId || (!pgToken && !paymentKey)) {
      // 파라미터가 아예 없는 경우에만 에러 처리
      // toast.error("유효하지 않은 결제 정보입니다.");
      // router.replace("/cart");
      return;
    }

    const approvePayment = async () => {
      processedRef.current = true;
      
      try {
        // [수정] API 호출 URL 구성
        let apiUrl = `/orders/payment/approve?orderId=${orderId}`;
        
        if (pgToken) {
            apiUrl += `&pg_token=${pgToken}`;
        }
        if (paymentKey) {
            apiUrl += `&paymentKey=${paymentKey}&amount=${amount}`;
        }

        // 1. 백엔드 승인 API 호출
        await fetchApi(apiUrl, { credentials: "include" });

        // 2. 장바구니 갱신 등 후처리
        if (typeof window !== 'undefined') {
            window.dispatchEvent(new Event("cart-updated"));
            sessionStorage.removeItem("checkoutData");
        }
        
        setStatus("success");
        toast.success("결제가 정상적으로 완료되었습니다! 🎉");
        router.replace(`/orders/detail/${orderId}`);
        
      } catch (error: any) {
        console.error("결제 승인 실패:", error);
        setStatus("error");
        toast.error(error.message || "결제 승인 처리에 실패했습니다.");
        router.replace("/cart");
      }
    };

    approvePayment();
  }, [searchParams, router]);

  // ... (UI 부분 동일) ...
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50">
      <div className="bg-white p-8 rounded-2xl shadow-lg border text-center max-w-sm w-full">
        {status === "loading" && (
          <>
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 mx-auto mb-4"></div>
            <h2 className="text-xl font-bold mb-2">결제 승인 중...</h2>
            <p className="text-gray-500 text-sm">잠시만 기다려주세요.</p>
          </>
        )}
        {status === "success" && <h2 className="text-xl font-bold text-blue-600">결제 완료!</h2>}
        {status === "error" && <h2 className="text-xl font-bold text-red-600">결제 실패</h2>}
      </div>
    </div>
  );
}

export default function OrderSuccessPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <OrderSuccessContent />
    </Suspense>
  );
}