"use client";

import { fetchApi } from "@/lib/api";
import { useEffect, useState } from "react";
import Link from "next/link";

interface OrderItemDTO {
  itemId: number;
  itemName: string;
  quantity: number;
  price: number;
  finalPrice: number;
}

interface OrderDetailDTO {
  orderId: number;
  status: string;
  orderDate: string;
  items: OrderItemDTO[];
  totalPrice: number;
}

export default function OrdersPage() {
  const [orders, setOrders] = useState<OrderDetailDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        setLoading(true);
        const response = await fetchApi<{ data: OrderDetailDTO[] }>("/orders/list", { 
          method: "GET",
          credentials: "include" 
        });
        
        if (response && response.data) {
          setOrders(response.data);
        }
      } catch (err) {
        console.error("주문 내역 로드 실패:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, []);

  const getStatusStyle = (status: string) => {
    switch (status) {
      case "PAID":
      case "결제완료":
        return "bg-blue-50 text-blue-600 border-blue-100";
      case "CANCELLED":
      case "취소":
        return "bg-red-50 text-red-500 border-red-100";
      case "PAYMENT_PENDING":
      case "결제대기":
        return "bg-amber-50 text-amber-600 border-amber-100";
      default:
        return "bg-gray-50 text-gray-600 border-gray-100";
    }
  };

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center bg-white">
      <div className="flex flex-col items-center gap-3">
        <div className="w-10 h-10 border-4 border-gray-100 border-t-blue-500 rounded-full animate-spin"></div>
        <p className="text-gray-400 font-medium">주문 내역을 불러오고 있습니다...</p>
      </div>
    </div>
  );

  if (!orders || orders.length === 0) return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[#fcfcfc] p-6">
      <div className="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center text-3xl mb-4">📦</div>
      <p className="text-gray-800 font-bold text-lg">주문 내역이 없습니다.</p>
      <p className="text-gray-400 text-sm mb-8">새로운 상품을 담아보세요!</p>
      <Link href="/" className="px-10 py-3.5 bg-black text-white rounded-full font-bold shadow-lg hover:bg-gray-800 transition active:scale-95">
        쇼핑하러 가기
      </Link>
    </div>
  );

  return (
    <div className="min-h-screen bg-[#fcfcfc] pb-24">
      <div className="max-w-2xl mx-auto pt-16 px-5">
        
        <div className="mb-10 text-center">
          <h1 className="text-3xl font-black text-gray-900 mb-2 tracking-tight">주문 내역</h1>
          <p className="text-gray-400 text-sm font-medium">최근 고객님이 주문하신 내역입니다.</p>
        </div>

        <div className="space-y-6">
          {orders.map((order) => (
            <div key={order.orderId} className="bg-white rounded-[2rem] border border-gray-100 shadow-[0_10px_40px_rgb(0,0,0,0.03)] overflow-hidden transition-all hover:shadow-md hover:-translate-y-0.5">
              
              <div className="px-8 py-5 border-b border-gray-50 flex justify-between items-center bg-gray-50/30">
                <div className="flex flex-col">
                  <span className="text-[11px] font-black text-gray-300 uppercase tracking-widest">Order Date</span>
                  <span className="text-sm font-bold text-gray-700">{new Date(order.orderDate).toLocaleDateString('ko-KR')}</span>
                </div>
                <div className="text-right">
                  <span className="text-[11px] font-black text-gray-300 uppercase tracking-widest block">No.</span>
                  <span className="text-sm font-black text-gray-900">#{order.orderId}</span>
                </div>
              </div>

              <div className="p-8">
                <div className="space-y-4">
                  {order.items?.map((item, idx) => (
                    <div key={`${order.orderId}-${idx}`} className="flex justify-between items-center group">
                      <div className="flex items-center gap-3">
                        <div className="w-1.5 h-1.5 bg-blue-500 rounded-full opacity-50 group-hover:opacity-100 transition-opacity"></div>
                        <span className="text-[15px] font-bold text-gray-800 line-clamp-1">
                          {item.itemName}
                        </span>
                        <span className="text-xs font-black text-gray-300">x{item.quantity}</span>
                      </div>
                      <span className="text-[15px] font-black text-gray-900 shrink-0">
                        {item.finalPrice.toLocaleString()}원
                      </span>
                    </div>
                  ))}
                </div>

                <div className="mt-8 pt-6 border-t border-gray-50 flex justify-between items-end">
                  <div className="space-y-1.5">
                    <span className="text-[10px] font-black text-gray-400 uppercase tracking-wider block ml-1">Status</span>
                    <span className={`px-4 py-1.5 rounded-xl text-xs font-black border ${getStatusStyle(order.status)}`}>
                      {order.status === 'PAID' ? '결제완료' : 
                       order.status === 'CANCELLED' ? '취소됨' : 
                       order.status === 'PAYMENT_PENDING' ? '결제대기' : order.status}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-[10px] font-black text-gray-400 uppercase tracking-wider block mr-1">Total Amount</span>
                    <p className="text-2xl font-black text-gray-900 tracking-tighter">
                      {order.totalPrice.toLocaleString()}<span className="text-sm font-bold ml-0.5">원</span>
                    </p>
                  </div>
                </div>

                {/* ★ [수정] 파일 경로가 orders/detail/[orderId] 이므로 href도 맞춰줍니다 */}
                <Link
                  href={`/orders/detail/${order.orderId}`}
                  className="mt-6 flex items-center justify-center w-full py-4 bg-gray-50 text-gray-800 text-sm font-black rounded-2xl border border-gray-100 hover:bg-gray-900 hover:text-white hover:border-gray-900 transition-all duration-300"
                >
                  상세 내역 확인하기
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}