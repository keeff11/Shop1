"use client";

/**
 * * 소셜 로그인 이후 추가 정보를 입력받는 페이지
 * useSearchParams()로 인한 빌드 에러 방지를 위해 내용을 AdditionalInfoContent로 분리하고
 * Suspense 경계를 설정함
 */

import { fetchApi } from "@/lib/api";
import { useSearchParams } from "next/navigation";
import { useState, Suspense } from "react"; // Suspense 추가
import DaumPostcode from "react-daum-postcode";

interface NicknameCheckApiResponse {
  data: boolean;
}

/**
 * * 실제 폼 로직과 상태를 관리하는 내부 컴포넌트
 */
function AdditionalInfoContent() {
  const searchParams = useSearchParams();

  const provider = searchParams.get("provider"); // kakao / naver
  const signUpToken =
    provider === "kakao"
      ? searchParams.get("kakaoToken")
      : searchParams.get("naverToken");

  const [userRole, setUserRole] = useState("CUSTOMER");
  const [nickname, setNickname] = useState("");
  const [loading, setLoading] = useState(false);
  const [isNicknameChecked, setIsNicknameChecked] = useState(false);

  const [zipCode, setZipCode] = useState("");
  const [roadAddress, setRoadAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  const [showPostcode, setShowPostcode] = useState(false);

  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNickname(e.target.value);
    setIsNicknameChecked(false);
  };

  const checkNickname = async () => {
    if (!nickname.trim()) return alert("닉네임을 입력해주세요.");
    try {
      const data = await fetchApi<NicknameCheckApiResponse>(`/auth/check-nickname?nickname=${nickname}`);
      if (data.data === true) {
        alert("이미 사용 중인 닉네임입니다.");
        setIsNicknameChecked(false);
      } else {
        alert("사용 가능한 닉네임입니다.");
        setIsNicknameChecked(true);
      }
    } catch (err) {
      console.error(err);
      alert("닉네임 중복 확인 중 오류가 발생했습니다.");
    }
  };

  const handleAddressComplete = (data: any) => {
    setZipCode(data.zonecode);
    setRoadAddress(data.roadAddress);
    setShowPostcode(false);
  };

  const handleSubmit = async () => {
    if (!signUpToken || !provider) {
      alert("유효하지 않은 접근입니다.");
      return;
    }
    if (!isNicknameChecked) {
      alert("닉네임 중복 확인을 해주세요.");
      return;
    }
    if (!zipCode || !roadAddress || !detailAddress) {
      alert("주소를 모두 입력해주세요.");
      return;
    }

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
      alert("회원가입이 완료되었습니다!");
      window.location.href = "/home";
    } catch (err: any) {
      console.error(err);
      alert(err.message || "추가 정보 입력 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md">
      <h1 className="text-2xl font-bold mb-6 text-center">추가 정보 입력</h1>
      
      {/* 닉네임 입력부 */}
      <div className="mb-4">
        <label className="block mb-1 font-medium text-sm">닉네임</label>
        <div className="flex gap-2">
          <input
            className="flex-1 border px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-ring"
            value={nickname}
            onChange={handleNicknameChange}
            placeholder="닉네임"
          />
          <button 
            type="button" 
            onClick={checkNickname}
            className={`px-3 py-2 text-sm rounded-lg border transition-colors ${
              isNicknameChecked 
                ? 'bg-green-50 text-green-700 border-green-200 cursor-default' 
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200 border-gray-300'
            }`}
            disabled={isNicknameChecked}
          >
            {isNicknameChecked ? "확인완료" : "중복확인"}
          </button>
        </div>
      </div>

      {/* 역할 선택부 */}
      <div className="mb-4">
        <label className="block mb-1 font-medium text-sm">역할</label>
        <select
          className="w-full border px-3 py-2 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-ring"
          value={userRole}
          onChange={(e) => setUserRole(e.target.value)}
        >
          <option value="CUSTOMER">CUSTOMER (구매자)</option>
          <option value="SELLER">SELLER (판매자)</option>
        </select>
      </div>

      {/* 주소 입력부 */}
      <div className="mb-4">
        <label className="block mb-1 font-medium text-sm">배송지 주소</label>
        <div className="flex gap-2 mb-2">
          <input value={zipCode} readOnly placeholder="우편번호" className="w-1/3 border px-3 py-2 rounded-lg bg-gray-100" />
          <button
            type="button"
            onClick={() => setShowPostcode(true)}
            className="flex-1 border px-3 py-2 rounded-lg bg-secondary text-primary hover:bg-secondary/80 font-medium text-sm"
          >
            주소 검색
          </button>
        </div>
        {showPostcode && (
          <div className="mb-3 border rounded-lg overflow-hidden bg-gray-50 p-2 relative">
            <button type="button" onClick={() => setShowPostcode(false)} className="absolute top-2 right-2 text-gray-500 hover:text-black font-bold z-10">✕</button>
            <div className="pt-6">
              <DaumPostcode onComplete={handleAddressComplete} style={{ height: '400px' }} />
            </div>
          </div>
        )}
        <input className="w-full border px-3 py-2 mb-2 rounded-lg bg-gray-100" value={roadAddress} readOnly placeholder="도로명 주소" />
        <input className="w-full border px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-ring" value={detailAddress} onChange={(e) => setDetailAddress(e.target.value)} placeholder="상세 주소" />
      </div>

      {/* 수령인 정보 */}
      <div className="grid grid-cols-2 gap-3 mb-6">
        <div>
          <label className="block mb-1 font-medium text-sm">수령인 이름</label>
          <input className="w-full border px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-ring" value={recipientName} onChange={(e) => setRecipientName(e.target.value)} />
        </div>
        <div>
          <label className="block mb-1 font-medium text-sm">연락처</label>
          <input className="w-full border px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-ring" value={recipientPhone} onChange={(e) => setRecipientPhone(e.target.value)} placeholder="010-1234-5678" />
        </div>
      </div>

      <button
        onClick={handleSubmit}
        disabled={loading}
        className="w-full bg-primary text-primary-foreground py-3 rounded-lg font-bold text-lg hover:bg-primary/90 transition disabled:bg-gray-400 disabled:cursor-not-allowed shadow-md"
      >
        {loading ? "처리중..." : "가입 완료"}
      </button>
    </div>
  );
}

/**
 * * 메인 페이지 컴포넌트 (빌드 에러 방지를 위한 Suspense 적용)
 */
export default function AdditionalInfoPage() {
  return (
    <Suspense fallback={<div className="flex justify-center mt-20">입력 양식을 불러오는 중...</div>}>
      <AdditionalInfoContent />
    </Suspense>
  );
}