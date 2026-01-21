"use client";

import { fetchApi } from "@/lib/api";
import React, { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { toast } from "react-hot-toast"; // ★ 토스트 임포트 확인

const DEFAULT_IMAGE = "/no_image.jpg"; 

// 카테고리 한글 매핑
const categoryMap: Record<string, string> = {
  ELECTRONICS: "전자기기",
  CLOTHING: "의류",
  HOME: "가전/생활",
  BOOKS: "도서",
  BEAUTY: "뷰티/화장품",
  OTHERS: "기타",
};

// 아이콘 컴포넌트
const StarIcon = ({ filled, className }: { filled?: boolean; className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill={filled ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
  </svg>
);

const EyeIcon = ({ className }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
);

interface Item {
  id: number;
  name: string;
  price: number;
  discountPrice?: number;
  quantity: number;
  stockStatus: string;
  category: string; 
  description: string;
  thumbnailUrl?: string; 
  images: string[]; 
  status: string;
  sellerNickname?: string;
  createdAt: string;
  updatedAt: string;
  averageRating?: number;
  reviewCount?: number;
  viewCount?: number;
}

interface Review {
  reviewId: number;
  nickname: string;
  rating: number;
  content: string;
  imageUrls: string[];
  createdAt: string;
  isOwner: boolean;
}

interface ApiResponse<T> {
  data: T;
}

interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  last: boolean;
}

export default function ItemDetailPage() {
  const router = useRouter();
  const params = useParams();
  const [item, setItem] = useState<Item | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState<string>("");
  const [activeTab, setActiveTab] = useState<"detail" | "reviews">("detail");
  const [reviews, setReviews] = useState<Review[]>([]);

  useEffect(() => {
    if (!params.itemId) return;

    const fetchData = async () => {
      try {
        setLoading(true);
        const itemRes = await fetchApi<ApiResponse<Item>>(`/items/${params.itemId}`);
        const itemData = itemRes.data;
        setItem(itemData);

        if (itemData.thumbnailUrl) {
            setSelectedImage(itemData.thumbnailUrl);
        } else if (itemData.images && itemData.images.length > 0) {
            setSelectedImage(itemData.images[0]);
        } else {
            setSelectedImage(DEFAULT_IMAGE);
        }

        try {
          const reviewRes = await fetchApi<ApiResponse<PageResponse<Review>>>(`/reviews/items/${params.itemId}`);
          if (reviewRes.data && Array.isArray(reviewRes.data.content)) {
            setReviews(reviewRes.data.content);
          } else {
            setReviews([]);
          }
        } catch (reviewErr) {
          console.error("리뷰 로드 실패:", reviewErr);
          setReviews([]);
        }

      } catch (err) {
        console.error("상품 로드 실패:", err);
        setItem(null);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [params.itemId]);

  const moveToReviews = () => {
    setActiveTab("reviews");
    setTimeout(() => {
      const section = document.getElementById("detail-section");
      if (section) {
        const headerOffset = 100;
        const elementPosition = section.getBoundingClientRect().top;
        const offsetPosition = elementPosition + window.scrollY - headerOffset;
        window.scrollTo({ top: offsetPosition, behavior: "smooth" });
      }
    }, 100);
  };

  /** 장바구니 추가 로직 수정 */
  const handleAddToCart = async () => {
    if (!item) return;
    
    // 1. 즉시 로딩 토스트 시작
    const toastId = toast.loading("장바구니에 담는 중...");

    try {
      await fetchApi("/cart/add", {
        method: "POST",
        credentials: "include",
        body: JSON.stringify({ itemId: item.id, quantity: 1 }),
      });

      // 2. 성공 시 기존 토스트 업데이트
      toast.success("장바구니에 추가되었습니다! 🛒", { id: toastId });
      
      // 장바구니 숫자 갱신을 위한 커스텀 이벤트
      window.dispatchEvent(new Event("cart-updated"));
    } catch (err) {
      // 3. 실패 시 에러 메시지로 업데이트
      console.error(err);
      toast.error("장바구니 추가에 실패했습니다.", { id: toastId });
    }
  };

  const handleBuyNow = () => {
    if (!item) {
      toast.error("상품 정보를 불러오는 중입니다.");
      return;
    }
    const finalPrice = item.discountPrice ? item.discountPrice : item.price;

    const checkoutData = {
      type: "SINGLE",
      itemOrders: [{ 
        itemId: item.id, 
        quantity: 1,
        itemName: item.name,
        price: finalPrice,
        imageUrl: selectedImage || DEFAULT_IMAGE
      }],
      addressId: null, 
      zipCode: "",
      roadAddress: "",
      detailAddress: "",
      recipientName: "",
      recipientPhone: "",
    };

    sessionStorage.setItem("checkoutData", JSON.stringify(checkoutData));
    router.push("/payments");
  };

  if (loading) return (
    <div className="flex justify-center items-center min-h-screen bg-white">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900"></div>
    </div>
  );
  
  if (!item) return <div className="flex justify-center items-center min-h-[60vh]">아이템을 찾을 수 없습니다.</div>;

  const discountRate = item.discountPrice 
    ? Math.round(((item.price - item.discountPrice) / item.price) * 100) 
    : 0;

  const rating = item.averageRating || 0;
  const reviewCount = item.reviewCount || 0;
  const viewCount = item.viewCount || 0;

  return (
    <div className="min-h-screen bg-white pb-20">
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        
        {/* === 상단 섹션 === */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 mb-20">
          
          {/* [Left] 이미지 */}
          <div className="space-y-4">
            <div className="aspect-square bg-gray-50 rounded-2xl overflow-hidden shadow-sm relative group border border-gray-100">
              <img
                src={selectedImage || DEFAULT_IMAGE}
                alt={item.name}
                onError={(e) => { e.currentTarget.src = DEFAULT_IMAGE; }}
                className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-500"
              />
            </div>
            {item.images && item.images.length > 1 && (
              <div className="grid grid-cols-5 gap-3">
                {item.images.map((img, idx) => (
                  <button
                    key={idx}
                    onClick={() => setSelectedImage(img)}
                    className={`aspect-square rounded-xl overflow-hidden border-2 transition-all ${
                      selectedImage === img 
                        ? "border-black ring-1 ring-black/10" 
                        : "border-transparent hover:border-gray-200"
                    }`}
                  >
                    <img src={img} alt={`Thumb ${idx}`} className="w-full h-full object-cover" onError={(e) => { e.currentTarget.src = DEFAULT_IMAGE; }} />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* [Right] 정보 */}
          <div className="flex flex-col pt-2">
            <div className="flex items-center justify-between text-sm text-gray-500 mb-4">
              <div className="flex items-center gap-2">
                <span className="font-bold text-gray-900">{item.sellerNickname || "Official Store"}</span>
                <span className="w-1 h-1 bg-gray-300 rounded-full"></span>
                <span className="font-medium text-gray-700">
                  {categoryMap[item.category] || item.category || "기타"}
                </span>
              </div>
              <div className="flex items-center gap-1.5 text-gray-400">
                <EyeIcon className="w-4 h-4" />
                <span>{viewCount.toLocaleString()} viewed</span>
              </div>
            </div>

            <h1 className="text-3xl sm:text-4xl font-black text-gray-900 tracking-tight leading-tight mb-4">
              {item.name}
            </h1>

            <div className="flex items-center gap-2 mb-8 cursor-pointer hover:opacity-70 transition-opacity" onClick={moveToReviews}>
              <div className="flex text-yellow-400">
                {[...Array(5)].map((_, i) => (
                  <StarIcon key={i} filled={i < Math.round(rating)} className="w-5 h-5" />
                ))}
              </div>
              <span className="font-bold text-gray-900 text-lg">{rating.toFixed(1)}</span>
              <span className="text-gray-400 text-sm underline decoration-gray-300 underline-offset-4">
                {reviewCount}개의 리뷰 보기
              </span>
            </div>

            <div className="mb-8 p-6 bg-gray-50 rounded-2xl border border-gray-100">
              {item.discountPrice ? (
                <div className="flex items-end gap-3">
                  <span className="text-3xl font-black text-red-600">{discountRate}%</span>
                  <span className="text-4xl font-black text-gray-900">{item.discountPrice.toLocaleString()}원</span>
                  <span className="text-lg text-gray-400 line-through mb-1 font-medium">{item.price.toLocaleString()}원</span>
                </div>
              ) : (
                <div className="text-4xl font-black text-gray-900">{item.price.toLocaleString()}원</div>
              )}
            </div>

            <div className="flex-1"></div>

            <div className="flex gap-3 mt-8">
              <button
                onClick={handleAddToCart}
                disabled={item.stockStatus === "OUT_OF_STOCK"}
                className="flex-1 py-4 px-6 rounded-xl border-2 border-gray-200 text-gray-800 font-bold text-lg hover:bg-gray-50 hover:border-gray-300 transition active:scale-[0.98] disabled:opacity-50"
              >
                장바구니
              </button>
              <button
                onClick={handleBuyNow}
                disabled={item.stockStatus === "OUT_OF_STOCK"}
                className="flex-[2] py-4 px-6 rounded-xl bg-black text-white font-bold text-lg shadow-xl hover:bg-gray-800 transition active:scale-[0.98] disabled:bg-gray-400"
              >
                {item.stockStatus === "OUT_OF_STOCK" ? "품절된 상품입니다" : "구매하기"}
              </button>
            </div>
          </div>
        </div>

        {/* === 하단 섹션 === */}
        <div id="detail-section"> 
          <div className="flex border-b border-gray-200 mb-10 sticky top-[72px] bg-white z-10">
            <button
              onClick={() => setActiveTab("detail")}
              className={`flex-1 py-4 text-lg font-bold text-center transition-colors border-b-2 ${
                activeTab === "detail" ? "border-black text-black" : "border-transparent text-gray-400 hover:text-gray-600"
              }`}
            >
              상품 상세
            </button>
            <button
              onClick={moveToReviews} 
              className={`flex-1 py-4 text-lg font-bold text-center transition-colors border-b-2 ${
                activeTab === "reviews" ? "border-black text-black" : "border-transparent text-gray-400 hover:text-gray-600"
              }`}
            >
              리뷰 ({reviewCount})
            </button>
          </div>

          <div className="min-h-[400px]">
            {activeTab === "detail" && (
              <div className="prose prose-lg max-w-none text-gray-600 whitespace-pre-line leading-relaxed animate-in fade-in duration-300">
                {item.description}
              </div>
            )}

            {activeTab === "reviews" && (
              <div className="animate-in fade-in duration-300">
                <div className="bg-gray-50 rounded-2xl p-8 mb-10 flex flex-col md:flex-row items-center justify-center gap-8 md:gap-16 border border-gray-100">
                  <div className="text-center">
                    <div className="text-5xl font-black text-gray-900 mb-2">{rating.toFixed(1)}</div>
                    <div className="flex justify-center text-yellow-400 mb-2">
                      {[...Array(5)].map((_, i) => (
                        <StarIcon key={i} filled={i < Math.round(rating)} className="w-6 h-6" />
                      ))}
                    </div>
                    <div className="text-gray-500 font-medium">총 {reviewCount}개의 리뷰</div>
                  </div>
                  <div className="w-px h-24 bg-gray-200 hidden md:block"></div>
                  <div className="text-center md:text-left">
                    <p className="font-bold text-gray-800 text-lg mb-2">구매자들의 생생한 후기</p>
                    <p className="text-gray-500">실제 구매 고객님들이 작성해주신 소중한 리뷰입니다.</p>
                  </div>
                </div>

                {reviews.length > 0 ? (
                  <div className="space-y-6">
                    {reviews.map((review) => (
                      <div key={review.reviewId} className="border-b border-gray-100 pb-8 last:border-0">
                        <div className="flex justify-between items-start mb-3">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center font-bold text-gray-400 text-sm">
                              {review.nickname ? review.nickname[0] : "?"}
                            </div>
                            <div>
                              <p className="font-bold text-gray-900">{review.nickname}</p>
                              <div className="flex items-center gap-2 text-xs text-gray-400 mt-0.5">
                                <span>{new Date(review.createdAt).toLocaleDateString()}</span>
                                <span>·</span>
                                <span>구매확정</span>
                              </div>
                            </div>
                          </div>
                          <div className="flex text-yellow-400">
                            {[...Array(5)].map((_, i) => (
                              <StarIcon key={i} filled={i < review.rating} className="w-4 h-4" />
                            ))}
                          </div>
                        </div>
                        <p className="text-gray-700 leading-relaxed pl-[52px] whitespace-pre-line">
                          {review.content}
                        </p>
                        {review.imageUrls && review.imageUrls.length > 0 && (
                          <div className="flex gap-2 mt-4 pl-[52px]">
                            {review.imageUrls.map((imgUrl, idx) => (
                              <img key={idx} src={imgUrl} alt="Review" className="w-20 h-20 object-cover rounded-lg border border-gray-100" />
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-20 text-gray-400 bg-gray-50 rounded-2xl border border-dashed border-gray-200">
                    <p className="text-lg font-medium text-gray-600">아직 작성된 리뷰가 없습니다.</p>
                    <p className="text-sm mt-2">첫 번째 리뷰의 주인공이 되어보세요!</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

      </main>
    </div>
  );
}