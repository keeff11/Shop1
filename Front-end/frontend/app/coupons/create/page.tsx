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
}

export default function CouponCreatePage() {
  const router = useRouter();
  const [form, setForm] = useState<CouponCreateRequest>({
    name: "",
    discountType: "FIXED",
    discountValue: 0,
    couponType: "ALL",
    expiredAt: "",
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
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
          <input
            type="text"
            name="name"
            placeholder="쿠폰 이름"
            value={form.name}
            onChange={handleChange}
            required
            className="border p-2 rounded"
          />

          <select
            name="discountType"
            value={form.discountType}
            onChange={handleChange}
            className="border p-2 rounded"
          >
            <option value="FIXED">금액 할인</option>
            <option value="RATE">퍼센트 할인</option>
          </select>

          <input
            type="number"
            name="discountValue"
            placeholder="할인 값"
            value={form.discountValue}
            onChange={handleChange}
            required
            className="border p-2 rounded"
          />

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

          {(form.couponType === "CATEGORY") && (
            <input
              type="text"
              name="category"
              placeholder="카테고리"
              value={form.category || ""}
              onChange={handleChange}
              required
              className="border p-2 rounded"
            />
          )}

          {(form.couponType === "TARGET") && (
            <input
              type="number"
              name="itemId"
              placeholder="상품 ID"
              value={form.itemId || ""}
              onChange={handleChange}
              required
              className="border p-2 rounded"
            />
          )}

          <input
            type="datetime-local"
            name="expiredAt"
            value={form.expiredAt}
            onChange={handleChange}
            required
            className="border p-2 rounded"
          />

          <button type="submit" className="bg-blue-500 text-white p-2 rounded">
            쿠폰 생성
          </button>
        </form>
      </div>
    </>
  );
}
