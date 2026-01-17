/**
 * * 소셜 로그인(Kakao/Naver) 이후 부족한 배송지 및 닉네임 정보를 입력받는 페이지
 * 실무 가이드: 
 * 1. 일반 회원가입과 동일한 디자인 시스템 및 유효성 검사 로직(Validation) 적용
 * 2. 모든 필드 입력 완료 전까지 버튼 비활성화(Constraint)
 * 3. 가입 완료 시 'auth-change' 이벤트를 통해 즉시 로그인 UI 반영
 * */
"use client";

import { fetchApi } from "@/lib/api";
import { useSearchParams } from "next/navigation";
import { useState, Suspense, useEffect } from "react";
import DaumPostcode from "react-daum-postcode";
import { toast } from "sonner";

function AdditionalInfoContent() {
  const searchParams = useSearchParams();

  // 소셜 인증 정보 추출
  const provider = searchParams.get("provider"); 
  const signUpToken = provider === "kakao" ? searchParams.get("kakaoToken") : searchParams.get("naverToken");

  // ===== 상태 관리 =====
  const [userRole, setUserRole] = useState("CUSTOMER");
  const [nickname, setNickname] = useState("");
  const [loading, setLoading] = useState(false);
  const [isNicknameChecked, setIsNicknameChecked] = useState(false);

  // ===== 주소 정보 =====
  const [zipCode, setZipCode] = useState("");
  const [roadAddress, setRoadAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  const [showPostcode, setShowPostcode] = useState(false);

  const isFormFilled = 
    nickname.trim() !== "" && 
    zipCode !== "" && 
    roadAddress !== "" && 
    detailAddress.trim() !== "" && 
    recipientName.trim() !== "" && 
    recipientPhone.trim() !== "";

  const isFormValid = isFormFilled && isNicknameChecked && !loading;

  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNickname(e.target.value);
    setIsNicknameChecked(false);
  };

  /** 닉네임 중복 체크 */
  const checkNickname = async () => {
    if (!nickname.trim()) return toast.error("닉네임을 입력해주세요.");
    try {
      const res: any = await fetchApi(`/auth/check-nickname?nickname=${encodeURIComponent(nickname)}`);
      if (res.data === false) {
        setIsNicknameChecked(true);
        toast.success("사용 가능한 닉네임입니다.");
      } else {
        toast.error("이미 사용 중인 닉네임입니다.");
      }
    } catch (err) {
      toast.error("닉네임 확인 중 오류가 발생했습니다.");
    }
  };

  const handleAddressComplete = (data: any) => {
    setZipCode(data.zonecode);
    setRoadAddress(data.roadAddress);
    setShowPostcode(false);
  };

  /** 회원가입 최종 제출 */
  const handleSubmit = async () => {
    if (!signUpToken || !provider) return toast.error("유효하지 않은 접근입니다.");
    
    if (!isNicknameChecked) return toast.error("닉네임 중복 확인을 해주세요.");
    if (!zipCode) return toast.error("주소 검색을 해주세요.");
    if (!detailAddress.trim()) return toast.error("상세 주소를 입력해주세요.");
    if (!recipientName.trim()) return toast.error("수령인 성함을 입력해주세요.");
    if (!recipientPhone.trim()) return toast.error("수령인 연락처를 입력해주세요.");

    setLoading(true);
    try {
      const url = `/auth/${provider}/login`;
      await fetchApi(url, {
        method: "POST",
        headers: { Authorization: signUpToken },
        credentials: "include",
        body: JSON.stringify({
          userRole,
          nickname,
          zipCode,
          roadAddress,
          detailAddress,
          recipientName,
          recipientPhone,
        }),
      });

      toast.success("회원가입이 완료되었습니다! 자동으로 로그인됩니다.");
      
      // 헤더 UI 즉시 갱신
      window.dispatchEvent(new Event("auth-change"));

      setTimeout(() => {
        window.location.href = "/home";
      }, 1000);
    } catch (err: any) {
      toast.error(err.message || "추가 정보 입력 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-10 px-4">
      <div className="w-full max-w-md p-8 bg-white rounded-xl shadow-lg border border-gray-100">
        <h1 className="text-2xl font-bold text-center mb-8 text-gray-800">소셜 계정 추가 정보</h1>
        
        <div className="space-y-5">
          {/* 닉네임 영역 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-gray-600">닉네임</label>
            <div className="flex gap-2">
              <input
                type="text"
                value={nickname}
                onChange={handleNicknameChange}
                placeholder="사용할 닉네임"
                className="flex-1 px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary outline-none transition"
              />
              <button 
                type="button" 
                onClick={checkNickname}
                className="px-3 py-2 text-xs font-bold rounded-lg border bg-gray-50 hover:bg-gray-100 disabled:bg-blue-50 disabled:text-blue-600 transition"
                disabled={isNicknameChecked}
              >
                {isNicknameChecked ? "확인완료" : "중복확인"}
              </button>
            </div>
          </div>

          {/* 역할 선택 영역 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-gray-600">회원 유형</label>
            <select
              value={userRole}
              onChange={(e) => setUserRole(e.target.value)}
              className="w-full px-4 py-2 border rounded-lg bg-white focus:ring-2 focus:ring-primary outline-none cursor-pointer"
            >
              <option value="CUSTOMER">일반 구매자 (Customer)</option>
              <option value="SELLER">판매자 (Seller)</option>
            </select>
          </div>

          {/* 주소 정보 영역 */}
          <div className="space-y-2 pt-2 border-t">
            <label className="text-sm font-semibold text-gray-600">배송지 정보</label>
            <div className="flex gap-2">
              <input value={zipCode} readOnly className="w-1/3 px-3 py-2 border rounded-lg bg-gray-50 text-gray-500 text-sm" placeholder="우편번호"/>
              <button type="button" onClick={() => setShowPostcode(true)} className="flex-1 bg-gray-800 text-white text-sm font-bold rounded-lg hover:bg-gray-700 transition">주소 검색</button>
            </div>
            {showPostcode && (
              <div className="border rounded-lg p-2 relative shadow-sm z-10 bg-white">
                <button type="button" onClick={() => setShowPostcode(false)} className="absolute top-2 right-2 font-bold p-1 hover:text-red-500 transition">✕</button>
                <div className="pt-6"><DaumPostcode onComplete={handleAddressComplete} style={{ height: '400px' }} /></div>
              </div>
            )}
            <input value={roadAddress} readOnly className="w-full px-4 py-2 border rounded-lg bg-gray-50 text-gray-500 text-sm" placeholder="도로명 주소"/>
            <input value={detailAddress} onChange={(e) => setDetailAddress(e.target.value)} className="w-full px-4 py-2 border rounded-lg outline-none focus:ring-2 focus:ring-primary" placeholder="상세 주소를 입력하세요"/>
            <div className="grid grid-cols-2 gap-2">
              <input value={recipientName} onChange={(e) => setRecipientName(e.target.value)} className="w-full px-4 py-2 border rounded-lg outline-none focus:ring-2 focus:ring-primary text-sm" placeholder="수령인 성함"/>
              <input value={recipientPhone} onChange={(e) => setRecipientPhone(e.target.value)} className="w-full px-4 py-2 border rounded-lg outline-none focus:ring-2 focus:ring-primary text-sm" placeholder="수령인 연락처"/>
            </div>
          </div>

          <button
            onClick={handleSubmit}
            disabled={!isFormValid}
            className={`w-full py-4 rounded-xl font-bold text-lg transition shadow-lg mt-6 ${
              isFormValid 
                ? "bg-primary text-primary-foreground hover:bg-primary/90 shadow-primary/20" 
                : "bg-gray-300 text-gray-500 cursor-not-allowed shadow-none"
            }`}
          >
            {loading ? "처리 중..." : "가입 및 로그인 완료"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function AdditionalInfoPage() {
  return (
    <Suspense fallback={<div className="flex justify-center mt-20 text-gray-500 animate-pulse">양식을 불러오는 중입니다...</div>}>
      <AdditionalInfoContent />
    </Suspense>
  );
}