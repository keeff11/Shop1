"use client";

import { fetchApi } from "@/lib/api";
import { useEffect, useState } from "react";
import Link from "next/link";

interface OrderItem {
  itemId: number;
  itemName: string;
  quantity: number;
  price: number;
}

interface OrderDetail {
  orderId: number;
  tid: string;
  status: string;
  orderDate: string;
  items: OrderItem[];
}

interface OrdersApiResponse {
  data: OrderDetail[];
}

export default function OrdersPage() {
  const [orders, setOrders] = useState<OrderDetail[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        const data = await fetchApi<OrdersApiResponse>("/orders", { cache: "no-store" });
        setOrders(data.data); // ApiResponse 포맷: { success: true, data: [...] }
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, []);

  if (loading)
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-500">
        주문 내역을 불러오는 중...
      </div>
    );

  if (orders.length === 0)
    return (
      <div className="min-h-screen bg-gray-100">
        <div className="p-6 text-center text-gray-500">주문 내역이 없습니다.</div>
      </div>
    );

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="max-w-3xl mx-auto p-6 space-y-4">
        <h1 className="text-2xl font-bold text-center mb-4">주문 내역</h1>

        <ul className="space-y-4">
          {orders.map((order) => {
            const totalPrice = order.items.reduce(
              (acc, item) => acc + item.price * item.quantity,
              0
            );

            return (
              <li
                key={order.orderId}
                className="bg-white rounded-lg shadow p-4 space-y-2 hover:shadow-lg transition"
              >
                <div className="flex justify-between text-gray-700 font-medium">
                  <span>주문번호: {order.orderId}</span>
                  <span>주문일시: {new Date(order.orderDate).toLocaleString()}</span>
                </div>

                <div className="space-y-1">
                  {order.items.map((item) => (
                    <div key={item.itemId} className="text-gray-600">
                      {item.itemName} × {item.quantity} (
                      {item.price.toLocaleString()}원)
                    </div>
                  ))}
                </div>

                <div className="flex justify-between items-center mt-2">
                  <span className="font-semibold">상태: {order.status}</span>
                  <Link
                    href={`/orders/${order.orderId}`}
                    className="text-primary-foreground bg-primary hover:bg-primary/90 px-4 py-2 rounded-lg font-medium transition"
                  >
                    상세보기
                  </Link>
                </div>

                <div className="text-right font-bold text-gray-800">
                  총 결제금액: {totalPrice.toLocaleString()}원
                </div>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
