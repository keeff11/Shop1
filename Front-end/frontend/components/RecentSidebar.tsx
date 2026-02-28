"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { fetchApi } from "@/lib/api";

interface ItemSummary {
  id: number;
  name: string;
  thumbnailUrl: string;
}

export default function RecentSidebar() {
  const [recentItems, setRecentItems] = useState<ItemSummary[]>([]);

  useEffect(() => {
    // 1. 디바이스 고유 Viewer-Id 생성 및 관리
    let viewerId = localStorage.getItem("viewerId");
    if (!viewerId) {
      viewerId = `guest-${Math.random().toString(36).substring(2, 15)}`;
      localStorage.setItem("viewerId", viewerId);
    }

    // 2. 최근 본 상품 불러오기
    const fetchRecentItems = async () => {
      try {
        const response: any = await fetchApi("/items/recent", {
          headers: { "Viewer-Id": viewerId as string },
        });
        setRecentItems(response.data || []);
      } catch (e) {
        console.error("최근 본 상품을 불러올 수 없습니다.", e);
      }
    };

    fetchRecentItems();

    // 사용자가 새로운 상품을 볼 때마다 사이드바를 갱신하기 위한 커스텀 이벤트 리스너
    window.addEventListener("updateRecentItems", fetchRecentItems);
    return () => window.removeEventListener("updateRecentItems", fetchRecentItems);
  }, []);

  // 🌟 최근 본 상품이 없을 때의 UI
  if (recentItems.length === 0) {
    return (
      <div className="fixed right-6 top-1/3 z-50 flex flex-col items-center bg-white border border-gray-200 rounded-2xl shadow-xl p-3 w-24">
        <div className="text-xs font-bold text-gray-500 mb-3 text-center w-full border-b border-gray-100 pb-2">
          최근 본 상품<br/><span className="text-gray-300">0</span>
        </div>
        <div className="flex flex-col items-center justify-center py-4 text-gray-300">
          {/* 눈 모양 또는 상품 없는 모양의 아이콘 */}
          <svg xmlns="http://www.w3.org/2000/svg" className="w-8 h-8 mb-2 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          <span className="text-[10px] text-center whitespace-nowrap">최근 본<br/>상품 없음</span>
        </div>
      </div>
    );
  }

  // 🌟 최근 본 상품이 있을 때의 기존 UI
  return (
    <div className="fixed right-6 top-1/3 z-50 flex flex-col items-center bg-white border border-gray-200 rounded-2xl shadow-xl p-3 w-24">
      <div className="text-xs font-bold text-gray-500 mb-3 text-center w-full border-b border-gray-100 pb-2">
        최근 본 상품<br/><span className="text-blue-500">{recentItems.length}</span>
      </div>
      
      <div className="flex flex-col gap-3 max-h-[400px] overflow-y-auto scrollbar-hide">
        {recentItems.map((item) => (
          <Link href={`/items/${item.id}`} key={item.id} className="group relative">
            <div className="w-16 h-16 rounded-xl overflow-hidden border border-gray-100 group-hover:border-blue-500 transition-colors">
              <img 
                src={item.thumbnailUrl} 
                alt={item.name} 
                className="w-full h-full object-cover group-hover:scale-110 transition-transform"
                onError={(e) => { e.currentTarget.src = '/no_image.jpg'; }}
              />
            </div>
            {/* 호버 시 상품명 툴팁 */}
            <div className="absolute right-full mr-3 top-1/2 -translate-y-1/2 w-32 bg-gray-900 text-white text-xs p-2 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50 line-clamp-2">
              {item.name}
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}