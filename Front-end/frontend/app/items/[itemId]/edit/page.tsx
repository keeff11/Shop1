/**
 * * 상품 수정(Update) 페이지
 * 실무 가이드: 
 * 1. 기존 데이터를 API로 로드하여 폼 초기값(pre-fill)으로 설정함
 * 2. 신규 이미지 업로드 시에만 FormData에 추가하여 S3 자원 효율화
 * 3. Sonner 알림창을 통한 직관적인 처리 결과 제공
 * */
"use client";

import { fetchApi } from "@/lib/api";
import { useState, useEffect, useRef } from "react";
import { useRouter, useParams } from "next/navigation";
import { toast } from "sonner"; // 기존 alert 대신 sonner 사용 제안

export default function EditItemPage() {
  const router = useRouter();
  const params = useParams();
  const itemId = params.itemId; // URL 파라미터에서 itemId 추출

  // 상태 관리 (생성 페이지 양식 유지)
  const [name, setName] = useState("");
  const [price, setPrice] = useState<number | "">("");
  const [quantity, setQuantity] = useState<number | "">("");
  const [category, setCategory] = useState("ELECTRONICS");
  const [description, setDescription] = useState("");
  
  // 이미지 상태
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);
  const [existingImages, setExistingImages] = useState<string[]>([]); // 기존 이미지 저장용
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchItemData = async () => {
      try {
        const res: any = await fetchApi(`/items/${itemId}`);
        if (res.data) {
          const item = res.data;
          setName(item.name);
          setPrice(item.price);
          setQuantity(item.quantity);
          setCategory(item.itemCategory || "ELECTRONICS");
          setDescription(item.description);
          setExistingImages(item.images || []); // 기존 이미지 경로들
        }
      } catch (error) {
        console.error("데이터 로드 실패:", error);
        toast.error("상품 정보를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };
    if (itemId) fetchItemData();
  }, [itemId]);

  // 이미지 선택 핸들러
  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const filesArray = Array.from(e.target.files);
      setSelectedFiles((prev) => [...prev, ...filesArray]);
      setExistingImages([]); 
    }
  };

  const removeImage = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  // 미리보기 생성
  useEffect(() => {
    const urls = selectedFiles.map((file) => URL.createObjectURL(file));
    setPreviewUrls(urls);
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, [selectedFiles]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const formData = new FormData();
    const itemData = { name, price: Number(price), quantity: Number(quantity), category, description };
    
    formData.append(
      "request",
      new Blob([JSON.stringify(itemData)], { type: "application/json" })
    );

    // 새 이미지가 있을 때만 전송
    selectedFiles.forEach((file) => {
      formData.append("images", file);
    });

    try {
      await fetchApi(`/items/${itemId}`, {
        method: "PUT",
        body: formData,
      });
      
      toast.success("상품이 성공적으로 수정되었습니다.");
      router.push(`/items/${itemId}`);
    } catch (error) {
      console.error(error);
      toast.error("수정 중 오류가 발생했습니다.");
    }
  };

  if (loading) return <div className="text-center py-20 text-gray-400">정보를 불러오는 중...</div>;

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight">상품 정보 수정</h1>
          <p className="mt-2 text-sm text-gray-600">수정할 내용을 입력하신 후 완료 버튼을 눌러주세요.</p>
        </div>

        <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-xl overflow-hidden border border-gray-100">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 p-8">
            
            {/* 기본 정보 */}
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">상품명</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:ring-2 focus:ring-blue-500 outline-none transition"
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">가격 (원)</label>
                  <input
                    type="number"
                    value={price}
                    onChange={(e) => setPrice(Number(e.target.value))}
                    className="w-full px-4 py-3 rounded-lg border border-gray-200 outline-none transition"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">재고 수량</label>
                  <input
                    type="number"
                    value={quantity}
                    onChange={(e) => setQuantity(Number(e.target.value))}
                    className="w-full px-4 py-3 rounded-lg border border-gray-200 outline-none transition"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">카테고리</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-4 py-3 rounded-lg border border-gray-200 outline-none bg-white transition"
                >
                  <option value="ELECTRONICS">💻 전자기기</option>
                  <option value="CLOTHING">👕 의류</option>
                  <option value="HOME">🏠 가전/생활</option>
                  <option value="BOOKS">📚 도서</option>
                  <option value="BEAUTY">💄 뷰티/화장품</option>
                  <option value="OTHERS">📦 기타</option>
                </select>
              </div>
            </div>

            {/* 상세 정보 & 이미지 */}
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">상품 설명</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-4 py-3 rounded-lg border border-gray-200 outline-none h-32 resize-none transition"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  이미지 수정 (새 파일을 선택하면 기존 이미지가 대체됩니다)
                </label>
                
                <div 
                  onClick={() => fileInputRef.current?.click()}
                  className="border-2 border-dashed border-gray-200 rounded-lg p-6 flex flex-col items-center justify-center cursor-pointer hover:bg-gray-50 hover:border-blue-500 transition"
                >
                  <span className="text-sm text-gray-400 font-medium">📷 클릭하여 사진 교체</span>
                  <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    accept="image/*"
                    onChange={handleImageChange}
                    className="hidden"
                  />
                </div>

                {/* 미리보기 및 기존 이미지 */}
                <div className="grid grid-cols-4 gap-3 mt-4">
                  {/* 새 이미지 미리보기 */}
                  {previewUrls.map((url, idx) => (
                    <div key={idx} className="relative aspect-square">
                      <img src={url} className="w-full h-full object-cover rounded-lg border border-blue-500" />
                      <button type="button" onClick={() => removeImage(idx)} className="absolute -top-1 -right-1 bg-red-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-[10px]">✕</button>
                    </div>
                  ))}
                  {/* 기존 이미지가 있고 새 파일을 선택 안 했을 때 표시 */}
                  {previewUrls.length === 0 && existingImages.map((img, idx) => (
                    <div key={idx} className="relative aspect-square opacity-70">
                      <img src={img} className="w-full h-full object-cover rounded-lg border border-gray-200" />
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>

          <div className="px-8 py-6 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
            <button
              type="button"
              onClick={() => router.back()}
              className="px-6 py-2.5 rounded-lg border border-gray-300 text-gray-600 hover:bg-white transition"
            >
              취소
            </button>
            <button
              type="submit"
              className="px-8 py-2.5 rounded-lg bg-blue-600 text-white font-bold hover:bg-blue-700 shadow-lg shadow-blue-100 transition active:scale-95"
            >
              수정 완료하기
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}