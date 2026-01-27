"use client";

import { fetchApi } from "@/lib/api";
import { useEffect, useState, Suspense } from "react"; // Suspense 추가
import { useRouter, useSearchParams } from "next/navigation";

const DEFAULT_IMAGE = "/no_image.jpg";

interface Item {
  id: number;
  name: string;
  price: number;
  thumbnailUrl?: string;
  images?: { imageUrl: string }[];
}

interface ApiResponse {
  success: boolean;
  data: Item[];
}

// 1. 실제 로직이 들어있는 컴포넌트 (기존 ItemsPage 내용)
function ItemsContent() {
  const router = useRouter();
  const searchParams = useSearchParams(); 

  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);

  // 검색 상태 관리
  const [keyword, setKeyword] = useState("");
  const [category, setCategory] = useState("");
  const [sort, setSort] = useState("latest");

  // 검색 함수
  const fetchItems = () => {
    setLoading(true);

    const params = new URLSearchParams();
    if (keyword) params.append("keyword", keyword);
    if (category) params.append("category", category);
    if (sort) params.append("sort", sort);

    fetchApi<ApiResponse>(`/items?${params.toString()}`)
      .then((data) => {
        if (data.success) setItems(data.data);
      })
      .catch((err) => console.error("상품 목록 로드 실패:", err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchItems();
  }, [category, sort]); 

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchItems();
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <main className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8">
        
        {/* 상단 헤더 & 필터 영역 */}
        <div className="flex flex-col md:flex-row justify-between items-center mb-8 gap-4">
          <h1 className="text-2xl font-bold text-gray-800">상품 둘러보기</h1>

          <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
            {/* 카테고리 필터 */}
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-black"
            >
              <option value="">모든 카테고리</option>
              <option value="ELECTRONICS">전자기기</option>
              <option value="CLOTHING">의류</option>
              <option value="HOME">가전/생활</option>
              <option value="BOOKS">도서</option>
              <option value="BEAUTY">뷰티</option>
              <option value="OTHERS">기타</option>
            </select>

            {/* 정렬 필터 */}
            <select
              value={sort}
              onChange={(e) => setSort(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-black"
            >
              <option value="latest">최신순</option>
              <option value="priceLow">낮은 가격순</option>
              <option value="priceHigh">높은 가격순</option>
              <option value="views">인기순(조회수)</option>
            </select>

            {/* 검색어 입력 */}
            <form onSubmit={handleSearch} className="relative">
              <input
                type="text"
                placeholder="상품명 검색..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="pl-3 pr-10 py-2 border border-gray-300 rounded-lg text-sm w-full sm:w-64 focus:outline-none focus:ring-2 focus:ring-black"
              />
              <button 
                type="submit"
                className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-500 hover:text-black"
              >
                🔍
              </button>
            </form>
          </div>
        </div>

        {/* 상품 목록 표시 */}
        {loading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
          </div>
        ) : items.length === 0 ? (
          <div className="text-center py-20 bg-white rounded-lg shadow-sm border border-gray-100">
            <p className="text-gray-500 text-lg">검색 결과가 없습니다.</p>
            <p className="text-gray-400 text-sm mt-1">다른 검색어나 필터를 사용해보세요.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {items.map((item) => {
              const imageUrl = item.thumbnailUrl 
                ? item.thumbnailUrl 
                : (item.images && item.images.length > 0 ? item.images[0].imageUrl : DEFAULT_IMAGE);

              return (
                <div
                  key={item.id}
                  onClick={() => router.push(`/items/${item.id}`)}
                  className="bg-white border border-gray-100 rounded-lg shadow-sm hover:shadow-md hover:-translate-y-1 transition-all duration-300 cursor-pointer overflow-hidden group"
                >
                  <div className="aspect-[4/3] overflow-hidden bg-gray-100 relative">
                    <img
                      src={imageUrl}
                      alt={item.name}
                      onError={(e) => { e.currentTarget.src = DEFAULT_IMAGE; }}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  </div>

                  <div className="p-4 flex flex-col gap-2">
                    <h2 className="text-lg font-medium text-gray-900 line-clamp-1">
                      {item.name}
                    </h2>
                    <p className="text-lg font-bold text-gray-900">
                      {item.price.toLocaleString()}원
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}

// 2. Suspense로 감싸는 메인 페이지 컴포넌트
export default function ItemsPage() {
  return (
    // useSearchParams를 사용하는 컴포넌트 경계에 Suspense를 적용합니다.
    <Suspense fallback={
      <div className="flex justify-center items-center h-screen bg-gray-50">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
      </div>
    }>
      <ItemsContent />
    </Suspense>
  );
}