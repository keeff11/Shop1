"use client";

import { fetchApi } from "@/lib/api";
import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

interface CartItem {
  itemId: number;
  itemName: string;
  price: number;
  quantity: number;
  imageUrl?: string;
}

interface ApiResponse {
  data: CartItem[];
}

export default function CartPage() {
  const router = useRouter();
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [selectedItems, setSelectedItems] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchCartItems = async () => {
    setLoading(true);
    try {
      const data = await fetchApi<ApiResponse>("/cart/list", { credentials: "include" });
      setCartItems(data.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCartItems();
  }, []);

  const toggleSelectItem = (itemId: number) => {
    setSelectedItems((prev) =>
      prev.includes(itemId) ? prev.filter((id) => id !== itemId) : [...prev, itemId]
    );
  };

  /** 결제 페이지로 이동: 상품 정보를 세션에 포함 */
  const goToPaymentPage = () => {
    if (selectedItems.length === 0) {
      alert("결제할 상품을 선택해주세요.");
      return;
    }

    const itemsToCheckout = cartItems
      .filter((item) => selectedItems.includes(item.itemId))
      .map((item) => ({
        itemId: item.itemId,
        itemName: item.itemName, // 결제 페이지 표시용
        price: item.price,       // 결제 페이지 표시용
        quantity: item.quantity,
        imageUrl: item.imageUrl,
      }));

    sessionStorage.setItem(
      "checkoutData",
      JSON.stringify({
        itemOrders: itemsToCheckout,
      })
    );

    router.push("/payments");
  };

  const totalPrice = cartItems
    .filter((item) => selectedItems.includes(item.itemId))
    .reduce((acc, item) => acc + item.price * item.quantity, 0);

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-2xl font-bold mb-6">🛒 장바구니</h1>
        {loading ? (
          <div className="text-center py-20">로딩중...</div>
        ) : (
          <>
            <div className="space-y-4">
              {cartItems.map((item) => (
                <div key={item.itemId} className="flex items-center gap-4 p-4 bg-white rounded-xl shadow-sm border border-gray-100 transition hover:shadow-md">
                  <input
                    type="checkbox"
                    checked={selectedItems.includes(item.itemId)}
                    onChange={() => toggleSelectItem(item.itemId)}
                    className="w-5 h-5 accent-gray-800"
                  />
                  {item.imageUrl && (
                    <img src={item.imageUrl} alt={item.itemName} className="w-20 h-20 object-cover rounded-lg" />
                  )}
                  <div className="flex-1">
                    <div className="font-bold text-lg">{item.itemName}</div>
                    <div className="text-gray-500">{item.price.toLocaleString()}원 × {item.quantity}개</div>
                  </div>
                  <div className="font-black text-lg">{(item.price * item.quantity).toLocaleString()}원</div>
                </div>
              ))}
            </div>
            <div className="mt-8 p-8 bg-white rounded-2xl shadow-sm border border-gray-100 flex justify-between items-center">
              <div>
                <p className="text-gray-500 text-sm">최종 결제 금액</p>
                <p className="text-3xl font-black text-gray-900">{totalPrice.toLocaleString()}원</p>
              </div>
              <button
                onClick={goToPaymentPage}
                className="px-12 py-4 bg-gray-900 text-white rounded-xl text-lg font-bold hover:bg-gray-800 transition shadow-lg shadow-gray-200"
              >
                결제하기
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}