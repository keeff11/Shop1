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
    const paymentKey = searchParams.get("paymentKey");   // 토스
    const amount = searchParams.get("amount");
    const paymentId = searchParams.get("paymentId");     // 네이버
    const resultCode = searchParams.get("resultCode");   // 네이버 (Success | Fail | UserCancel ...)

    // 네이버페이는 결제 실패/취소 시에도 returnUrl로 돌아온다. 승인 요청 없이 장바구니로 돌려보낸다.
    // (주문은 PAYMENT_PENDING으로 남고, 정합성 배치가 PG 조회 후 취소·재고 복구한다)
    if (resultCode && resultCode !== "Success") {
      processedRef.current = true;
      toast.error(searchParams.get("resultMessage") || "결제가 취소되었거나 실패했습니다.");
      router.replace("/cart");
      return;
    }

    // 파라미터가 유효하지 않은 초기 렌더링 시에는 Lock을 걸지 않고 종료
    const pgCredential = pgToken || paymentKey || paymentId;
    if (!orderId || !pgCredential) {
      return;
    }

    // 2. API 호출 조건이 충족된 즉시 Lock 활성화
    processedRef.current = true;

    const approvePayment = async () => {
      try {
        const params = new URLSearchParams({ orderId });
        if (pgToken) params.set("pg_token", pgToken);
        if (paymentKey) {
          params.set("paymentKey", paymentKey);
          if (amount) params.set("amount", amount);
        }
        if (paymentId) params.set("paymentId", paymentId);

        // 백엔드 @Idempotent는 Idempotency-Key 헤더가 필수다.
        // 같은 결제 콜백(새로고침, 재시도)이면 항상 같은 키가 나오도록 PG가 준 값으로 결정적으로 만든다.
        const idempotencyKey = `approve:${orderId}:${pgCredential}`;

        const res = await fetchApi<{ data?: { orderId?: number } }>(
          `/orders/payment/approve?${params.toString()}`,
          { credentials: "include", headers: { "Idempotency-Key": idempotencyKey } }
        );

        if (typeof window !== 'undefined') {
            window.dispatchEvent(new Event("cart-updated"));
            sessionStorage.removeItem("checkoutData");
        }
        
        setStatus("success");
        toast.success("결제가 정상적으로 완료되었습니다! 🎉");
        // 토스는 orderId가 "ORDER_{id}_{timestamp}" 형태라 그대로 쓰면 상세 조회가 실패한다. 서버가 돌려준 실제 ID를 쓴다.
        router.replace(`/orders/detail/${res.data?.orderId ?? orderId}`);
        
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