"use client";

import { fetchApi } from "@/lib/api";
import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

interface UserInfo {
  nickname: string;
  email: string;
  userRole: string;
  profileImg?: string;
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

export default function MyPage() {
  const router = useRouter();

  const [user, setUser] = useState<UserInfo | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);

  // 프로필 조회
  useEffect(() => {
    const fetchUser = async () => {
      try {
        const data = await fetchApi<UserApiResponse>("/user/my", {
          credentials: "include",
        });
        setUser(data.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    const fetchAddresses = async () => {
      try {
        const data = await fetchApi<AddressesApiResponse>("/address", {
          credentials: "include",
        });
        setAddresses(data.data);
      } catch (err) {
        console.error(err);
      }
    };

    fetchUser();
    fetchAddresses();
  }, []);

  if (loading) return <div className="p-8">로딩 중...</div>;

  return (
    <div className="bg-gray-50 min-h-screen py-8">
      <div className="max-w-4xl mx-auto space-y-6 px-4">
        {/* ===== 프로필 카드 ===== */}
        <div className="bg-white shadow-md rounded-lg p-6 flex items-center space-x-6">
          <img
            src={user?.profileImg || "/default_profile.png"}
            alt="프로필 이미지"
            className="w-24 h-24 rounded-full object-cover border-2 border-primary"
          />
          <div>
            <h2 className="text-2xl font-semibold text-gray-800">
              {user?.nickname}
            </h2>
            <p className="text-gray-500">{user?.email}</p>
            <p className="mt-1 text-gray-600 font-medium">
              역할: {user?.userRole.toUpperCase()}
            </p>
            <button
              className="mt-3 px-4 py-1 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition"
              onClick={() => router.push("/user/my/edit")}
            >
              프로필 수정
            </button>
          </div>
        </div>

        {/* ===== 마이페이지 주요 섹션 ===== */}
        {/* 카드들을 항상 세로로 일렬 배치 */}
        <div className="space-y-6">
          {/* 주소 관리 */}
          <div className="bg-white shadow-md rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-700 mb-4">
              배송지
            </h3>
            {addresses.length === 0 ? (
              <p className="text-gray-400">등록된 배송지가 없습니다.</p>
            ) : (
              addresses.map((addr) => (
                <div
                  key={addr.id}
                  className="border border-gray-200 rounded-lg p-3 mb-3 flex justify-between items-center"
                >
                  <div>
                    <p className="text-gray-700">{addr.roadAddress}</p>
                    <p className="text-gray-500 text-sm">
                      {addr.detailAddress} / {addr.recipientName} ({addr.recipientPhone})
                    </p>
                  </div>
                  <button
                    className="text-primary hover:underline"
                    onClick={() => router.push(`/user/address/${addr.id}/edit`)}
                  >
                    수정
                  </button>
                </div>
              ))
            )}
            <button
              className="mt-2 px-3 py-1 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition"
              onClick={() => router.push("/user/address/create")}
            >
              배송지 추가
            </button>
          </div>

          {/* 비밀번호 변경 */}
          <div className="bg-white shadow-md rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-700 mb-4">
              비밀번호 변경
            </h3>
            <form
              className="space-y-3"
              onSubmit={async (e) => {
                e.preventDefault();
                const currentPassword = (e.currentTarget.elements.namedItem("current") as HTMLInputElement).value;
                const newPassword = (e.currentTarget.elements.namedItem("new") as HTMLInputElement).value;

                try {
                  const res = await fetchApi("/user/my/password", {
                    method: "PATCH",
                    credentials: "include",
                    body: JSON.stringify({
                      currentPassword,
                      newPassword,
                    }),
                  });
                  alert("비밀번호가 변경되었습니다.");
                } catch (err) {
                  console.error(err);
                }
              }}
            >
              <input
                name="current"
                type="password"
                placeholder="현재 비밀번호"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
              />
              <input
                name="new"
                type="password"
                placeholder="새 비밀번호"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
              />
              <button
                type="submit"
                className="w-full bg-primary text-primary-foreground py-2 rounded-lg hover:bg-primary/90 transition"
              >
                변경
              </button>
            </form>
          </div>

          {/* 최근 주문 */}
          <div className="bg-white shadow-md rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-700 mb-4">
              최근 주문
            </h3>
            <button
              className="px-3 py-1 bg-secondary text-secondary-foreground rounded-lg hover:bg-secondary/80 transition"
              onClick={() => router.push("/orders")}
            >
              주문 내역 보기
            </button>
          </div>

          {/* 쿠폰 */}
          <div className="bg-white shadow-md rounded-lg p-6">
            <h3 className="text-lg font-semibold text-gray-700 mb-4">
              쿠폰
            </h3>
            <button
              className="px-3 py-1 bg-secondary text-secondary-foreground rounded-lg hover:bg-secondary/80 transition"
              onClick={() => router.push("/coupons/received")}
            >
              내 쿠폰 확인
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
