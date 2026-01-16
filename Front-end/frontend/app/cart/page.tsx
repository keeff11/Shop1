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

  /** 장바구니 조회 */
  const fetchCartItems = async () => {
    setLoading(true);
    try {
      const data = await fetchApi<ApiResponse>("/cart/list", {
        credentials: "include",
      });
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

  /** 체크박스 토글 */
  const toggleSelectItem = (itemId: number) => {
    setSelectedItems((prev) =>
      prev.includes(itemId)
        ? prev.filter((id) => id !== itemId)
        : [...prev, itemId]
    );
  };

  /** 결제 페이지로 이동 */
  const goToPaymentPage = () => {
    if (selectedItems.length === 0) {
      alert("결제할 상품을 선택해주세요.");
      return;
    }

    // 선택된 아이템만 가져오기
    const itemsToCheckout = cartItems
      .filter((item) => selectedItems.includes(item.itemId))
      .map((item) => ({
        itemId: item.itemId,
        quantity: item.quantity,
      }));

    // sessionStorage에 통합 API 기준 DTO 저장
    sessionStorage.setItem(
      "checkoutData",
      JSON.stringify({
        type: "CART",
        itemOrders: itemsToCheckout,
        addressId: null,
        zipCode: "",
        roadAddress: "",
        detailAddress: "",
        recipientName: "",
        recipientPhone: "",
      })
    );

    router.push("/payments");
  };


  /** 총 결제 금액 */
  const totalPrice = cartItems
    .filter((item) => selectedItems.includes(item.itemId))
    .reduce((acc, item) => acc + item.price * item.quantity, 0);

  return (
    <div className="min-h-screen bg-gray-50">

      <div className="max-w-4xl mx-auto p-6">
        <h1 className="text-2xl font-bold mb-6">🛒 장바구니</h1>

        {loading ? (
          <div className="text-center py-20">로딩중...</div>
        ) : (
          <>
            {/* 장바구니 리스트 */}
            <div className="space-y-4">
              {cartItems.length === 0 ? (
                <div className="text-center text-gray-500 py-10">
                  장바구니에 담긴 상품이 없습니다.
                </div>
              ) : (
                cartItems.map((item) => (
                  <div
                    key={item.itemId}
                    className="flex items-center gap-4 p-4 bg-white rounded-lg shadow hover:shadow-lg transition cursor-pointer"
                    onClick={() => router.push(`/items/${item.itemId}`)}
                  >
                    <input
                      type="checkbox"
                      checked={selectedItems.includes(item.itemId)}
                      onClick={(e) => e.stopPropagation()} // 부모 클릭 방지
                      onChange={() => toggleSelectItem(item.itemId)}
                      className="w-4 h-4"
                    />

                    {item.imageUrl && (
                      <img
                        src={item.imageUrl}
                        alt={item.itemName}
                        className="w-20 h-20 object-cover rounded"
                      />
                    )}

                    <div className="flex-1">
                      <div className="font-semibold">{item.itemName}</div>
                      <div className="text-sm text-gray-500">
                        {item.price.toLocaleString()}원 × {item.quantity}
                      </div>
                    </div>

                    <div className="font-bold">
                      {(item.price * item.quantity).toLocaleString()}원
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* 결제 영역 */}
            <div className="mt-8 p-6 bg-white rounded-lg shadow">
              <div className="flex justify-between text-lg font-bold">
                <span>총 결제 금액</span>
                <span>{totalPrice.toLocaleString()}원</span>
              </div>

              <button
                onClick={goToPaymentPage}
                className="mt-6 w-full py-4 bg-primary hover:bg-primary/90 text-white rounded-lg text-lg font-semibold"
              >
                결제 페이지로 이동
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
