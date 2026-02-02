"use client";

// ... (기존 import문 동일)
import { fetchApi } from "@/lib/api";
import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { Search, RotateCcw, ChevronLeft, ChevronRight, SlidersHorizontal, ShoppingCart } from "lucide-react";
import DualRangeSlider from "@/components/ui/dual-range-slider";

const DEFAULT_IMAGE = "/no_image.jpg";

// ... (CategoryMap, SortOptions 등 기존 코드 동일)
const categoryMap: Record<string, string> = {
  ELECTRONICS: "전자기기",
  CLOTHING: "의류",
  HOME: "가전/생활",
  BOOKS: "도서",
  BEAUTY: "뷰티/화장품",
  OTHERS: "기타",
};

const sortOptions = [
  { value: "latest", label: "최신순" },
  { value: "priceHigh", label: "높은가격순" },
  { value: "priceLow", label: "낮은가격순" },
  { value: "views", label: "인기순" },
];

const MIN_PRICE_LIMIT = 0;
const MAX_PRICE_LIMIT = 1000000;

function ItemsContent() {
  // ... (기존 상태 및 fetch 로직 동일)
  const router = useRouter();
  const searchParams = useSearchParams();

  const initialKeyword = searchParams.get("keyword") || "";
  const initialCategory = searchParams.get("category") || "";
  const initialSort = searchParams.get("sort") || "latest";
  const initialMinPrice = parseInt(searchParams.get("minPrice") || "0", 10);
  const initialMaxPrice = parseInt(searchParams.get("maxPrice") || String(MAX_PRICE_LIMIT), 10);
  const initialPage = parseInt(searchParams.get("page") || "0", 10);

  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(initialPage);

  const [keyword, setKeyword] = useState(initialKeyword);
  const [category, setCategory] = useState(initialCategory);
  const [sort, setSort] = useState(initialSort);
  const [priceRange, setPriceRange] = useState<[number, number]>([initialMinPrice, initialMaxPrice]);
  const [showFilters, setShowFilters] = useState(true);

  const fetchItems = async (pageNum: number) => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (keyword) params.append("keyword", keyword);
      if (category) params.append("category", category);
      if (sort) params.append("sort", sort);
      if (priceRange[0] > MIN_PRICE_LIMIT) params.append("minPrice", priceRange[0].toString());
      if (priceRange[1] < MAX_PRICE_LIMIT) params.append("maxPrice", priceRange[1].toString());
      
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
      toast.error("상품 목록 로드 실패");
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

  const handleReset = () => {
    setKeyword("");
    setCategory("");
    setSort("latest");
    setPriceRange([MIN_PRICE_LIMIT, MAX_PRICE_LIMIT]);
    setPage(0);
    router.push("/items");
    setTimeout(() => window.location.reload(), 50);
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
      fetchItems(newPage);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const handleAddToCart = async (e: React.MouseEvent, itemId: number) => {
    e.stopPropagation();
    try {
        await fetchApi("/cart/add", {
            method: "POST",
            credentials: "include",
            body: JSON.stringify({ itemId, quantity: 1 }),
        });
        toast.success("장바구니에 담았습니다! 🛒");
        window.dispatchEvent(new Event("cart-updated"));
    } catch (err) {
        toast.error("로그인이 필요하거나 재고가 없습니다.");
    }
  };

  const handleDelete = async (e: React.MouseEvent, itemId: number) => {
    e.stopPropagation();
    if (!confirm("정말 삭제하시겠습니까?")) return;
    try {
      await fetchApi(`/items/${itemId}`, { method: "DELETE" });
      toast.success("상품 삭제 완료");
      setItems((prevItems) => prevItems.filter((item) => item.id !== itemId));
    } catch (error: any) {
      toast.error("삭제 권한이 없거나 실패했습니다.");
    }
  };

  // 가격 입력 핸들러
  const handlePriceInputChange = (index: 0 | 1, value: string) => {
    const numericValue = parseInt(value.replace(/[^0-9]/g, ""), 10);
    if (isNaN(numericValue)) {
       const newRange = [...priceRange] as [number, number];
       newRange[index] = 0;
       setPriceRange(newRange);
       return;
    }
    const newRange = [...priceRange] as [number, number];
    newRange[index] = numericValue;
    setPriceRange(newRange);
  };

  const formatPrice = (price: number) => {
    if (price >= 10000) return `${(price / 10000).toFixed(0)}만`;
    return `${price}`;
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
        <span key={i} className="bg-yellow-200 text-black font-semibold rounded-sm px-0.5">{part}</span>
      ) : part
    );
  };

  return (
    <div className="min-h-screen bg-gray-50/50 pb-20">
      <div className="bg-white border-b border-gray-200 sticky top-[60px] z-20 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
            {/* 상단 검색바 영역 (기존과 동일) */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
                        📦 상품 둘러보기
                        <span className="text-sm font-normal text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
                            Total {totalPages * 12}
                        </span>
                    </h1>
                </div>
                <div className="flex gap-2 w-full md:w-auto">
                    <div className="relative flex-grow md:w-80">
                        <input
                            type="text"
                            placeholder="검색어를 입력하세요..."
                            value={keyword}
                            onChange={(e) => setKeyword(e.target.value)}
                            onKeyDown={handleKeyDown}
                            className="w-full pl-4 pr-12 py-2.5 bg-gray-100 border-transparent focus:bg-white focus:border-black focus:ring-0 rounded-xl transition-all text-sm"
                        />
                        <button onClick={handleSearch} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-black p-1">
                            <Search className="w-5 h-5" />
                        </button>
                    </div>
                    <button onClick={() => setShowFilters(!showFilters)} className={`flex items-center gap-2 px-4 py-2.5 rounded-xl border transition-colors text-sm font-medium ${showFilters ? "bg-black text-white border-black" : "bg-white text-gray-700 border-gray-200 hover:bg-gray-50"}`}>
                        <SlidersHorizontal className="w-4 h-4" />
                        <span className="hidden sm:inline">필터</span>
                    </button>
                </div>
            </div>

            {/* 확장 필터 패널 */}
            {showFilters && (
                <div className="mt-6 pt-6 border-t border-gray-100 animate-in slide-in-from-top-2 duration-200">
                    <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
                        {/* 카테고리 & 정렬 (기존 동일) */}
                        <div className="md:col-span-5 space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-1.5">
                                    <label className="text-xs font-bold text-gray-500 uppercase">Category</label>
                                    <select value={category} onChange={(e) => setCategory(e.target.value)} className="w-full p-2.5 bg-white border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-black outline-none cursor-pointer">
                                        <option value="">전체 카테고리</option>
                                        {Object.entries(categoryMap).map(([key, label]) => (
                                            <option key={key} value={key}>{label}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="space-y-1.5">
                                    <label className="text-xs font-bold text-gray-500 uppercase">Sort By</label>
                                    <select value={sort} onChange={(e) => setSort(e.target.value)} className="w-full p-2.5 bg-white border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-black outline-none cursor-pointer">
                                        {sortOptions.map((opt) => (
                                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                        </div>

                        {/* [수정] 가격 슬라이더 */}
                        <div className="md:col-span-5 space-y-4 px-2">
                            <div className="flex justify-between items-end mb-2">
                                <label className="text-xs font-bold text-gray-500 uppercase">Price Range</label>
                                <div className="flex items-center gap-2">
                                    <div className="relative">
                                        <input 
                                            type="text"
                                            value={priceRange[0].toLocaleString()}
                                            onChange={(e) => handlePriceInputChange(0, e.target.value)}
                                            className="w-20 text-right text-sm font-bold border-b border-gray-300 focus:border-black outline-none bg-transparent p-0 pb-0.5 tabular-nums"
                                        />
                                        <span className="absolute right-0 bottom-6 text-[10px] text-gray-400">Min</span>
                                    </div>
                                    <span className="text-gray-400">~</span>
                                    <div className="relative">
                                        <input 
                                            type="text"
                                            value={priceRange[1].toLocaleString()}
                                            onChange={(e) => handlePriceInputChange(1, e.target.value)}
                                            className="w-24 text-right text-sm font-bold border-b border-gray-300 focus:border-black outline-none bg-transparent p-0 pb-0.5 tabular-nums"
                                        />
                                        <span className="absolute right-0 bottom-6 text-[10px] text-gray-400">Max</span>
                                    </div>
                                </div>
                            </div>
                            
                            <div className="py-2 px-1">
                                <DualRangeSlider
                                    min={MIN_PRICE_LIMIT}
                                    max={MAX_PRICE_LIMIT}
                                    step={1000}
                                    value={priceRange}
                                    onValueChange={setPriceRange}
                                />
                            </div>
                            
                            {/* 하단 눈금 라벨 */}
                            <div className="flex justify-between text-[10px] text-gray-400 font-medium">
                                <span>0원</span>
                                <span>25만원</span>
                                <span>50만원</span>
                                <span>75만원</span>
                                <span>100만원+</span>
                            </div>
                        </div>

                        {/* 액션 버튼 */}
                        <div className="md:col-span-2 flex flex-col justify-end gap-2">
                            <button onClick={handleSearch} className="w-full py-2.5 bg-black text-white rounded-lg text-sm font-bold hover:bg-gray-800 transition-colors shadow-lg">
                                적용하기
                            </button>
                            <button onClick={handleReset} className="w-full py-2.5 text-gray-500 bg-gray-50 hover:bg-gray-100 rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-1.5">
                                <RotateCcw className="w-3.5 h-3.5" /> 초기화
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
      </div>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {loading ? (
            <div className="flex flex-col items-center justify-center py-32 space-y-4">
                <div className="animate-spin rounded-full h-10 w-10 border-2 border-gray-200 border-t-black"></div>
                <p className="text-sm text-gray-500 font-medium">상품을 불러오는 중입니다...</p>
            </div>
        ) : items.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-32 bg-white rounded-2xl border border-dashed border-gray-200">
                <div className="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mb-4 text-3xl">🔍</div>
                <h3 className="text-lg font-bold text-gray-900 mb-1">검색 결과가 없습니다</h3>
                <p className="text-gray-500 text-sm mb-6">다른 검색어나 필터로 다시 시도해보세요.</p>
                <button onClick={handleReset} className="px-5 py-2.5 bg-black text-white rounded-lg text-sm font-medium hover:bg-gray-800 transition-colors">
                    필터 초기화
                </button>
            </div>
        ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-x-6 gap-y-10">
            {items.map((item) => (
                <div
                    key={item.id}
                    onClick={() => router.push(`/items/${item.id}`)}
                    className="group cursor-pointer flex flex-col gap-3"
                >
                    <div className="aspect-[1/1.1] w-full overflow-hidden rounded-2xl bg-gray-100 relative border border-gray-100/50">
                        <img
                            src={item.thumbnailUrl || DEFAULT_IMAGE}
                            alt={item.name}
                            className="h-full w-full object-cover object-center group-hover:scale-105 transition-transform duration-500"
                        />
                        {item.stockStatus === "OUT_OF_STOCK" && (
                            <div className="absolute inset-0 bg-black/60 flex items-center justify-center backdrop-blur-[1px]">
                                <span className="text-white font-bold px-3 py-1 border border-white/30 rounded-full text-xs tracking-wider">SOLD OUT</span>
                            </div>
                        )}
                        {item.stockStatus !== "OUT_OF_STOCK" && (
                            <button
                                onClick={(e) => handleAddToCart(e, item.id)}
                                className="absolute bottom-3 right-3 bg-white text-black p-2.5 rounded-full shadow-lg opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300 hover:bg-black hover:text-white"
                                title="장바구니 담기"
                            >
                                <ShoppingCart className="w-4 h-4" />
                            </button>
                        )}
                        {item.discountPrice && (
                            <div className="absolute top-3 left-3 bg-red-600 text-white text-[10px] font-bold px-2 py-1 rounded-md shadow-sm">
                                SALE
                            </div>
                        )}
                    </div>
                    <div className="space-y-1 px-1">
                        <div className="flex items-center justify-between text-xs text-gray-500">
                            <span>{categoryMap[item.category] || "기타"}</span>
                        </div>
                        <h3 className="text-sm font-medium text-gray-900 leading-snug line-clamp-2 group-hover:text-gray-600 transition-colors">
                            {renderHighlightedText(item.name, keyword)}
                        </h3>
                        <div className="flex items-baseline gap-2 mt-1">
                            <p className="text-base font-bold text-gray-900">
                                {item.discountPrice ? item.discountPrice.toLocaleString() : item.price.toLocaleString()}원
                            </p>
                            {item.discountPrice && (
                                <p className="text-xs text-gray-400 line-through decoration-gray-300">
                                    {item.price.toLocaleString()}원
                                </p>
                            )}
                        </div>
                    </div>
                </div>
            ))}
            </div>
        )}

        {/* 페이지네이션 */}
        {totalPages > 1 && (
            <div className="mt-16 flex justify-center items-center gap-2">
                <button onClick={() => handlePageChange(page - 1)} disabled={page === 0} className="p-2 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                    <ChevronLeft className="w-5 h-5 text-gray-600" />
                </button>
                <div className="flex items-center gap-1 px-2">
                    {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                        let pageNum = page - 2 + i;
                        if (page < 2) pageNum = i;
                        if (page > totalPages - 3) pageNum = totalPages - 5 + i;
                        if (pageNum < 0 || pageNum >= totalPages) return null;
                        return (
                            <button key={pageNum} onClick={() => handlePageChange(pageNum)} className={`w-8 h-8 rounded-lg text-sm font-medium transition-all ${page === pageNum ? "bg-black text-white shadow-md scale-105" : "text-gray-500 hover:bg-gray-100"}`}>
                                {pageNum + 1}
                            </button>
                        );
                    })}
                </div>
                <button onClick={() => handlePageChange(page + 1)} disabled={page === totalPages - 1} className="p-2 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                    <ChevronRight className="w-5 h-5 text-gray-600" />
                </button>
            </div>
        )}
      </main>
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