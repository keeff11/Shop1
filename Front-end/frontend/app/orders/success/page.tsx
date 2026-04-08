"use client";

import { useEffect, useState, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { fetchApi } from "@/lib/api";
import { toast } from "sonner";

function OrderSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  
  // 렌더링 주기에 영향을 받지 않는 Lock 변수 선언
  const processedRef = useRef(false);

  useEffect(() => {
    // 1. 이미 결제 승인 프로세스가 진행 중이라면 조기 종료 (중복 실행 방어)
    if (processedRef.current) return;

    const orderId = searchParams.get("orderId");
    const pgToken = searchParams.get("pg_token");
    const paymentKey = searchParams.get("paymentKey");
    const amount = searchParams.get("amount");

    // 파라미터가 유효하지 않은 초기 렌더링 시에는 Lock을 걸지 않고 종료
    if (!orderId || (!pgToken && !paymentKey)) {
      return;
    }

    // 2. API 호출 조건이 충족된 즉시 Lock 활성화
    processedRef.current = true;

    const approvePayment = async () => {
      try {
        let apiUrl = `/orders/payment/approve?orderId=${orderId}`;
        
        if (pgToken) {
            apiUrl += `&pg_token=${pgToken}`;
        }
        if (paymentKey) {
            apiUrl += `&paymentKey=${paymentKey}&amount=${amount}`;
        }

        await fetchApi(apiUrl, { credentials: "include" });

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