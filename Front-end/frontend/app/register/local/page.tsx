"use client";

import { fetchApi } from "@/lib/api";
import React, { useState, useEffect } from "react";
// import { useRouter } from "next/navigation"; 
import DaumPostcode from "react-daum-postcode";

export default function RegisterPage() {
  // ===== 계정 정보 =====
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [role, setRole] = useState("CUSTOMER");

  // ===== 비밀번호 유효성 상태 =====
  const [passwordMessage, setPasswordMessage] = useState("");
  const [isPasswordSafe, setIsPasswordSafe] = useState(false);

  // ===== 이메일 인증 & 중복 확인 상태 =====
  const [emailCode, setEmailCode] = useState("");
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [isNicknameChecked, setIsNicknameChecked] = useState(false);

  // ===== 주소 정보 =====
  const [zipCode, setZipCode] = useState("");
  const [roadAddress, setRoadAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [recipientName, setRecipientName] = useState("");
  const [recipientPhone, setRecipientPhone] = useState("");
  const [showPostcode, setShowPostcode] = useState(false);

  // ----------------------------------------------------
  // 🔐 비밀번호 유효성 검사 함수
  // ----------------------------------------------------
  useEffect(() => {
    if (!password) {
      setPasswordMessage("");
      setIsPasswordSafe(false);
      return;
    }

    // 규칙: 영문, 숫자, 특수문자(!@#$%^&*) 포함 8~20자
    const passwordRegex = /^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,20}$/;

    if (!passwordRegex.test(password)) {
      setPasswordMessage("영문, 숫자, 특수문자(!@#$%^&*) 포함 8자 이상이어야 합니다.");
      setIsPasswordSafe(false);
    } else {
      setPasswordMessage("안전한 비밀번호입니다.");
      setIsPasswordSafe(true);
    }
  }, [password]);

  // ... (기존 이메일, 닉네임, 주소 핸들러들은 그대로 유지) ...
  const handleEmailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setEmail(e.target.value);
    setIsEmailVerified(false);
    setIsCodeSent(false);
  };
  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNickname(e.target.value);
    setIsNicknameChecked(false);
  };
  const sendVerificationCode = async () => { /* ... 기존 코드 ... */ };
  const verifyEmailCode = async () => { /* ... 기존 코드 ... */ };
  const checkNickname = async () => { /* ... 기존 코드 ... */ };
  const handleAddressComplete = (data: any) => { /* ... 기존 코드 ... */ };

  // ----------------------------------------------------
  // 핸들러: 회원가입 제출
  // ----------------------------------------------------
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isEmailVerified) return alert("이메일 인증을 완료해주세요.");
    
    // [추가] 비밀번호 안전성 체크
    if (!isPasswordSafe) return alert("비밀번호 규칙을 확인해주세요.");

    if (!isNicknameChecked) return alert("닉네임 중복 확인을 해주세요.");
    if (!zipCode) return alert("주소를 입력해주세요.");

    const formData = {
      email, password, nickname, userRole: role,
      zipCode, roadAddress, detailAddress, recipientName, recipientPhone,
    };

    try {
      await fetchApi("/auth/local/sign-up", {
        method: "POST",
        credentials: "include",
        body: JSON.stringify(formData),
      });

      alert("회원가입 성공! 홈으로 이동합니다.");
      window.location.href = "/home";

    } catch (error: any) {
      console.error("회원가입 에러:", error);
      alert(error.message || "오류 발생");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-10">
      <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-lg">
        <h1 className="text-2xl font-bold text-center mb-6">Shop1 회원가입</h1>

        <form className="space-y-4" onSubmit={handleSubmit}>
          
          {/* 이메일 (기존 코드 유지) */}
          <div>
            <label className="block text-sm font-medium mb-1">이메일</label>
            <div className="flex gap-2">
              <input
                type="email"
                value={email}
                onChange={handleEmailChange}
                placeholder="example@gmail.com"
                className="flex-1 px-4 py-2 border rounded-lg"
                readOnly={isEmailVerified}
              />
              <button 
                type="button" 
                onClick={sendVerificationCode}
                className="px-3 py-2 text-sm rounded-lg border bg-secondary text-primary"
                disabled={isEmailVerified}
              >
                {isEmailVerified ? "인증완료" : "인증번호 전송"}
              </button>
            </div>
            {isCodeSent && !isEmailVerified && (
                <div className="flex gap-2 mt-2">
                    <input
                        type="text"
                        value={emailCode}
                        onChange={(e) => setEmailCode(e.target.value)}
                        placeholder="인증번호 6자리"
                        className="flex-1 px-4 py-2 border rounded-lg"
                    />
                    <button type="button" onClick={verifyEmailCode} className="px-3 py-2 text-sm border bg-gray-100">확인</button>
                </div>
            )}
          </div>

          {/* 🔐 비밀번호 (수정된 부분) */}
          <div>
            <label className="block text-sm font-medium mb-1">비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="영문, 숫자, 특수문자 포함 8자 이상"
              className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                password.length > 0 
                  ? (isPasswordSafe ? "border-green-500 focus:ring-green-500" : "border-red-500 focus:ring-red-500") 
                  : "border-gray-300 focus:ring-ring"
              }`}
              required
            />
            {/* 비밀번호 피드백 메시지 */}
            {password.length > 0 && (
              <p className={`text-xs mt-1 ${isPasswordSafe ? "text-green-600" : "text-red-500"}`}>
                {passwordMessage}
              </p>
            )}
          </div>

          {/* 닉네임 */}
          <div>
            <label className="block text-sm font-medium mb-1">닉네임</label>
            <div className="flex gap-2">
              <input
                type="text"
                value={nickname}
                onChange={handleNicknameChange}
                placeholder="닉네임"
                className="flex-1 px-4 py-2 border rounded-lg"
              />
              <button 
                type="button" 
                onClick={checkNickname}
                className="px-3 py-2 text-sm rounded-lg border bg-gray-100"
                disabled={isNicknameChecked}
              >
                {isNicknameChecked ? "확인완료" : "중복확인"}
              </button>
            </div>
          </div>

          {/* 역할 선택 */}
          <div>
            <label className="block text-sm font-medium mb-1">역할</label>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full px-4 py-2 border rounded-lg bg-white"
            >
              <option value="CUSTOMER">구매자</option>
              <option value="SELLER">판매자</option>
            </select>
          </div>

          {/* 주소 (기존 코드 유지) */}
          <div>
            <label className="block text-sm font-medium mb-1">주소</label>
            <div className="flex gap-2 mb-2">
              <input value={zipCode} readOnly className="w-1/3 px-3 py-2 border rounded-lg bg-gray-100" placeholder="우편번호"/>
              <button type="button" onClick={() => setShowPostcode(true)} className="flex-1 border px-3 py-2 rounded-lg bg-secondary text-primary">주소 검색</button>
            </div>
            {showPostcode && (
              <div className="mb-3 border rounded-lg p-2 relative">
                <button type="button" onClick={() => setShowPostcode(false)} className="absolute top-2 right-2 font-bold">✕</button>
                <div className="pt-6"><DaumPostcode onComplete={handleAddressComplete} style={{ height: '400px' }} /></div>
              </div>
            )}
            <input value={roadAddress} readOnly className="w-full px-4 py-2 border rounded-lg bg-gray-100 mb-2" placeholder="도로명 주소"/>
            <input value={detailAddress} onChange={(e) => setDetailAddress(e.target.value)} className="w-full px-4 py-2 border rounded-lg mb-2" placeholder="상세 주소"/>
            <div className="grid grid-cols-2 gap-2">
              <input value={recipientName} onChange={(e) => setRecipientName(e.target.value)} className="w-full px-4 py-2 border rounded-lg" placeholder="수령인"/>
              <input value={recipientPhone} onChange={(e) => setRecipientPhone(e.target.value)} className="w-full px-4 py-2 border rounded-lg" placeholder="연락처"/>
            </div>
          </div>

          <button
            type="submit"
            disabled={!isPasswordSafe || !isEmailVerified || !isNicknameChecked} // 안전하지 않으면 버튼 비활성화
            className="w-full bg-primary text-primary-foreground py-3 rounded-lg font-bold text-lg hover:bg-primary/90 transition disabled:bg-gray-400 disabled:cursor-not-allowed mt-4"
          >
            회원가입 완료
          </button>
        </form>
      </div>
    </div>
  );
}