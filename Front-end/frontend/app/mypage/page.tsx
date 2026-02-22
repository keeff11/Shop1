"use client";

import { fetchApi } from "@/lib/api";
import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

interface UserInfo {
  nickname: string;
  email: string;
  userRole: string; // "USER", "SELLER", "ADMIN" 등
  profileImg?: string;
  loginType: string;
}

interface UserApiResponse {
  data: UserInfo;
}

interface Address {
  id: number;
  roadAddress: string;
  detailAddress: string;
  recipientName: string;
  recipientPhone: string;
}

interface AddressesApiResponse {
  data: Address[];
}

// 쿠폰 요약 정보를 위한 인터페이스 추가
interface CouponResponse {
  couponId: number;
  used: boolean;
}

interface CouponsApiResponse {
  data: CouponResponse[];
}

export default function MyPage() {
  const router = useRouter();

  const [user, setUser] = useState<UserInfo | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [usableCouponCount, setUsableCouponCount] = useState<number>(0); // 사용 가능 쿠폰 수 상태
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const data = await fetchApi<UserApiResponse>("/user/my", {
          credentials: "include",
        });
        setUser(data.data);
      } catch (err) {
        console.error("유저 정보 조회 실패:", err);
      } finally {
        setLoading(false);
      }
    };

    const fetchAddresses = async () => {
      try {
        const data = await fetchApi<AddressesApiResponse>("/user/addresses", {
          credentials: "include",
        });
        setAddresses(data.data);
      } catch (err) {
        console.error("배송지 조회 실패:", err);
      }
    };

    // 마이페이지 대시보드용 쿠폰 데이터 호출
    const fetchCoupons = async () => {
      try {
        const data = await fetchApi<CouponsApiResponse>("/coupons/my", {
          credentials: "include",
        });
        // used가 false인(사용 가능한) 쿠폰 개수만 필터링
        const count = data.data?.filter(coupon => !coupon.used).length || 0;
        setUsableCouponCount(count);
      } catch (err) {
        console.error("쿠폰 정보 조회 실패:", err);
      }
    };

    fetchUser();
    fetchAddresses();
    fetchCoupons(); // 쿠폰 호출 추가
  }, []);

  if (loading) return <div className="p-8 text-center text-gray-500">로딩 중...</div>;

  return (
    <div className="bg-gray-50 min-h-screen py-8">
      <div className="max-w-4xl mx-auto space-y-6 px-4">
        {/* ===== 프로필 카드 ===== */}
        <div className="bg-white shadow-md rounded-lg p-6 flex items-center space-x-6">
          <img
            src={user?.profileImg || "/default_profile.png"}
            alt="프로필 이미지"
            className="w-24 h-24 rounded-full object-cover border-2 border-blue-100"
          />
          <div>
            <h2 className="text-2xl font-bold text-gray-800">
              {user?.nickname}
            </h2>
            <p className="text-gray-500">{user?.email}</p>
            <div className="mt-2 space-x-2">
              <span className={`font-semibold text-xs px-2 py-1 rounded ${
                user?.userRole === "ADMIN" ? "bg-red-100 text-red-600" :
                user?.userRole === "SELLER" ? "bg-green-100 text-green-600" :
                "bg-gray-100 text-gray-600"
              }`}>
                {user?.userRole.toUpperCase()}
              </span>
              <span className="text-gray-600 font-medium text-xs px-2 py-1 bg-gray-100 rounded">
                {user?.loginType}
              </span>
            </div>
            
            <button
              className="mt-4 px-4 py-1.5 text-sm bg-blue-50 text-blue-600 font-semibold rounded-lg hover:bg-blue-100 transition"
              onClick={() => router.push("/mypage/edit")}
            >
              프로필 수정
            </button>
          </div>
        </div>

        {/* ===== 마이페이지 주요 섹션 ===== */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          
          {/* 쿠폰 요약 대시보드 (새로 추가/수정된 부분) */}
          <div className="bg-white shadow-md rounded-lg p-6 border-l-4 border-blue-500 flex flex-col justify-between">
            <div>
              <div className="flex justify-between items-center mb-2">
                <h3 className="text-lg font-bold text-gray-800">내 쿠폰함</h3>
                <span className="text-2xl font-black text-blue-600">{usableCouponCount}장</span>
              </div>
              <p className="text-sm text-gray-500 mb-6">현재 결제 시 사용할 수 있는 쿠폰입니다.</p>
            </div>
            
            <div className="flex gap-2">
              <button
                className="flex-1 py-2 bg-gray-100 text-gray-700 font-semibold rounded-lg hover:bg-gray-200 transition"
                onClick={() => router.push("/coupons/received")}
              >
                쿠폰함 보기
              </button>

              {/* 관리자 또는 판매자 전용 '쿠폰 생성' 메뉴 노출 */}
              {(user?.userRole === "ADMIN" || user?.userRole === "SELLER") && (
                <button
                  className="flex-1 py-2 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition"
                  onClick={() => router.push("/coupons/create")}
                >
                  쿠폰 발급/관리
                </button>
              )}
            </div>
          </div>

          {/* 최근 주문 (디자인 약간 개선) */}
          <div className="bg-white shadow-md rounded-lg p-6 flex flex-col justify-between">
             <div>
              <h3 className="text-lg font-bold text-gray-800 mb-2">최근 주문</h3>
              <p className="text-sm text-gray-500 mb-6">최근 결제하신 주문 내역을 확인합니다.</p>
             </div>
            <button
              className="w-full py-2 bg-gray-100 text-gray-700 font-semibold rounded-lg hover:bg-gray-200 transition"
              onClick={() => router.push("/orders")}
            >
              주문 내역 보기
            </button>
          </div>
        </div>

        {/* 하단 섹션 (주소지 관리 및 비밀번호 변경) */}
        <div className="space-y-6">
          {/* 주소 관리 */}
          <div className="bg-white shadow-md rounded-lg p-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-bold text-gray-800">배송지 관리</h3>
              <button
                className="px-3 py-1.5 text-sm bg-blue-50 text-blue-600 font-semibold rounded-lg hover:bg-blue-100 transition"
                onClick={() => router.push("/mypage/address/create")}
              >
                + 새 배송지 추가
              </button>
            </div>
            {addresses.length === 0 ? (
              <p className="text-center py-6 text-gray-400 border border-dashed rounded-lg">등록된 배송지가 없습니다.</p>
            ) : (
              <div className="grid gap-3">
                {addresses.map((addr) => (
                  <div
                    key={addr.id}
                    className="border border-gray-100 bg-gray-50 rounded-lg p-4 flex justify-between items-center"
                  >
                    <div>
                      <p className="font-semibold text-gray-800">{addr.recipientName} <span className="text-gray-400 font-normal text-sm ml-2">{addr.recipientPhone}</span></p>
                      <p className="text-gray-600 text-sm mt-1">{addr.roadAddress} {addr.detailAddress}</p>
                    </div>
                    <button
                      className="text-gray-400 hover:text-blue-500 text-sm font-semibold underline"
                      onClick={() => router.push(`/mypage/address/${addr.id}/edit`)}
                    >
                      수정
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 비밀번호 변경 (LOCAL 로그인 유저에게만 표시) */}
          {user?.loginType === "LOCAL" && (
            <div className="bg-white shadow-md rounded-lg p-6">
              <h3 className="text-lg font-bold text-gray-800 mb-4">
                비밀번호 변경
              </h3>
              <form
                className="space-y-3 max-w-md"
                onSubmit={async (e) => {
                  e.preventDefault();
                  const currentPassword = (e.currentTarget.elements.namedItem("current") as HTMLInputElement).value;
                  const newPassword = (e.currentTarget.elements.namedItem("new") as HTMLInputElement).value;

                  try {
                    await fetchApi("/user/password", {
                      method: "PATCH",
                      credentials: "include",
                      body: JSON.stringify({
                        currentPassword,
                        newPassword,
                      }),
                    });
                    alert("비밀번호가 변경되었습니다.");
                    e.currentTarget.reset(); // 성공 시 폼 초기화
                  } catch (err) {
                    console.error(err);
                    alert("비밀번호 변경에 실패했습니다.");
                  }
                }}
              >
                <input
                  name="current"
                  type="password"
                  placeholder="현재 비밀번호"
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
                  required
                />
                <input
                  name="new"
                  type="password"
                  placeholder="새 비밀번호"
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
                  required
                />
                <button
                  type="submit"
                  className="w-full bg-gray-800 text-white font-semibold py-2 rounded-lg hover:bg-gray-900 transition"
                >
                  비밀번호 변경
                </button>
              </form>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}