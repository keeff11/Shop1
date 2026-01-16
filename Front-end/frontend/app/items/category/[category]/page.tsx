"use client";

import { fetchApi } from "@/lib/api";
import { useRouter, useParams } from "next/navigation";
import { useEffect, useState } from "react";

// ✅ 기본 이미지 설정 (public 폴더 내)
const DEFAULT_IMAGE = "/no_image.jpg";

interface Item {
  id: number;
  name: string;
  price: number;
  // 백엔드 ItemResponseDTO에 추가된 필드
  thumbnailUrl?: string;
  // images는 선택사항
  images?: { imageUrl: string }[];
}

interface ApiResponse {
  success: boolean;
  data: Item[];
}

export default function CategoryPage() {
  const router = useRouter();
  const params = useParams();
  const category = params.category; // URL의 [category] 파라미터

  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!category) return;

    const fetchItems = async () => {
      try {
        // category가 배열일 경우를 대비해 문자열로 변환 처리
        const categoryStr = Array.isArray(category) ? category[0] : category;
        
        // 백엔드 API 호출
        const res = await fetchApi<ApiResponse>(`/items/category/${categoryStr.toUpperCase()}`);
        
        // 응답 데이터 세팅
        if (res.success) {
            setItems(res.data as Item[]);
        }
      } catch (err) {
        console.error("카테고리 상품 로드 실패:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchItems();
  }, [category]);

  if (loading) return (
    <div className="flex justify-center items-center min-h-screen">
      <div className="text-gray-500">상품 목록을 불러오는 중...</div>
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-50">
      
      <main className="max-w-6xl mx-auto p-4 sm:p-6 lg:p-8">
        <h1 className="text-2xl font-bold mb-6 text-gray-800 capitalize">
          {category} 카테고리
        </h1>

        {items.length === 0 ? (
          <div className="text-center py-20 text-gray-500 bg-white rounded-lg shadow-sm">
            해당 카테고리에 등록된 상품이 없습니다.
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {items.map((item) => {
              // ✅ 1순위: thumbnailUrl, 2순위: images[0], 3순위: 기본 이미지
              const imageUrl = item.thumbnailUrl 
                ? item.thumbnailUrl 
                : (item.images && item.images.length > 0 ? item.images[0].imageUrl : DEFAULT_IMAGE);

              return (
                <div
                  key={item.id}
                  onClick={() => router.push(`/items/${item.id}`)}
                  className="bg-white border border-gray-100 rounded-lg shadow-sm hover:shadow-md hover:-translate-y-1 transition-all duration-300 cursor-pointer overflow-hidden group"
                >
                  {/* 이미지 영역 (비율 고정) */}
                  <div className="aspect-[4/3] overflow-hidden bg-gray-100 relative">
                    <img
                      src={imageUrl}
                      alt={item.name}
                      onError={(e) => {
                        // 엑박 발생 시 기본 이미지로 교체
                        e.currentTarget.src = DEFAULT_IMAGE;
                      }}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  </div>

                  {/* 텍스트 정보 영역 */}
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