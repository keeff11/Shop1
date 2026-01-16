"use client";

import { fetchApi } from "@/lib/api";
import React, { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";

// ✅ 기본 이미지 설정
const DEFAULT_IMAGE = "/no_image.jpg"; 

interface Item {
  id: number;
  name: string;
  price: number;
  discountPrice?: number;
  quantity: number;
  stockStatus: string;
  itemCategory?: string;
  description: string;
  // 백엔드 ItemResponseDTO에 추가된 필드
  thumbnailUrl?: string; 
  // 상세 조회이므로 images 배열 전체가 올 것으로 예상 (객체 배열 {imageUrl, sortOrder} 형태일 수 있음)
  // 현재 프론트엔드 코드에 맞춰 string[] 또는 {imageUrl: string}[] 등 유연하게 처리 필요
  // 여기선 기존 코드(string[])를 유지하되, 필요 시 {imageUrl: string}[]로 수정하세요.
  images: string[]; 
  status: string;
  sellerNickname?: string;
  createdAt: string;
  updatedAt: string;
}

interface ApiResponse {
  data: Item;
}

export default function ItemDetailPage() {
  const router = useRouter();
  const params = useParams();
  const [item, setItem] = useState<Item | null>(null);
  const [loading, setLoading] = useState(true);
  
  // 현재 선택된 이미지 상태
  const [selectedImage, setSelectedImage] = useState<string>("");

  useEffect(() => {
    const fetchItem = async () => {
      try {
        const data = await fetchApi<ApiResponse>(`/items/${params.itemId}`);
        const itemData = data.data;
        setItem(itemData);

        // ✅ [이미지 초기 설정 로직 개선]
        // 1. thumbnailUrl이 있으면 그걸로 설정
        // 2. 없으면 images 배열의 첫 번째 이미지
        // 3. 그것도 없으면 DEFAULT_IMAGE
        if (itemData.thumbnailUrl) {
            setSelectedImage(itemData.thumbnailUrl);
        } else if (itemData.images && itemData.images.length > 0) {
            setSelectedImage(itemData.images[0]);
        } else {
            setSelectedImage(DEFAULT_IMAGE);
        }

      } catch (err) {
        console.error(err);
        setItem(null);
      } finally {
        setLoading(false);
      }
    };
    fetchItem();
  }, [params.itemId]);

  /** 장바구니 추가 */
  const handleAddToCart = async () => {
    if (!item) return;

    try {
      await fetchApi("/cart/add", {
        method: "POST",
        credentials: "include",
        body: JSON.stringify({ itemId: item.id, quantity: 1 }),
      });

      alert("장바구니에 추가되었습니다!");
    } catch (err) {
      console.error(err);
      alert("장바구니 추가 중 오류가 발생했습니다.");
    }
  };

  /** 단건 구매 → 결제 확인 페이지 */
  const handleBuyNow = () => {
    if (!item) return alert("아이템 정보를 불러오는 중입니다. 잠시만 기다려주세요.");

    const checkoutData = {
      type: "SINGLE",
      itemOrders: [{ itemId: item.id, quantity: 1 }],
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

  if (loading) return <div className="flex justify-center items-center min-h-[60vh]">로딩중...</div>;
  if (!item) return <div className="flex justify-center items-center min-h-[60vh]">아이템을 찾을 수 없습니다.</div>;

  // 할인율 계산
  const discountRate = item.discountPrice 
    ? Math.round(((item.price - item.discountPrice) / item.price) * 100) 
    : 0;

  return (
    <div className="min-h-screen bg-white">
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        
        {/* 상단: 이미지 & 상품 정보 그리드 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-10 lg:gap-16">
          
          {/* [Left] 이미지 갤러리 */}
          <div className="space-y-4">
            {/* 메인 이미지 */}
            <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden shadow-sm relative group">
              <img
                src={selectedImage || DEFAULT_IMAGE}
                alt={item.name}
                onError={(e) => {
                  // 이미지 로드 실패 시(엑박) 기본 이미지로 교체
                  e.currentTarget.src = DEFAULT_IMAGE;
                }}
                className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-500"
              />
            </div>
            
            {/* 썸네일 리스트 (이미지가 2개 이상일 때만 표시) */}
            {item.images && item.images.length > 1 && (
              <div className="grid grid-cols-5 gap-2">
                {item.images.map((img, idx) => (
                  <button
                    key={idx}
                    onClick={() => setSelectedImage(img)}
                    className={`aspect-square rounded-lg overflow-hidden border-2 transition-all ${
                      selectedImage === img 
                        ? "border-black ring-1 ring-black/20" 
                        : "border-transparent hover:border-gray-300"
                    }`}
                  >
                    <img 
                      src={img} 
                      alt={`썸네일 ${idx}`} 
                      className="w-full h-full object-cover"
                      onError={(e) => { e.currentTarget.src = DEFAULT_IMAGE; }}
                    />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* [Right] 상품 정보 */}
          <div className="flex flex-col justify-center">
            
            {/* 판매자 및 메타 정보 */}
            <div className="flex items-center gap-2 text-sm text-gray-500 mb-4">
              <span className="font-medium text-gray-900">{item.sellerNickname || "판매자"}</span>
              <span className="w-1 h-1 bg-gray-300 rounded-full"></span>
              <span>{new Date(item.createdAt).toLocaleDateString()} 등록</span>
              {item.stockStatus === "OUT_OF_STOCK" && (
                 <span className="ml-auto px-2 py-0.5 bg-red-100 text-red-600 text-xs font-bold rounded">품절</span>
              )}
            </div>

            {/* 상품명 */}
            <h1 className="text-3xl sm:text-4xl font-bold text-gray-900 tracking-tight mb-6">
              {item.name}
            </h1>

            {/* 가격 섹션 */}
            <div className="mb-8 p-6 bg-gray-50 rounded-lg">
              {item.discountPrice ? (
                <div className="flex items-end gap-3">
                  <span className="text-3xl font-bold text-red-600">{discountRate}%</span>
                  <span className="text-4xl font-bold text-gray-900">
                    {item.discountPrice.toLocaleString()}원
                  </span>
                  <span className="text-lg text-gray-400 line-through mb-1">
                    {item.price.toLocaleString()}원
                  </span>
                </div>
              ) : (
                <div className="text-4xl font-bold text-gray-900">
                  {item.price.toLocaleString()}원
                </div>
              )}
            </div>

            <div className="h-px bg-gray-200 mb-8"></div>

            {/* 버튼 그룹 */}
            <div className="flex gap-4">
              <button
                onClick={handleAddToCart}
                disabled={item.stockStatus === "OUT_OF_STOCK"}
                className="flex-1 py-4 px-6 rounded-lg border border-gray-300 text-gray-700 font-bold text-lg hover:bg-gray-50 transition active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
              >
                장바구니 담기
              </button>
              <button
                onClick={handleBuyNow}
                disabled={item.stockStatus === "OUT_OF_STOCK"}
                className="flex-1 py-4 px-6 rounded-lg bg-primary text-primary-foreground font-bold text-lg shadow-lg hover:bg-primary/90 transition active:scale-[0.98] disabled:bg-gray-400 disabled:shadow-none disabled:cursor-not-allowed"
              >
                바로 구매하기
              </button>
            </div>
          </div>
        </div>

        {/* 하단: 상품 상세 설명 */}
        <div className="mt-16 lg:mt-24">
          <h2 className="text-2xl font-bold text-gray-900 mb-6 border-b pb-4">상품 상세 정보</h2>
          <div className="prose prose-lg max-w-none text-gray-600 whitespace-pre-line leading-relaxed bg-white">
            {item.description}
          </div>
        </div>

      </main>
    </div>
  );
}