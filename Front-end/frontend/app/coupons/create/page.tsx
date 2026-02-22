"use client";

import { fetchApi } from "@/lib/api";
import { useState } from "react";
import { useRouter } from "next/navigation";

interface CouponCreateRequest {
  name: string;
  discountType: "FIXED" | "RATE";
  discountValue: number;
  couponType: "ALL" | "CATEGORY" | "TARGET";
  category?: string;
  itemId?: number;
  expiredAt: string;
  totalQuantity: number; // 백엔드 Coupon 엔티티의 totalQuantity 필드와 대응
}

export default function CouponCreatePage() {
  const router = useRouter();
  const [form, setForm] = useState<CouponCreateRequest>({
    name: "",
    discountType: "FIXED",
    discountValue: 0,
    couponType: "ALL",
    expiredAt: "",
    totalQuantity: 100, // 기본값 설정
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    // 숫자형 필드는 변환하여 저장
    const updatedValue = (name === "discountValue" || name === "itemId" || name === "totalQuantity") 
      ? Number(value) 
      : value;
    
    setForm({ ...form, [name]: updatedValue });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await fetchApi("/coupons", {
        method: "POST",
        body: JSON.stringify(form),
      });

      alert("쿠폰 생성 완료!");
      router.push("/coupons/received");
    } catch (err: any) {
      alert(err.message);
    }
  };

  return (
    <>
      <div className="max-w-xl mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">쿠폰 생성</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <label className="text-sm font-medium">쿠폰 이름</label>
          <input
            type="text"
            name="name"
            placeholder="쿠폰 이름"
            value={form.name}
            onChange={handleChange}
            required
            className="border p-2 rounded"
          />

          <label className="text-sm font-medium">할인 방식</label>
          <select
            name="discountType"
            value={form.discountType}
            onChange={handleChange}
            className="border p-2 rounded"
          >
            <option value="FIXED">금액 할인</option>
            <option value="RATE">퍼센트 할인</option>
          </select>

          <label className="text-sm font-medium">할인 값 ({form.discountType === "FIXED" ? "원" : "%"})</label>
          <input
            type="number"
            name="discountValue"
            placeholder="할인 값"
            value={form.discountValue}
            onChange={handleChange}
            required
            className="border p-2 rounded"
          />

          <label className="text-sm font-medium">발급 수량 (선착순 제한)</label>
          <input
            type="number"
            name="totalQuantity"
            placeholder="총 발행 수량"
            value={form.totalQuantity}
            onChange={handleChange}
            required
            min="1"
            className="border p-2 rounded"
          />

          <label className="text-sm font-medium">쿠폰 종류</label>
          <select
            name="couponType"
            value={form.couponType}
            onChange={handleChange}
            className="border p-2 rounded"
          >
            <option value="ALL">전체 적용</option>
            <option value="CATEGORY">카테고리 적용</option>
            <option value="TARGET">특정 상품 적용</option>
          </select>

          {form.couponType === "CATEGORY" && (
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium">카테고리명</label>
              <input
                type="text"
                name="category"
                placeholder="카테고리"
                value={form.category || ""}
                onChange={handleChange}
                required
                className="border p-2 rounded"
              />
            </div>
          )}

          {form.couponType === "TARGET" && (
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium">상품 ID</label>
              <input
                type="number"
                name="itemId"
                placeholder="상품 ID"
                value={form.itemId || ""}
                onChange={handleChange}
                required
                className="border p-2 rounded"
              />
            </div>
          )}

          <label className="text-sm font-medium">만료 일시</label>
          <input
            type="datetime-local"
            name="expiredAt"
            value={form.expiredAt}
            onChange={handleChange}
            required
            className="border p-2 rounded"
          />

          <button type="submit" className="bg-blue-500 text-white p-2 mt-2 rounded font-bold hover:bg-blue-600">
            쿠폰 생성하기
          </button>
        </form>
      </div>
    </>
  );
}