"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import { fetchApi } from "../../lib/api";

interface ItemSummary {
  id: number;
  name: string;
  price: number;
  thumbnailUrl: string;
}

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
}

export default function HomePage() {
  const [popularItems, setPopularItems] = useState<ItemSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // 컴포넌트 마운트 시 실시간 랭킹 데이터 불러오기
  useEffect(() => {
    const fetchRanking = async () => {
      try {
        const response = await fetchApi<ApiResponse<ItemSummary[]>>("/items/ranking");
        setPopularItems(response.data || []);
      } catch (error) {
        console.error("실시간 랭킹을 불러오는데 실패했습니다.", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchRanking();
    
    // 1분(60초)마다 자동으로 랭킹 데이터를 갱신 (Polling)
    const intervalId = setInterval(fetchRanking, 60000);
    return () => clearInterval(intervalId);
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      
      {/* 메인 랭킹 섹션 (Hero 배너 대체) */}
      <main className="relative pt-12 pb-16 md:pt-20 md:pb-24 overflow-hidden bg-white border-b border-gray-100">
        
        {/* 배경 은은한 빛 장식 */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-full -z-10 pointer-events-none">
          <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[60%] bg-blue-100/50 rounded-full blur-[120px]"></div>
          <div className="absolute bottom-[10%] right-[-5%] w-[30%] h-[50%] bg-purple-100/40 rounded-full blur-[100px]"></div>
        </div>

        <div className="max-w-7xl mx-auto px-6">
          
          {/* 타이틀 및 Live 인디케이터 영역 */}
          <div className="flex flex-col md:flex-row md:items-end justify-between mb-10 gap-4">
            <div>
              <h1 className="text-4xl md:text-5xl font-extrabold text-gray-900 tracking-tight flex items-center gap-3">
                <span className="text-red-500 animate-bounce">🔥</span> 
                Shop1 실시간 인기 상품
              </h1>
              <p className="text-gray-500 mt-4 text-lg">
                지금 가장 많은 사람들이 주목하고 있는 Top 10 상품입니다.
              </p>
            </div>
            
            {/* Live 깜빡임 UI */}
            <div className="flex items-center gap-2 bg-gray-900 text-white px-4 py-2.5 rounded-full text-sm font-medium shadow-lg w-fit">
              <span className="relative flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-green-500"></span>
              </span>
              최근 1시간 조회수 기준 업데이트
            </div>
          </div>

          {/* 랭킹 그리드 영역 */}
          {isLoading ? (
            // 로딩 중 스켈레톤 UI
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4 md:gap-6">
              {[...Array(10)].map((_, i) => (
                <div key={i} className="animate-pulse flex flex-col gap-3">
                  <div className="bg-gray-200 aspect-square rounded-2xl"></div>
                  <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                  <div className="h-5 bg-gray-200 rounded w-1/2"></div>
                </div>
              ))}
            </div>
          ) : popularItems.length === 0 ? (
            // 데이터 없을 때
            <div className="flex flex-col items-center justify-center py-32 text-gray-400 bg-gray-50/50 rounded-3xl border border-gray-100">
              <span className="text-4xl mb-4">📭</span>
              <p className="text-lg font-medium">현재 집계된 인기 상품이 없습니다.</p>
            </div>
          ) : (
            // 데이터 렌더링
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4 md:gap-6">
              {popularItems.map((item, index) => (
                <Link href={`/items/${item.id}`} key={item.id} className="group cursor-pointer">
                  <div className="relative rounded-2xl overflow-hidden bg-white shadow-sm border border-gray-100 group-hover:shadow-2xl group-hover:-translate-y-2 transition-all duration-300 flex flex-col h-full">
                    
                    {/* 순위 뱃지 (1~3위 특별 디자인) */}
                    <div className={`absolute top-3 left-3 z-10 w-10 h-10 flex items-center justify-center rounded-2xl font-black text-lg shadow-lg 
                      ${index === 0 ? 'bg-gradient-to-br from-yellow-300 to-yellow-500 text-yellow-900 shadow-yellow-500/40' : 
                        index === 1 ? 'bg-gradient-to-br from-gray-200 to-gray-400 text-gray-800 shadow-gray-400/40' : 
                        index === 2 ? 'bg-gradient-to-br from-orange-300 to-orange-500 text-orange-950 shadow-orange-500/40' : 
                        'bg-gray-900/80 text-white backdrop-blur-md'}`}>
                      {index + 1}
                    </div>

                    {/* 썸네일 */}
                    <div className="aspect-square bg-gray-50 overflow-hidden relative">
                      <img 
                        src={item.thumbnailUrl} 
                        alt={item.name} 
                        className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-out"
                        onError={(e) => { e.currentTarget.src = '/no_image.jpg'; }}
                      />
                    </div>

                    {/* 상품 정보 */}
                    <div className="p-4 flex flex-col flex-grow justify-between">
                      <h3 className="text-sm md:text-base font-medium text-gray-700 line-clamp-2 mb-2 group-hover:text-blue-600 transition-colors">
                        {item.name}
                      </h3>
                      <p className="text-lg md:text-xl font-bold text-gray-900">
                        {item.price.toLocaleString()}원
                      </p>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
          
          {/* 전체 상품 보기 버튼 */}
          <div className="mt-12 flex justify-center">
            <Link
              href="/items"
              className="inline-flex items-center gap-2 px-8 py-4 bg-white text-gray-800 text-lg font-bold rounded-xl border border-gray-200 hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm"
            >
              Shop1 전체 상품 둘러보기 <span>→</span>
            </Link>
          </div>
        </div>
      </main>

      {/* 쇼핑몰 특징 안내 섹션 (카드형 디자인으로 개선) */}
      <section className="py-20 max-w-7xl mx-auto px-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-center">
          <div className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
            <div className="text-4xl mb-4">🚀</div>
            <h3 className="text-xl font-bold mb-3 text-gray-900">빠른 배송</h3>
            <p className="text-gray-500 text-sm leading-relaxed">주문 후 24시간 이내에<br/>발송을 원칙으로 합니다.</p>
          </div>
          <div className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
            <div className="text-4xl mb-4">💎</div>
            <h3 className="text-xl font-bold mb-3 text-gray-900">엄선된 품질</h3>
            <p className="text-gray-500 text-sm leading-relaxed">모든 제품은 전문가의 검수를<br/>거쳐 입고됩니다.</p>
          </div>
          <div className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
            <div className="text-4xl mb-4">🔒</div>
            <h3 className="text-xl font-bold mb-3 text-gray-900">안전한 결제</h3>
            <p className="text-gray-500 text-sm leading-relaxed">당신의 소중한 정보는<br/>강력하게 보호됩니다.</p>
          </div>
        </div>
      </section>

    </div>
  );
}