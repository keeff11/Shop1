"use client";

import { fetchApi } from "@/lib/api";
import { useEffect, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";

interface OrderItem {
  itemId: number;
  itemName: string;
  quantity: number;
  price: number;          // 원래 가격
  couponDiscount?: number; // 쿠폰 할인
  finalPrice: number;     // 최종 결제금액
}

interface Address {
  zipCode: string;
  roadAddress: string;
  detailAddress: string;
  recipientName: string;
  recipientPhone: string;
}

interface OrderDetail {
  orderId: number;
  tid: string;
  status: string;
  orderDate: string;
  items: OrderItem[];
  totalPrice: number;
  address: Address;
}

interface OrderApiResponse {
  data: OrderDetail;
}

export default function OrderDetailPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const router = useRouter();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [paymentSuccess, setPaymentSuccess] = useState(false);

  const orderId = params.orderId;
  const approved = searchParams.get("approved");

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const data = await fetchApi<OrderApiResponse>(`/orders/${orderId}`, { cache: "no-store" });
        setOrder(data.data);

        if (approved === "true") setPaymentSuccess(true);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchOrder();
  }, [orderId, approved]);

  if (loading)
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-400 font-medium">
        주문 정보를 불러오는 중...
      </div>
    );
  if (!order)
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-400 font-medium">
        주문 정보를 찾을 수 없습니다.
      </div>
    );

  const statusColor = (status: string) => {
    switch (status) {
      case "결제완료":
        return "text-success bg-success/10";
      case "취소":
        return "text-destructive bg-destructive/10";
      default:
        return "text-accent-foreground bg-accent/10";
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">

      <div className="max-w-4xl mx-auto p-6 space-y-6">
        {paymentSuccess && (
          <div className="p-4 bg-success/10 text-success rounded-lg text-center font-semibold shadow-md">
            ✅ 결제가 성공적으로 완료되었습니다!
          </div>
        )}

        <h1 className="text-3xl font-bold text-center text-gray-800 mb-6">
          주문 상세
        </h1>

        {/* 주문 정보 */}
        <div className="bg-white rounded-lg shadow p-6 grid grid-cols-1 md:grid-cols-3 gap-4 text-gray-700">
          <div className="flex flex-col">
            <span className="text-sm text-gray-500">주문번호</span>
            <span className="font-semibold text-gray-800">{order.orderId}</span>
          </div>
          <div className="flex flex-col">
            <span className="text-sm text-gray-500">주문일시</span>
            <span className="font-semibold text-gray-800">
              {new Date(order.orderDate).toLocaleString()}
            </span>
          </div>
          <div className="flex flex-col">
            <span className="text-sm text-gray-500">주문 상태</span>
            <span
              className={`mt-1 font-semibold px-3 py-1 rounded-full text-center text-sm ${statusColor(
                order.status
              )}`}
            >
              {order.status}
            </span>
          </div>
        </div>

        {/* 배송지 정보 */}
        <div className="bg-white rounded-lg shadow p-6 space-y-2">
          <h2 className="text-xl font-semibold border-b pb-2 mb-3">
            배송지 정보
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-gray-700">
            <div>
              <span className="text-sm text-gray-500">수령인</span>
              <div className="font-semibold">{order.address.recipientName}</div>
            </div>
            <div>
              <span className="text-sm text-gray-500">전화번호</span>
              <div className="font-semibold">{order.address.recipientPhone}</div>
            </div>
            <div className="md:col-span-2">
              <span className="text-sm text-gray-500">주소</span>
              <div className="font-semibold">
                [{order.address.zipCode}] {order.address.roadAddress} {order.address.detailAddress}
              </div>
            </div>
          </div>
        </div>

        {/* 주문 상품 */}
        <div className="bg-white rounded-lg shadow p-6 space-y-4">
          <h2 className="text-xl font-semibold border-b pb-2 mb-4">주문 상품</h2>

          {order.items.map((item) => (
            <div
              key={item.itemId}
              className="flex justify-between items-center p-3 border rounded-lg hover:shadow-md transition cursor-pointer bg-gray-50"
              onClick={() => router.push(`/items/${item.itemId}`)}
            >
              <div className="flex flex-col md:flex-row md:items-center md:gap-4">
                <span className="font-medium text-blue-600 hover:underline">
                  {item.itemName}
                </span>
                <span className="text-gray-500 text-sm mt-1 md:mt-0">
                  {item.quantity} × {item.price.toLocaleString()}원
                </span>

                {/* 쿠폰 할인 표시 */}
                {item.couponDiscount && item.couponDiscount > 0 && (
                  <span className="text-red-500 text-sm ml-2">
                    (-{item.couponDiscount.toLocaleString()}원 쿠폰 적용)
                  </span>
                )}
              </div>

              <span className="font-semibold text-gray-800">
                {item.finalPrice.toLocaleString()}원
              </span>
            </div>
          ))}

          <div className="flex justify-between mt-4 font-bold text-lg border-t pt-3 text-gray-800">
            <span>총 결제금액</span>
            <span>{order.totalPrice.toLocaleString()}원</span>
          </div>
        </div>

        <button
          className="w-full py-4 bg-primary text-primary-foreground font-semibold rounded-lg hover:bg-primary/90 transition shadow-md"
          onClick={() => router.push("/")}
        >
          홈으로 돌아가기
        </button>
      </div>
    </div>
  );
}
