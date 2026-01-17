/**
 * * 통합 판매자 센터 접근 권한이 반영된 헤더 컴포넌트
 * 실무 가이드: 
 * 1. 개별 관리 기능(상품 등록, 쿠폰 발급)을 하나의 통합 대시보드(/seller)로 일원화함
 * 2. 권한(RBAC)에 따른 선별적 UI 노출로 사용자 경험(UX) 최적화
 * */
"use client";

import { Button } from "@/components/ui/button";
import { fetchApi } from "@/lib/api";
import React, { useEffect, useState, useCallback } from "react";
import { useRouter, usePathname } from "next/navigation";

/**
 * * 백엔드 UserInfoResponseDTO 구조와 일치하도록 인터페이스 정의
 * */
interface UserInfo {
  nickname: string;
  email: string;
  userRole: string;
  loginType: string;
  profileImg?: string;
}

interface Category {
  key: string;
  label: string;
}

interface UserApiResponse {
  data: UserInfo;
}

interface CartApiResponse {
  data: { quantity: number }[];
}

const categories: Category[] = [
  { key: "ELECTRONICS", label: "전자기기" },
  { key: "CLOTHING", label: "의류" },
  { key: "HOME", label: "가전/생활" },
  { key: "BOOKS", label: "도서" },
  { key: "BEAUTY", label: "뷰티/화장품" },
  { key: "OTHERS", label: "기타" },
];

export default function Header() {
  const router = useRouter();
  const pathname = usePathname();

  const [user, setUser] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [cartCount, setCartCount] = useState(0);

  const fetchUserData = useCallback(async () => {
    try {
      const data = await fetchApi<UserApiResponse>("/auth/me", {
        credentials: "include",
      });
      setUser(data.data);
    } catch (err) {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchCartData = useCallback(async () => {
    try {
      const data = await fetchApi<CartApiResponse>("/cart/list", {
        credentials: "include",
      });
      const list = Array.isArray(data.data) ? data.data : [];
      const count = list.reduce((acc: number, item: any) => acc + item.quantity, 0);
      setCartCount(count);
    } catch (err) {
      setCartCount(0);
    }
  }, []);

  useEffect(() => {
    fetchUserData();
    fetchCartData();

    const handleAuthChange = () => {
      fetchUserData();
      fetchCartData();
    };

    window.addEventListener("auth-change", handleAuthChange);
    return () => window.removeEventListener("auth-change", handleAuthChange);
  }, [fetchUserData, fetchCartData]);

  const handleLogout = async () => {
    try {
      await fetchApi("/auth/logout", {
        method: "POST",
        credentials: "include",
      });
    } catch (err) {
      console.error("Logout failed:", err);
    } finally {
      setUser(null);
      window.location.href = "/home"; 
    }
  };

  const isAllItems = pathname === "/items";
  const isCategoryActive = (key: string) => pathname === `/items/category/${key}`;

  return (
    <header className="bg-card shadow-md sticky top-0 z-50 border-b">
      <div className="flex justify-between items-center px-4 sm:px-10 py-4">
        {/* 로고 영역 */}
        <div className="cursor-pointer flex items-center" onClick={() => router.push("/home")}>
          <img
            src="/Shop1.png"
            alt="Shop1"
            className="h-14 w-auto hover:scale-105 transition-transform object-contain"
            onError={(e) => (e.currentTarget.style.display = 'none')}
          />
          <span className="text-2xl font-black ml-2 sm:hidden text-primary">Shop1</span>
        </div>

        {/* 우측 아이콘 영역 */}
        <div className="flex items-center space-x-2 sm:space-x-3">
          {loading ? (
            <div className="h-10 w-28 animate-pulse bg-muted rounded-full" />
          ) : user ? (
            <>
              <div className="flex items-center space-x-3 mr-4 border-r pr-4 border-gray-200">
                <span className="text-base font-bold hidden md:inline">
                  {user.nickname}님
                </span>
                <Button variant="outline" size="sm" onClick={handleLogout} className="font-semibold px-4">
                  로그아웃
                </Button>
              </div>

              {/* ✅ [수정] 통합 판매자 센터 버튼 (기존 등록, 쿠폰발급 버튼 대체) */}
              {["ADMIN", "SELLER"].includes(user.userRole.toUpperCase()) && (
                <Button 
                  variant="ghost" 
                  className="p-2.5 rounded-full hover:bg-gray-100" 
                  onClick={() => router.push("/seller")} 
                  title="판매자 센터"
                >
                  <img src="/seller.png" alt="판매자관리" className="w-7 h-7" />
                </Button>
              )}

              {/* 장바구니 버튼 */}
              <Button variant="ghost" className="p-2.5 rounded-full hover:bg-gray-100 relative" onClick={() => router.push("/cart")}>
                <img src="/cart.png" alt="장바구니" className="w-7 h-7" />
                {cartCount > 0 && (
                  <span className="absolute top-0 right-0 bg-destructive text-white text-[11px] font-bold w-5 h-5 flex items-center justify-center rounded-full border-2 border-white">
                    {cartCount > 9 ? '9+' : cartCount}
                  </span>
                )}
              </Button>

              {/* 주문내역 버튼 */}
              <Button variant="ghost" className="p-2.5 rounded-full hover:bg-gray-100" onClick={() => router.push("/orders")} title="주문내역">
                <img src="/order.png" alt="주문" className="w-7 h-7" />
              </Button>

              {/* 쿠폰함 버튼 */}
              <Button variant="ghost" className="p-2.5 rounded-full hover:bg-gray-100" onClick={() => router.push("/coupons/received")} title="쿠폰함">
                <img src="/coupon.png" alt="쿠폰" className="w-7 h-7" />
              </Button>

              {/* 마이페이지 버튼 */}
              <Button variant="ghost" className="p-2.5 rounded-full hover:bg-gray-100" onClick={() => router.push("/mypage")} title="마이페이지">
                <img src="/my_page.png" alt="MY" className="w-7 h-7" />
              </Button>
            </>
          ) : (
            <Button size="lg" className="px-6 font-bold text-base" onClick={() => router.push("/register")}>
              로그인 / 회원가입
            </Button>
          )}
        </div>
      </div>

      {/* 카테고리 내비게이션 */}
      <nav className="bg-primary text-primary-foreground shadow-inner">
        <div className="max-w-7xl mx-auto px-4 sm:px-10 h-14 flex items-center overflow-x-auto scrollbar-hide">
          <button
            onClick={() => router.push("/items")}
            className={`px-6 h-full flex items-center text-base font-bold whitespace-nowrap transition-all
            ${isAllItems ? "bg-white/20" : "hover:bg-white/10"}`}
          >
            전체 상품
          </button>

          <div className="h-6 w-px bg-white/20 mx-2 flex-shrink-0" />

          {categories.map((cat, idx) => {
            const active = isCategoryActive(cat.key);
            return (
              <React.Fragment key={cat.key}>
                <button
                  onClick={() => router.push(`/items/category/${cat.key}`)}
                  className={`px-6 h-full flex items-center text-base font-semibold whitespace-nowrap transition-all
                  ${active ? "bg-white/20" : "hover:bg-white/10"}`}
                >
                  {cat.label}
                </button>
                {idx !== categories.length - 1 && (
                  <div className="h-5 w-px bg-white/10 mx-1 flex-shrink-0" />
                )}
              </React.Fragment>
            );
          })}
        </div>
      </nav>
    </header>
  );
}