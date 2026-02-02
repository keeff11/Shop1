"use client";

import { fetchApi } from "@/lib/api";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Edit3, Trash2, Package, Calendar, Plus } from "lucide-react";
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

  const fetchMyItems = async () => {
    try {
      // 내 상품 조회
      const res = await fetchApi<ApiResponse>("/items/me", { credentials: "include" });
      if (res.data) setItems(res.data);
    } catch (err: any) {
      toast.error("상품 로드 실패", { description: err.message });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (itemId: number) => {
    if (!confirm("정말 이 상품을 삭제하시겠습니까?")) return;

    try {
      await fetchApi(`/items/${itemId}`, { method: "DELETE", credentials: "include" });
      toast.success("삭제 성공");
      
      // 목록에서 즉시 제거
      setItems(prev => prev.filter(item => item.id !== itemId));
      
    } catch (err: any) {
      toast.error("삭제 실패", { description: err.message });
    }
  };

  if (loading) return <div className="flex justify-center items-center min-h-[60vh]">Loading...</div>;

  return (
    <div className="min-h-screen bg-gray-50/50">
      <main className="max-w-5xl mx-auto py-12 px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center mb-10">
          <h1 className="text-3xl font-bold">내 상품 관리</h1>
          <Button onClick={() => router.push("/items/create")}><Plus className="w-5 h-5 mr-2" /> 새 상품 등록</Button>
        </div>

        {items.length === 0 ? (
          <div className="text-center py-24 bg-white rounded-2xl border border-dashed">등록된 상품이 없습니다.</div>
        ) : (
          <div className="grid gap-4">
            {items.map((item) => (
              <div key={item.id} className="bg-white border rounded-xl p-6 flex items-center gap-6">
                <img src={item.thumbnailUrl || DEFAULT_IMAGE} alt={item.name} className="w-24 h-24 object-cover rounded-lg bg-gray-100" />
                <div className="flex-1">
                  <Badge variant="secondary" className="mb-2">{item.category}</Badge>
                  <h2 className="text-xl font-bold">{item.name}</h2>
                  <div className="text-gray-500 text-sm mt-1">
                    {item.price.toLocaleString()}원 • 재고 {item.quantity}개
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" onClick={() => router.push(`/items/${item.id}/edit`)}>수정</Button>
                  <Button variant="destructive" onClick={() => handleDelete(item.id)}>삭제</Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}