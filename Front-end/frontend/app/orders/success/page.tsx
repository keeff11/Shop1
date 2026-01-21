"use client";

import { useEffect, useState, useRef, Suspense } from "react"; // ★ Suspense 추가
import { useRouter, useSearchParams } from "next/navigation";
import { fetchApi } from "@/lib/api";
import { toast } from "react-hot-toast"; // 앞서 설정한 toast 적용

// 실제 결제 승인 로직을 수행하는 내부 컴포넌트
function OrderSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const processedRef = useRef(false);

  useEffect(() => {
    if (processedRef.current) return;

    const orderId = searchParams.get("orderId");
    const pgToken = searchParams.get("pg_token");

    if (!orderId || !pgToken) {
      toast.error("유효하지 않은 결제 정보입니다.");
      router.replace("/cart");
      return;
    }

    const approvePayment = async () => {
      processedRef.current = true;
      
      try {
        // 1. 백엔드 승인 API 호출
        await fetchApi(
          `/orders/payment/approve?orderId=${orderId}&pg_token=${pgToken}`,
          { credentials: "include" }
        );

        // 2. 헤더 장바구니 갱신 신호
        window.dispatchEvent(new Event("cart-updated"));

        setStatus("success");
        sessionStorage.removeItem("checkoutData");
        
        toast.success("결제가 정상적으로 완료되었습니다! 🎉");
        router.replace(`/orders/detail/${orderId}`);
        
      } catch (error) {
        console.error("결제 승인 실패:", error);
        setStatus("error");
        toast.error("결제 승인 처리에 실패했습니다.");
        router.replace("/cart");
      }
    };

    approvePayment();
  }, [searchParams, router]);

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

// 메인 페이지 컴포넌트: 빌드 오류 방지를 위해 Suspense로 래핑
export default function OrderSuccessPage() {
  return (
    <Suspense 
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900"></div>
        </div>
      }
    >
      <OrderSuccessContent />
    </Suspense>
  );
}