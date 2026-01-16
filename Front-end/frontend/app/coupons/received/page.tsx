"use client";

import { fetchApi } from "@/lib/api";
import { useEffect, useState } from "react";

interface CouponResponse {
  couponId: number;
  name: string;
  discountType: "FIXED" | "RATE";
  discountValue: number;
  couponType: "ALL" | "CATEGORY" | "TARGET";
  category?: string;
  itemId?: number;
  used: boolean;
}

interface ApiResponse {
  data: CouponResponse[];
}

export default function ReceivedCouponsPage() {
  const [coupons, setCoupons] = useState<CouponResponse[]>([]);

  useEffect(() => {
    fetchApi<ApiResponse>("/coupons/my")
      .then((data: ApiResponse) => setCoupons(data.data || []))
      .catch(err => console.error(err));
  }, []);

  return (
    <>
      <div className="max-w-xl mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">내 쿠폰</h1>

        {coupons.length === 0 ? (
          <p>받은 쿠폰이 없습니다.</p>
        ) : (
          <ul className="flex flex-col gap-3">
            {coupons.map(coupon => (
              <li
                key={coupon.couponId}
                className={`border p-3 rounded ${
                  coupon.used ? "bg-gray-200" : "bg-white"
                }`}
              >
                <div className="font-bold">{coupon.name}</div>
                <div>
                  {coupon.discountType === "FIXED"
                    ? `${coupon.discountValue}원 할인`
                    : `${coupon.discountValue}% 할인`}
                </div>
                <div>타입: {coupon.couponType}</div>
                {coupon.couponType === "CATEGORY" && <div>카테고리: {coupon.category}</div>}
                {coupon.couponType === "TARGET" && <div>상품 ID: {coupon.itemId}</div>}
                <div>사용 여부: {coupon.used ? "사용함" : "사용 가능"}</div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  );
}
