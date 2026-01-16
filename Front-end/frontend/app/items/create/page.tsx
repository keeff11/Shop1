"use client";

import { fetchApi } from "@/lib/api";
import { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";

interface CreateItemResponse {
  data: string; // Assuming the API returns the item ID as a string on success
}

interface ApiMessageResponse {
  message: string;
}

type ApiResponse = CreateItemResponse | ApiMessageResponse;

export default function CreateItemPage() {
  const router = useRouter();
  
  // 입력 상태
  const [name, setName] = useState("");
  const [price, setPrice] = useState<number | "">("");
  const [quantity, setQuantity] = useState<number | "">("");
  const [category, setCategory] = useState("ELECTRONICS");
  const [description, setDescription] = useState("");
  
  // 이미지 상태
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 이미지 파일 선택 핸들러 (추가 방식)
  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const filesArray = Array.from(e.target.files);
      setSelectedFiles((prev) => [...prev, ...filesArray]);
    }
  };

  // 이미지 삭제 핸들러
  const removeImage = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  // 미리보기 URL 생성 및 메모리 해제
  useEffect(() => {
    const urls = selectedFiles.map((file) => URL.createObjectURL(file));
    setPreviewUrls(urls);

    return () => {
      urls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [selectedFiles]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name || !price || !quantity) {
      alert("필수 정보를 모두 입력해주세요.");
      return;
    }

    const formData = new FormData();
    const itemData = { name, price: Number(price), quantity: Number(quantity), category, description };
    
    // JSON 데이터 추가
    formData.append(
      "request",
      new Blob([JSON.stringify(itemData)], { type: "application/json" })
    );

    // 이미지 파일 추가
    selectedFiles.forEach((file) => {
      formData.append("images", file);
    });

    try {
      const data = await fetchApi<ApiResponse>("/items", {
        method: "POST",
        credentials: "include",
        body: formData,
      });
      
      if ('data' in data) {
        alert("상품이 성공적으로 등록되었습니다!");
        router.push(`/items/${data.data}`);
      } else {
        alert(data.message || "상품 등록에 실패했습니다.");
      }
    } catch (error) {
      console.error(error);
      alert("서버 통신 중 오류가 발생했습니다.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        
        {/* 헤더 섹션 */}
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight">상품 등록</h1>
          <p className="mt-2 text-sm text-gray-600">
            판매할 상품의 정보를 입력해주세요.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-xl overflow-hidden">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 p-8">
            
            {/* [왼쪽 컬럼] 기본 정보 */}
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">상품명</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="예) 아이폰 15 Pro"
                  className="w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-ring focus:border-transparent outline-none transition"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">가격 (원)</label>
                  <input
                    type="number"
                    value={price}
                    onChange={(e) => setPrice(Number(e.target.value))}
                    placeholder="0"
                    className="w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-ring outline-none transition"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">재고 수량</label>
                  <input
                    type="number"
                    value={quantity}
                    onChange={(e) => setQuantity(Number(e.target.value))}
                    placeholder="1"
                    className="w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-ring outline-none transition"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">카테고리</label>
                <div className="relative">
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    className="w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-ring outline-none appearance-none bg-white transition"
                  >
                    <option value="ELECTRONICS">💻 전자기기</option>
                    <option value="CLOTHING">👕 의류</option>
                    <option value="HOME">🏠 가전/생활</option>
                    <option value="BOOKS">📚 도서</option>
                    <option value="BEAUTY">💄 뷰티/화장품</option>
                    <option value="OTHERS">📦 기타</option>
                  </select>
                  <div className="absolute inset-y-0 right-0 flex items-center px-4 pointer-events-none text-gray-500">
                    ▼
                  </div>
                </div>
              </div>
            </div>

            {/* [오른쪽 컬럼] 상세 정보 & 이미지 */}
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">상품 설명</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="상품의 특징, 상태 등을 자세히 적어주세요."
                  className="w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-ring outline-none h-32 resize-none transition"
                />
              </div>

              {/* 이미지 업로드 영역 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  상품 이미지 ({selectedFiles.length}개)
                </label>
                
                {/* 드래그 앤 드롭 영역 스타일 */}
                <div 
                  onClick={() => fileInputRef.current?.click()}
                  className="border-2 border-dashed border-gray-300 rounded-lg p-6 flex flex-col items-center justify-center cursor-pointer hover:bg-gray-50 hover:border-primary transition group"
                >
                  <div className="w-12 h-12 bg-secondary text-primary rounded-full flex items-center justify-center mb-3 group-hover:scale-110 transition">
                    📷
                  </div>
                  <span className="text-sm text-gray-500 font-medium">클릭하여 이미지 업로드</span>
                  <span className="text-xs text-gray-400 mt-1">JPG, PNG, GIF 지원</span>
                  <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    accept="image/*"
                    onChange={handleImageChange}
                    className="hidden"
                  />
                </div>

                {/* 미리보기 리스트 */}
                {previewUrls.length > 0 && (
                  <div className="grid grid-cols-4 gap-3 mt-4">
                    {previewUrls.map((url, idx) => (
                      <div key={idx} className="relative aspect-square group">
                        <img
                          src={url}
                          alt="preview"
                          className="w-full h-full object-cover rounded-lg border border-gray-200"
                        />
                        {/* 삭제 버튼 */}
                        <button
                          type="button"
                          onClick={() => removeImage(idx)}
                          className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full w-6 h-6 flex items-center justify-center text-xs shadow-md opacity-0 group-hover:opacity-100 transition transform hover:scale-110"
                        >
                          ✕
                        </button>
                        {idx === 0 && (
                          <div className="absolute bottom-0 left-0 right-0 bg-black/60 text-white text-[10px] text-center py-1 rounded-b-lg">
                            대표 이미지
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* 하단 버튼 */}
          <div className="px-8 py-6 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
            <button
              type="button"
              onClick={() => router.back()}
              className="px-6 py-2.5 rounded-lg border border-gray-300 text-gray-700 font-medium hover:bg-white transition"
            >
              취소
            </button>
            <button
              type="submit"
              className="px-8 py-2.5 rounded-lg bg-primary text-primary-foreground font-bold hover:bg-primary/90 shadow-lg shadow-blue-200 transition transform active:scale-95"
            >
              상품 등록하기
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}