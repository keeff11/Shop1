"use client";

import { fetchApi } from "@/lib/api";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Edit3, Trash2, Package, Calendar, Plus, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

const DEFAULT_IMAGE = "/no_image.jpg";

interface ItemSummary {
  id: number;
  name: string;
  price: number;
  quantity: number;
  category: string;
  thumbnailUrl?: string;
  createdAt: string;
}

interface ApiResponse {
  success: boolean;
  data: ItemSummary[];
}

export default function MyItemsPage() {
  const router = useRouter();
  const [items, setItems] = useState<ItemSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchMyItems();
  }, []);

  /** 본인 등록 상품 조회 */
  const fetchMyItems = async () => {
    try {
      const res = await fetchApi<ApiResponse>("/api/items/me");
      if (res.data) {
        setItems(res.data);
      }
    } catch (err: any) {
      toast.error("상품 로드 실패", { description: err.message });
    } finally {
      setLoading(false);
    }
  };

  /** 상품 삭제 처리 */
  const handleDelete = async (itemId: number) => {
    if (!confirm("정말 이 상품을 삭제하시겠습니까? 관련 데이터와 이미지가 모두 삭제됩니다.")) return;

    try {
      await fetchApi(`/api/items/${itemId}`, { method: "DELETE" });
      toast.success("삭제 성공", { description: "상품이 정상적으로 제거되었습니다." });
      // UI에서 즉시 제거
      setItems(prev => prev.filter(item => item.id !== itemId));
    } catch (err: any) {
      toast.error("삭제 실패", { description: err.message });
    }
  };

  if (loading) return (
    <div className="flex justify-center items-center min-h-[60vh]">
      <div className="animate-pulse text-gray-400">관리 데이터를 불러오는 중...</div>
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-50/50">
      <main className="max-w-5xl mx-auto py-12 px-4 sm:px-6 lg:px-8">
        
        {/* 상단 헤더 */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-10 gap-4">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 tracking-tight">내 상품 관리</h1>
            <p className="text-gray-500 mt-1">판매 중인 상품의 재고와 정보를 실시간으로 관리하세요.</p>
          </div>
          <Button 
            onClick={() => router.push("/items/create")}
            className="bg-primary hover:bg-primary/90 shadow-sm gap-2 h-12 px-6"
          >
            <Plus className="w-5 h-5" /> 새 상품 등록
          </Button>
        </div>

        {/* 상품 리스트 영역 */}
        {items.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-24 bg-white rounded-2xl border border-dashed border-gray-200 shadow-sm">
            <Package className="w-16 h-16 text-gray-200 mb-4" />
            <p className="text-gray-500 text-lg">아직 등록된 상품이 없습니다.</p>
            <Button variant="link" onClick={() => router.push("/items/create")} className="mt-2">
              첫 상품 등록하기 →
            </Button>
          </div>
        ) : (
          <div className="grid gap-4">
            {items.map((item) => (
              <div
                key={item.id}
                className="bg-white border border-gray-100 rounded-xl shadow-sm overflow-hidden flex flex-col sm:flex-row items-center transition-all hover:shadow-md hover:border-gray-200"
              >
                {/* 썸네일 영역 */}
                <div className="w-full sm:w-32 h-32 bg-gray-50 shrink-0">
                  <img
                    src={item.thumbnailUrl || DEFAULT_IMAGE}
                    alt={item.name}
                    className="w-full h-full object-cover"
                    onError={(e) => { e.currentTarget.src = DEFAULT_IMAGE; }}
                  />
                </div>

                {/* 상품 정보 영역 */}
                <div className="p-6 flex-1 flex flex-col sm:flex-row justify-between items-center w-full gap-6">
                  <div className="flex-1 space-y-2 w-full text-center sm:text-left">
                    <div className="flex items-center gap-2 justify-center sm:justify-start">
                      <Badge variant="secondary" className="font-normal">{item.category}</Badge>
                      <h2 className="text-xl font-bold text-gray-900 line-clamp-1">{item.name}</h2>
                    </div>
                    
                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 justify-center sm:justify-start text-sm text-gray-500">
                      <span className="text-lg font-bold text-gray-900">{item.price.toLocaleString()}원</span>
                      <div className="flex items-center gap-1">
                        <Package className="w-4 h-4" /> 재고 {item.quantity}개
                      </div>
                      <div className="flex items-center gap-1">
                        <Calendar className="w-4 h-4" /> {new Date(item.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                  </div>

                  {/* 관리 버튼 세트 */}
                  <div className="flex items-center gap-2 w-full sm:w-auto">
                    <Button
                      variant="outline"
                      onClick={() => router.push(`/items/${item.id}/edit`)}
                      className="flex-1 sm:flex-none gap-2"
                    >
                      <Edit3 className="w-4 h-4" /> 수정
                    </Button>
                    <Button
                      variant="destructive"
                      onClick={() => handleDelete(item.id)}
                      className="flex-1 sm:flex-none gap-2"
                    >
                      <Trash2 className="w-4 h-4" /> 삭제
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}