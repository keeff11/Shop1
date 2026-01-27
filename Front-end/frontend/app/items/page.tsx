"use client";

import { fetchApi } from "@/lib/api";
import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner"; // 또는 react-hot-toast

const DEFAULT_IMAGE = "/no_image.jpg";

function ItemsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // URL 파라미터 읽기
  const initialKeyword = searchParams.get("keyword") || "";
  const initialCategory = searchParams.get("category") || "";
  const initialSort = searchParams.get("sort") || "latest";
  const initialMinPrice = searchParams.get("minPrice") || "";
  const initialMaxPrice = searchParams.get("maxPrice") || "";
  const initialPage = parseInt(searchParams.get("page") || "0", 10);

  // 상태 관리
  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(initialPage);

  // 필터 상태
  const [keyword, setKeyword] = useState(initialKeyword);
  const [category, setCategory] = useState(initialCategory);
  const [sort, setSort] = useState(initialSort);
  const [minPrice, setMinPrice] = useState(initialMinPrice);
  const [maxPrice, setMaxPrice] = useState(initialMaxPrice);

  // 데이터 조회
  const fetchItems = async (pageNum: number) => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (keyword) params.append("keyword", keyword);
      if (category) params.append("category", category);
      if (sort) params.append("sort", sort);
      if (minPrice) params.append("minPrice", minPrice);
      if (maxPrice) params.append("maxPrice", maxPrice);
      
      params.append("page", pageNum.toString());
      params.append("size", "12");

      router.replace(`/items?${params.toString()}`, { scroll: false });

      const res = await fetchApi<any>(`/items?${params.toString()}`);
      if (res.success) {
        setItems(res.data.content);
        setTotalPages(res.data.totalPages);
        setPage(res.data.number);
      }
    } catch (err) {
      console.error(err);
      toast.error("상품 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPage(0);
    fetchItems(0);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") handleSearch();
  };

  // [수정] 삭제 처리
  const handleDelete = async (e: React.MouseEvent, itemId: number) => {
    e.stopPropagation();
    if (!confirm("정말 삭제하시겠습니까? 복구할 수 없습니다.")) return;

    try {
      await fetchApi(`/items/${itemId}`, { method: "DELETE" });
      toast.success("상품이 삭제되었습니다.");
      
      // [핵심] 삭제된 아이템을 현재 목록에서 즉시 제거 (새로고침 없이 반영)
      setItems((prevItems) => prevItems.filter((item) => item.id !== itemId));
      
    } catch (error: any) {
      toast.error(error.message || "삭제 권한이 없거나 실패했습니다.");
    }
  };

  useEffect(() => {
    fetchItems(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); 

  const renderHighlightedText = (text: string, highlight: string) => {
    if (!highlight) return text;
    const parts = text.split(new RegExp(`(${highlight})`, "gi"));
    return parts.map((part, i) =>
      part.toLowerCase() === highlight.toLowerCase() ? (
        <span key={i} className="bg-yellow-200 font-bold">{part}</span>
      ) : (
        part
      )
    );
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* 필터 UI 생략 (기존과 동일) */}
      
      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 mb-8 space-y-4">
        <div className="flex flex-col md:flex-row justify-between gap-4">
          <h1 className="text-2xl font-bold text-gray-900">📦 상품 둘러보기</h1>
          <div className="relative w-full md:w-96">
            <input
              type="text"
              placeholder="검색어 입력..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={handleKeyDown}
              className="w-full pl-4 pr-10 py-2.5 border rounded-lg focus:ring-2 focus:ring-black outline-none"
            />
            <button onClick={handleSearch} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-black">🔍</button>
          </div>
        </div>
        {/* 필터 옵션들... (기존 코드 유지) */}
      </div>

      {/* 리스트 */}
      {loading ? (
        <div className="flex justify-center py-20"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-black"></div></div>
      ) : items.length === 0 ? (
        <div className="text-center py-20 text-gray-500 bg-white rounded-xl border border-dashed border-gray-300">검색 결과가 없습니다.</div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {items.map((item) => (
            <div
              key={item.id}
              onClick={() => router.push(`/items/${item.id}`)}
              className="group bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-md transition-all duration-300 cursor-pointer overflow-hidden relative"
            >
              <div className="aspect-[4/3] bg-gray-100 overflow-hidden relative">
                <img
                  src={item.thumbnailUrl || DEFAULT_IMAGE}
                  alt={item.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />
              </div>

              {/* 삭제 버튼 (목록에서도 삭제 가능하도록) */}
              <button
                onClick={(e) => handleDelete(e, item.id)}
                className="absolute top-2 right-2 z-10 bg-white/90 text-red-500 p-2 rounded-full opacity-0 group-hover:opacity-100 transition hover:bg-red-50 shadow-sm"
                title="상품 삭제"
              >
                🗑️
              </button>

              <div className="p-4">
                <h3 className="text-gray-900 font-medium line-clamp-1 mb-1">
                  {renderHighlightedText(item.name, keyword)}
                </h3>
                <p className="text-lg font-bold text-gray-900">{item.price.toLocaleString()}원</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 페이징 UI 생략 (기존과 동일) */}
    </div>
  );
}

export default function ItemsPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Suspense fallback={<div className="flex justify-center items-center min-h-screen">Loading...</div>}>
        <ItemsContent />
      </Suspense>
    </div>
  );
}