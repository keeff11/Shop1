"use client";

import { useSearchParams, useRouter } from "next/navigation";
import { useState, useRef, useEffect } from "react";
import { fetchApi } from "@/lib/api"; // 원래 쓰시던 방식 그대로 사용

export default function ReviewPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  
  const orderItemId = searchParams.get("orderItemId");
  const itemName = searchParams.get("itemName");

  const [rating, setRating] = useState(5);
  const [content, setContent] = useState("");
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!orderItemId) {
      alert("잘못된 접근입니다.");
      router.back();
    }
  }, [orderItemId, router]);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    if (imageFiles.length + files.length > 5) {
      alert("이미지는 최대 5장까지 첨부 가능합니다.");
      return;
    }
    const newFiles = Array.from(files);
    const newPreviews = newFiles.map((file) => URL.createObjectURL(file));
    setImageFiles((prev) => [...prev, ...newFiles]);
    setPreviewUrls((prev) => [...prev, ...newPreviews]);
    e.target.value = "";
  };

  const handleRemoveImage = (index: number) => {
    setImageFiles((prev) => prev.filter((_, i) => i !== index));
    setPreviewUrls((prev) => {
      URL.revokeObjectURL(prev[index]);
      return prev.filter((_, i) => i !== index);
    });
  };

  const handleSubmit = async () => {
    if (content.length < 10) {
      alert("리뷰 내용을 10자 이상 작성해주세요.");
      return;
    }

    setLoading(true);
    try {
      const formData = new FormData();
      
      // 1. 텍스트 데이터 (Blob으로 감싸서 application/json 타입임을 명시)
      const reviewData = { rating, content };
      formData.append(
        "review",
        new Blob([JSON.stringify(reviewData)], { type: "application/json" })
      );

      // 2. 이미지 파일들 추가
      imageFiles.forEach((file) => {
        formData.append("images", file);
      });

      // ★ [핵심] 원래 쓰시던 fetchApi를 호출합니다.
      // 쿼리 파라미터로 orderItemId를 넘깁니다.
      await fetchApi(`/reviews?orderItemId=${orderItemId}`, {
        method: "POST",
        body: formData, // FormData를 그대로 전달
        // ★ [주의] fetchApi 내부 로직에서 body가 FormData인 경우 
        // headers['Content-Type']을 자동으로 삭제하거나 세팅하지 않도록 되어있어야 합니다.
        credentials: "include", 
      });

      alert("리뷰가 등록되었습니다!");
      router.back(); 
    } catch (err: any) {
      console.error("리뷰 등록 에러 상세:", err);
      // 만약 여기서 500이나 400 에러가 난다면 fetchApi가 Content-Type을 강제하고 있을 확률이 높습니다.
      alert(`등록 실패: ${err.message || "서버 오류"}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-10 px-4">
      <div className="max-w-2xl mx-auto bg-white rounded-2xl shadow-xl p-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-2 text-center">리뷰 작성</h1>
        <p className="text-gray-500 text-center mb-8">{itemName}</p>

        <div className="space-y-6">
          <div className="flex flex-col items-center">
            <span className="text-sm font-medium text-gray-600 mb-2">상품은 어떠셨나요?</span>
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((num) => (
                <button key={num} onClick={() => setRating(num)} className={`text-4xl transition-colors ${num <= rating ? "text-yellow-400" : "text-gray-200"}`}>★</button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">상세 리뷰</label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              className="w-full h-40 p-4 border border-gray-200 rounded-xl focus:ring-2 focus:ring-black outline-none resize-none text-gray-700 transition-all"
              placeholder="최소 10자 이상 작성해주세요."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">사진 첨부 ({imageFiles.length}/5)</label>
            <div className="flex gap-3 overflow-x-auto pb-2 min-h-[100px]">
              <div 
                onClick={() => fileInputRef.current?.click()}
                className="w-24 h-24 flex-shrink-0 border-2 border-dashed border-gray-300 rounded-xl flex flex-col items-center justify-center text-gray-400 cursor-pointer hover:bg-gray-50 hover:border-gray-400 transition-colors"
              >
                <span className="text-2xl">+</span>
              </div>
              <input type="file" ref={fileInputRef} className="hidden" accept="image/*" multiple onChange={handleImageChange} />
              {previewUrls.map((url, idx) => (
                <div key={idx} className="relative w-24 h-24 flex-shrink-0 group">
                  <img src={url} alt={`Preview ${idx}`} className="w-full h-full object-cover rounded-xl border border-gray-100" />
                  <button onClick={() => handleRemoveImage(idx)} className="absolute -top-2 -right-2 w-6 h-6 bg-red-500 text-white rounded-full flex items-center justify-center text-xs shadow-md opacity-0 group-hover:opacity-100 transition-opacity">✕</button>
                </div>
              ))}
            </div>
          </div>

          <button onClick={handleSubmit} disabled={loading} className="w-full py-4 bg-black text-white font-bold rounded-xl hover:bg-gray-800 transition shadow-lg disabled:bg-gray-300 active:scale-[0.98]">
            {loading ? "등록 중..." : "리뷰 등록하기"}
          </button>
        </div>
      </div>
    </div>
  );
}