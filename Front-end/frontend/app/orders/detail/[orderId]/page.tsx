"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import { fetchApi } from "@/lib/api";

interface OrderItemDTO {
  orderItemId: number;
  itemId: number;
  itemName: string;
  quantity: number;
  price: number;
  couponDiscount: number;
  finalPrice: number;
  totalPrice: number;
  isReviewWritten: boolean; 
}

interface AddressDTO {
  zipCode: string;
  roadAddress: string;
  detailAddress: string;
  recipientName: string;
  recipientPhone: string;
}

interface OrderDetailDTO {
  orderId: number;
  tid: string;
  status: string;
  orderDate: string;
  items: OrderItemDTO[];
  totalPrice: number;
  address: AddressDTO;
}

export default function OrderDetailPage() {
  const { orderId } = useParams();
  const router = useRouter();
  const [order, setOrder] = useState<OrderDetailDTO | null>(null);
  const [loading, setLoading] = useState(true);

  /**
   * 주문 상세 정보를 서버에서 새로 가져오는 함수
   * 타임스탬프(?t=...)를 붙여 브라우저 캐시를 강제로 무시합니다.
   */
  const fetchOrder = useCallback(async () => {
    if (!orderId) return;
    
    try {
      // 캐시를 타지 않도록 현재 시간을 쿼리 파라미터로 추가
      const res = await fetchApi<{ data: OrderDetailDTO }>(
        `/orders/detail/${orderId}?t=${new Date().getTime()}`, 
        { credentials: "include" }
      );

      if (res.data) {
        setOrder(res.data);
      }
    } catch (err) {
      console.error("상세 내역 로드 실패:", err);
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    // 1. 컴포넌트 마운트 시 최초 실행
    fetchOrder();

    // 2. 리뷰 작성 창(새 탭/창)에서 돌아오거나, 
    // 페이지가 다시 포커스(활성화)될 때 최신 데이터를 다시 가져옴
    window.addEventListener('focus', fetchOrder);
    
    return () => {
      window.removeEventListener('focus', fetchOrder);
    };
  }, [fetchOrder]);

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center bg-white">
      <div className="flex flex-col items-center gap-3">
        <div className="w-10 h-10 border-4 border-gray-200 border-t-black rounded-full animate-spin"></div>
        <p className="text-gray-500 font-medium">데이터를 갱신하고 있습니다...</p>
      </div>
    </div>
  );

  if (!order) return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-white p-4">
      <p className="text-gray-800 font-bold mb-6 text-lg">주문 내역을 찾을 수 없습니다.</p>
      <button onClick={() => router.push("/orders")} className="px-8 py-3 bg-black text-white rounded-full font-bold transition">
        주문 목록으로 돌아가기
      </button>
    </div>
  );

  return (
    <div className="min-h-screen bg-[#fcfcfc] pb-24">
      <div className="max-w-2xl mx-auto pt-12 px-5">
        
        {/* 주문 상태 헤더 */}
        <div className="text-center mb-10">
          <p className="text-blue-600 font-bold text-sm mb-2">
            {order.status === 'PAID' ? '결제완료' : order.status}
          </p>
          <h1 className="text-3xl font-black text-gray-900 mb-2">주문 상세 내역</h1>
          <p className="text-gray-400 text-sm">
            주문번호 {order.orderId}  |  {new Date(order.orderDate).toLocaleString('ko-KR')}
          </p>
        </div>

        <div className="space-y-5">
          {/* 1. 상품 리스트 카드 */}
          <div className="bg-white rounded-3xl p-8 border border-gray-100 shadow-[0_8px_30px_rgb(0,0,0,0.02)]">
            <h2 className="text-lg font-bold text-gray-900 mb-6 flex items-center gap-2">
              주문 상품 <span className="text-blue-600">{order.items.length}</span>
            </h2>
            
            <div className="divide-y divide-gray-50">
              {order.items.map((item) => (
                <div key={item.orderItemId} className="py-6 first:pt-0 last:pb-0">
                  <div className="flex justify-between items-start gap-4 mb-4">
                    <div className="flex-1">
                      <p 
                        className="font-bold text-gray-900 text-lg hover:text-blue-600 cursor-pointer transition-colors leading-tight"
                        onClick={() => router.push(`/items/${item.itemId}`)}
                      >
                        {item.itemName}
                      </p>
                      <p className="text-sm text-gray-400 mt-1">
                        수량 {item.quantity}개  ·  {item.price.toLocaleString()}원
                      </p>
                    </div>
                    <p className="font-black text-lg text-gray-900 whitespace-nowrap">
                      {item.totalPrice.toLocaleString()}원
                    </p>
                  </div>

                  {/* 결제 완료 상태일 때 버튼 활성화 */}
                  {(order.status === "PAID" || order.status === "결제 완료") && (
                    <button 
                      onClick={() => {
                        if (!item.isReviewWritten) {
                          router.push(`/review?orderItemId=${item.orderItemId}&itemName=${encodeURIComponent(item.itemName)}`);
                        }
                      }}
                      disabled={item.isReviewWritten}
                      className={`w-full py-3 text-sm font-bold rounded-xl transition active:scale-[0.98] border ${
                        item.isReviewWritten 
                          ? "bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed" // 리뷰 작성 후 상태
                          : "bg-gray-50 text-gray-700 border-gray-100 hover:bg-gray-100"   // 리뷰 작성 전 상태
                      }`}
                    >
                      {item.isReviewWritten ? "리뷰 작성 완료" : "상품 리뷰 작성하기"}
                    </button>
                  )}
                </div>
              ))}
            </div>

            <div className="mt-8 pt-8 border-t-2 border-dashed border-gray-100 flex justify-between items-center">
              <span className="font-bold text-gray-500">총 결제 금액</span>
              <span className="text-3xl font-black text-gray-900">
                {order.totalPrice.toLocaleString()}<span className="text-xl ml-1">원</span>
              </span>
            </div>
          </div>

          {/* 2. 배송 정보 카드 */}
          <div className="bg-white rounded-3xl p-8 border border-gray-100 shadow-[0_8px_30px_rgb(0,0,0,0.02)]">
            <h2 className="text-lg font-bold text-gray-900 mb-6">배송지 정보</h2>
            <div className="space-y-5 text-[15px]">
              <div className="flex gap-4">
                <span className="text-gray-400 w-16 shrink-0">받는사람</span>
                <span className="text-gray-800 font-bold">{order.address.recipientName} ({order.address.recipientPhone})</span>
              </div>
              <div className="flex gap-4">
                <span className="text-gray-400 w-16 shrink-0">주소</span>
                <span className="text-gray-800 leading-relaxed font-medium">
                  {order.address.roadAddress}<br/>
                  <span className="text-gray-500">{order.address.detailAddress}</span>
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 mt-10">
          <button onClick={() => router.push("/")} className="py-4 bg-white text-gray-800 font-bold rounded-2xl border border-gray-200 hover:bg-gray-50 transition active:scale-[0.98]">
            쇼핑 계속하기
          </button>
          <button onClick={() => router.push("/orders")} className="py-4 bg-black text-white font-bold rounded-2xl hover:bg-gray-800 transition shadow-lg active:scale-[0.98]">
            주문 내역 목록
          </button>
        </div>
      </div>
    </div>
  );
}