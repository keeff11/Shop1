"use client";

import { Button } from "@/components/ui/button";
import { fetchApi } from "@/lib/api";
import React, { useEffect, useState, useCallback, useRef } from "react";
import { useRouter, usePathname } from "next/navigation";

/**
 * 백엔드 구조와 일치하도록 인터페이스 정의
 */
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

// 🌟 자동완성 검색결과 데이터 타입
interface SuggestionItem {
  id: number;
  name: string;
  price: number;
  thumbnailUrl: string;
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

  // 검색 관련 상태 관리
  const [searchQuery, setSearchQuery] = useState("");
  const [suggestions, setSuggestions] = useState<SuggestionItem[]>([]);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // 유저 정보 조회
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

  // 장바구니 수량 조회
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
    // 1. 초기 로드
    fetchUserData();
    fetchCartData();

    // 2. 이벤트 핸들러 정의
    const handleAuthChange = () => {
      fetchUserData();
      fetchCartData();
    };

    const handleCartUpdate = () => {
      fetchCartData();
    };

    // 3. 이벤트 리스너 등록
    window.addEventListener("auth-change", handleAuthChange);
    window.addEventListener("cart-updated", handleCartUpdate);

    // 4. 클린업
    return () => {
      window.removeEventListener("auth-change", handleAuthChange);
      window.removeEventListener("cart-updated", handleCartUpdate);
    };
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
      setCartCount(0);
      window.location.href = "/home"; 
    }
  };

  // 외부 클릭 시 검색 드롭다운 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Debounce를 적용한 실시간 자동완성 API 호출 (Elasticsearch 연동)
  useEffect(() => {
    if (!searchQuery.trim()) {
      setSuggestions([]);
      setIsDropdownOpen(false);
      return;
    }

    // 300ms 딜레이를 주어 서버 부하(과도한 API 호출) 방지
    const delayDebounceFn = setTimeout(async () => {
      try {
        const response: any = await fetchApi(`/items/search/autocomplete?keyword=${encodeURIComponent(searchQuery)}`);
        if (response.data && response.data.length > 0) {
          setSuggestions(response.data);
          setIsDropdownOpen(true);
        } else {
          setSuggestions([]);
          setIsDropdownOpen(false);
        }
      } catch (error) {
        console.error("자동완성 데이터를 불러오는데 실패했습니다.", error);
      }
    }, 300); 

    return () => clearTimeout(delayDebounceFn);
  }, [searchQuery]);

  // 엔터키 입력 시 검색 결과 페이지로 이동 (일반 검색)
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      setIsDropdownOpen(false);
      router.push(`/items?search=${encodeURIComponent(searchQuery)}`);
    }
  };

  // 자동완성 드롭다운에서 상품 클릭 시 상세 페이지로 이동
  const handleSuggestionClick = (itemId: number) => {
    setIsDropdownOpen(false);
    setSearchQuery("");
    router.push(`/items/${itemId}`);
  };

  const isAllItems = pathname === "/items";
  const isCategoryActive = (key: string) => pathname === `/items/category/${key}`;

  return (
    <header className="bg-card shadow-md sticky top-0 z-50 border-b">
      <div className="flex justify-between items-center px-4 sm:px-10 py-4 gap-4 md:gap-8">
        
        {/* 1. 로고 영역 (왼쪽) */}
        <div className="cursor-pointer flex items-center flex-shrink-0" onClick={() => router.push("/home")}>
          <img
            src="/Shop1.png"
            alt="Shop1"
            className="h-10 md:h-14 w-auto hover:scale-105 transition-transform object-contain"
            onError={(e) => (e.currentTarget.style.display = 'none')}
          />
          {/* 텍스트 로고 삭제됨 */}
        </div>

        {/* 2. 검색창 & 자동완성 영역 (중앙) */}
        <div className="flex-1 max-w-2xl relative" ref={dropdownRef}>
          <form onSubmit={handleSearchSubmit} className="relative w-full">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onFocus={() => { if (suggestions.length > 0) setIsDropdownOpen(true); }}
              placeholder="찾으시는 상품을 검색해보세요"
              className="w-full bg-gray-100/80 text-gray-900 text-sm rounded-full pl-5 pr-12 py-2.5 outline-none focus:ring-2 focus:ring-primary focus:bg-white transition-all border border-transparent"
            />
            <button
              type="submit"
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1.5 text-gray-400 hover:text-primary transition-colors"
            >
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth="2.5" stroke="currentColor" className="w-5 h-5">
                <path strokeLinecap="round" strokeLinejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
              </svg>
            </button>
          </form>

          {/* 자동완성 드롭다운 UI */}
          {isDropdownOpen && (
            <div className="absolute top-[calc(100%+8px)] left-0 right-0 bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden z-[60] animate-in fade-in slide-in-from-top-2 duration-200">
              <ul className="max-h-[350px] overflow-y-auto scrollbar-hide py-2">
                <li className="px-4 py-2 text-[11px] font-bold text-primary/70 bg-primary/5 uppercase tracking-wider">추천 검색 결과</li>
                {suggestions.map((item) => (
                  <li 
                    key={item.id} 
                    onClick={() => handleSuggestionClick(item.id)}
                    className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 cursor-pointer border-b border-gray-50 last:border-0 transition-colors"
                  >
                    <div className="w-10 h-10 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0 border border-gray-200/50">
                      <img 
                        src={item.thumbnailUrl || '/no_image.jpg'} 
                        alt={item.name} 
                        className="w-full h-full object-cover"
                        onError={(e) => { e.currentTarget.src = '/no_image.jpg'; }}
                      />
                    </div>
                    <div className="flex flex-col overflow-hidden">
                      <span className="text-sm font-medium text-gray-800 truncate">{item.name}</span>
                      <span className="text-xs font-bold text-primary mt-0.5">{item.price.toLocaleString()}원</span>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        {/* 3. 우측 유저 아이콘 영역 (오른쪽) */}
        <div className="flex items-center space-x-1 sm:space-x-2 flex-shrink-0">
          {loading ? (
            <div className="h-10 w-28 animate-pulse bg-muted rounded-full" />
          ) : user ? (
            <>
              <div className="hidden md:flex items-center space-x-3 mr-2 border-r pr-4 border-gray-200">
                <span className="text-sm font-bold text-gray-700">
                  {user.nickname}님
                </span>
                <Button variant="outline" size="sm" onClick={handleLogout} className="text-xs px-3 h-8">
                  로그아웃
                </Button>
              </div>

              {/* 모바일용 로그아웃 (숨김처리) */}
              <div className="md:hidden mr-1">
                 <Button variant="ghost" size="sm" onClick={handleLogout} className="text-xs px-2 h-8 text-gray-500 hover:bg-gray-100">
                  로그아웃
                </Button>
              </div>

              {["ADMIN", "SELLER"].includes(user.userRole.toUpperCase()) && (
                <Button 
                  variant="ghost" 
                  className="p-2 sm:p-2.5 rounded-full hover:bg-gray-100" 
                  onClick={() => router.push("/seller")} 
                  title="판매자 센터"
                >
                  <img src="/seller.png" alt="판매자관리" className="w-6 h-6 sm:w-7 sm:h-7" />
                </Button>
              )}

              <Button variant="ghost" className="p-2 sm:p-2.5 rounded-full hover:bg-gray-100 relative" onClick={() => router.push("/cart")}>
                <img src="/cart.png" alt="장바구니" className="w-6 h-6 sm:w-7 sm:h-7" />
                {cartCount > 0 && (
                  <span className="absolute top-0 right-0 bg-destructive text-white text-[10px] sm:text-[11px] font-bold w-4 h-4 sm:w-5 sm:h-5 flex items-center justify-center rounded-full border-2 border-white animate-in zoom-in duration-200">
                    {cartCount > 9 ? '9+' : cartCount}
                  </span>
                )}
              </Button>

              <Button variant="ghost" className="p-2 sm:p-2.5 rounded-full hover:bg-gray-100" onClick={() => router.push("/orders")} title="주문내역">
                <img src="/order.png" alt="주문" className="w-6 h-6 sm:w-7 sm:h-7" />
              </Button>

              <Button variant="ghost" className="p-2 sm:p-2.5 rounded-full hover:bg-gray-100" onClick={() => router.push("/coupons/received")} title="쿠폰함">
                <img src="/coupon.png" alt="쿠폰" className="w-6 h-6 sm:w-7 sm:h-7" />
              </Button>

              <Button variant="ghost" className="p-2 sm:p-2.5 rounded-full hover:bg-gray-100" onClick={() => router.push("/mypage")} title="마이페이지">
                <img src="/my_page.png" alt="MY" className="w-6 h-6 sm:w-7 sm:h-7" />
              </Button>
            </>
          ) : (
            <Button size="sm" className="px-4 font-bold text-sm h-9 md:h-10" onClick={() => router.push("/register")}>
              로그인 / 회원가입
            </Button>
          )}
        </div>
      </div>

      {/* 4. 하단 카테고리 내비게이션 (유지) */}
      <nav className="bg-primary text-primary-foreground shadow-inner">
        <div className="max-w-7xl mx-auto px-4 sm:px-10 h-12 sm:h-14 flex items-center overflow-x-auto scrollbar-hide">
          <button
            onClick={() => router.push("/items")}
            className={`px-4 sm:px-6 h-full flex items-center text-sm sm:text-base font-bold whitespace-nowrap transition-all
            ${isAllItems ? "bg-white/20" : "hover:bg-white/10"}`}
          >
            전체 상품
          </button>

          <div className="h-5 sm:h-6 w-px bg-white/20 mx-1 sm:mx-2 flex-shrink-0" />

          {categories.map((cat, idx) => {
            const active = isCategoryActive(cat.key);
            return (
              <React.Fragment key={cat.key}>
                <button
                  onClick={() => router.push(`/items/category/${cat.key}`)}
                  className={`px-4 sm:px-6 h-full flex items-center text-sm sm:text-base font-semibold whitespace-nowrap transition-all
                  ${active ? "bg-white/20" : "hover:bg-white/10"}`}
                >
                  {cat.label}
                </button>
                {idx !== categories.length - 1 && (
                  <div className="h-4 sm:h-5 w-px bg-white/10 mx-0.5 sm:mx-1 flex-shrink-0" />
                )}
              </React.Fragment>
            );
          })}
        </div>
      </nav>
    </header>
  );
}