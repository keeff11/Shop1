"use client";

import { fetchApi } from "@/lib/api";
import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge"; // 프로젝트 내 공통 UI 컴포넌트가 있다면 활용
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

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
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchApi<ApiResponse>("/coupons/my")
      .then((data: ApiResponse) => {
        setCoupons(data.data || []);
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  if (loading) {
    return <div className="text-center p-10">쿠폰 목록을 불러오는 중입니다...</div>;
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6 text-gray-800">내 쿠폰함</h1>

      {coupons.length === 0 ? (
        <div className="text-center py-20 border-2 border-dashed rounded-lg text-gray-500">
          보유 중인 쿠폰이 없습니다.
        </div>
      ) : (
        <div className="grid gap-4">
          {coupons.map((coupon) => (
            <Card 
              key={coupon.couponId} 
              className={`relative overflow-hidden ${coupon.used ? "opacity-60 bg-gray-50" : "bg-white shadow-sm border-blue-100"}`}
            >
              {/* 사용 완료 스탬프 효과 */}
              {coupon.used && (
                <div className="absolute right-4 top-1/2 -translate-y-1/2 border-4 border-gray-400 text-gray-400 font-bold px-4 py-1 rounded-sm rotate-12 z-10">
                  USED
                </div>
              )}

              <CardHeader className="pb-2">
                <div className="flex justify-between items-start">
                  <div>
                    <Badge variant={coupon.used ? "secondary" : "default"} className="mb-2">
                      {coupon.couponType === "ALL" && "전체 할인"}
                      {coupon.couponType === "CATEGORY" && `카테고리: ${coupon.category}`}
                      {coupon.couponType === "TARGET" && `상품 전용 (ID: ${coupon.itemId})`}
                    </Badge>
                    <CardTitle className={`text-xl ${coupon.used ? "text-gray-500" : "text-blue-600"}`}>
                      {coupon.name}
                    </CardTitle>
                  </div>
                  <div className="text-right">
                    <span className="text-2xl font-black">
                      {coupon.discountType === "FIXED" 
                        ? `${coupon.discountValue.toLocaleString()}원` 
                        : `${coupon.discountValue}%`}
                    </span>
                    <p className="text-xs text-gray-400">OFF</p>
                  </div>
                </div>
              </CardHeader>

              <CardContent>
                <div className="flex justify-between items-center text-sm">
                  <span className="text-gray-500">
                    {coupon.used ? "사용한 쿠폰입니다" : "결제 단계에서 사용 가능합니다"}
                  </span>
                  {!coupon.used && (
                    <span className="text-blue-500 font-semibold">사용 가능</span>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}