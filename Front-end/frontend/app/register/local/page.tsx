/**
 * * 모든 입력 필드 및 배송지 정보 강제 검증이 적용된 회원가입 페이지
 * 실무 가이드: 
 * 1. 닉네임 확인뿐만 아니라 상세 주소, 수령인 등 배송 정보가 모두 채워져야 버튼 활성화
 * 2. .trim()을 사용하여 공백만 입력된 경우를 유효하지 않은 입력으로 간주
 * 3. 모든 인증(이메일, 비밀번호 일치, 닉네임 중복)과 입력이 완료되어야 가입 가능
 * */
"use client";

import { fetchApi } from "@/lib/api";
import React, { useState, useEffect } from "react";
import DaumPostcode from "react-daum-postcode";
import { toast } from "sonner";

export default function RegisterPage() {
  // ===== 계정 정보 =====
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [role, setRole] = useState("CUSTOMER");

  // ===== 상태 관리 =====
  const [passwordMessage, setPasswordMessage] = useState("");
  const [isPasswordSafe, setIsPasswordSafe] = useState(false);
  const [isPasswordMatch, setIsPasswordMatch] = useState(false);
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

  // 🔐 비밀번호 유효성 및 일치 검사
  useEffect(() => {
    if (!password) {
      setPasswordMessage("");
      setIsPasswordSafe(false);
    } else {
      const passwordRegex = /^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,20}$/;
      if (!passwordRegex.test(password)) {
        setPasswordMessage("영문, 숫자, 특수문자(!@#$%^&*) 포함 8자 이상이어야 합니다.");
        setIsPasswordSafe(false);
      } else {
        setPasswordMessage("안전한 비밀번호입니다.");
        setIsPasswordSafe(true);
      }
    }
    setIsPasswordMatch(password !== "" && password === confirmPassword);
  }, [password, confirmPassword]);

  // ✅ [핵심] 모든 칸이 실제로 입력되었는지 검사 (배송지 정보 포함)
  const isFormFilled = 
    email.trim() !== "" && 
    password !== "" && 
    confirmPassword !== "" && 
    nickname.trim() !== "" && 
    zipCode !== "" && 
    roadAddress !== "" && 
    detailAddress.trim() !== "" && 
    recipientName.trim() !== "" && 
    recipientPhone.trim() !== "";

  // ✅ [핵심] 모든 보안 인증 조건과 입력 조건이 완벽할 때만 활성화
  const isFormValid = 
    isFormFilled && 
    isPasswordSafe && 
    isPasswordMatch && 
    isEmailVerified && 
    isNicknameChecked;

  // 📧 이메일 핸들러
  const handleEmailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setEmail(e.target.value);
    setIsEmailVerified(false);
    setIsCodeSent(false);
  };

  const sendVerificationCode = async () => {
    if (!email) return toast.error("이메일을 입력해주세요.");
    try {
      await fetchApi(`/auth/email/send-code?email=${encodeURIComponent(email)}`, { method: "POST" });
      setIsCodeSent(true);
      toast.success("인증 번호가 발송되었습니다.");
    } catch (error: any) {
      toast.error(error.message || "발송 실패");
    }
  };

  const verifyEmailCode = async () => {
    if (!emailCode) return toast.error("인증번호를 입력해주세요.");
    try {
      const res: any = await fetchApi(`/auth/email/verify-code?email=${encodeURIComponent(email)}&code=${encodeURIComponent(emailCode)}`, { method: "POST" });
      if (res.data === true) {
        setIsEmailVerified(true);
        toast.success("이메일 인증 완료");
      } else {
        toast.error("인증 번호가 일치하지 않습니다.");
      }
    } catch (error) {
      toast.error("인증 처리 중 오류가 발생했습니다.");
    }
  };

  // 🏷️ 닉네임 핸들러
  const checkNickname = async () => {
    if (!nickname) return toast.error("닉네임을 입력해주세요.");
    try {
      const res: any = await fetchApi(`/auth/check-nickname?nickname=${encodeURIComponent(nickname)}`);
      if (res.data === false) {
        setIsNicknameChecked(true);
        toast.success("사용 가능한 닉네임입니다.");
      } else {
        toast.error("이미 사용 중인 닉네임입니다.");
      }
    } catch (error) {
      toast.error("중복 확인 중 오류 발생");
    }
  };

  // 🏠 주소 핸들러
  const handleAddressComplete = (data: any) => {
    setZipCode(data.zonecode);
    setRoadAddress(data.roadAddress);
    setShowPostcode(false);
  };

  /** 회원가입 제출 */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isFormValid) {
      toast.error("모든 필수 항목을 입력하고 인증을 완료해주세요.");
      return;
    }

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

      toast.success("가입 성공! 자동으로 로그인되었습니다.");
      window.dispatchEvent(new Event("auth-change"));
      setTimeout(() => { window.location.href = "/home"; }, 1000);
    } catch (error: any) {
      toast.error(error.message || "가입 오류 발생");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-10 px-4">
      <div className="w-full max-w-md p-8 bg-white rounded-xl shadow-lg border border-gray-100">
        <h1 className="text-2xl font-bold text-center mb-8 text-gray-800">Shop1 회원가입</h1>

        <form className="space-y-5" onSubmit={handleSubmit}>
          {/* 이메일 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-gray-600">이메일</label>
            <div className="flex gap-2">
              <input type="email" value={email} onChange={handleEmailChange} placeholder="example@gmail.com" className="flex-1 px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary outline-none transition" readOnly={isEmailVerified} />
              <button type="button" onClick={sendVerificationCode} className="px-3 py-2 text-xs font-bold rounded-lg border bg-gray-50 hover:bg-gray-100 disabled:bg-green-50 disabled:text-green-600 transition" disabled={isEmailVerified}>
                {isEmailVerified ? "인증완료" : (isCodeSent ? "재발송" : "번호전송")}
              </button>
            </div>
            {isCodeSent && !isEmailVerified && (
              <div className="flex gap-2 mt-2">
                <input type="text" value={emailCode} onChange={(e) => setEmailCode(e.target.value)} placeholder="인증번호 6자리" className="flex-1 px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary outline-none" />
                <button type="button" onClick={verifyEmailCode} className="px-4 py-2 text-sm font-bold bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition">확인</button>
              </div>
            )}
          </div>

          {/* 비밀번호 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-gray-600">비밀번호</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="영문, 숫자, 특수문자 포함 8~20자" className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 transition ${password.length > 0 ? (isPasswordSafe ? "border-green-500 focus:ring-green-500" : "border-red-500 focus:ring-red-500") : "border-gray-200 focus:ring-primary"}`} />
            {password.length > 0 && <p className={`text-[11px] font-medium mt-1 ${isPasswordSafe ? "text-green-600" : "text-red-500"}`}>{passwordMessage}</p>}
          </div>

          {/* 비밀번호 확인 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-gray-600">비밀번호 확인</label>
            <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} placeholder="비밀번호 재입력" className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 transition ${confirmPassword.length > 0 ? (isPasswordMatch ? "border-green-500 focus:ring-green-500" : "border-red-500 focus:ring-red-500") : "border-gray-200 focus:ring-primary"}`} />
            {confirmPassword.length > 0 && <p className={`text-[11px] font-medium mt-1 ${isPasswordMatch ? "text-green-600" : "text-red-500"}`}>{isPasswordMatch ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다."}</p>}
          </div>

          {/* 닉네임 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-gray-600">닉네임</label>
            <div className="flex gap-2">
              <input type="text" value={nickname} onChange={(e) => { setNickname(e.target.value); setIsNicknameChecked(false); }} placeholder="사용할 닉네임" className="flex-1 px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary outline-none" />
              <button type="button" onClick={checkNickname} className="px-3 py-2 text-xs font-bold rounded-lg border bg-gray-50 hover:bg-gray-100 disabled:bg-blue-50 disabled:text-blue-600 transition" disabled={isNicknameChecked}>
                {isNicknameChecked ? "확인완료" : "중복확인"}
              </button>
            </div>
          </div>

          {/* 역할 선택 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-gray-600">회원 유형</label>
            <select value={role} onChange={(e) => setRole(e.target.value)} className="w-full px-4 py-2 border rounded-lg bg-white focus:ring-2 focus:ring-primary outline-none cursor-pointer">
              <option value="CUSTOMER">일반 구매자 (Customer)</option>
              <option value="SELLER">판매자 (Seller)</option>
            </select>
          </div>

          {/* 주소 정보 영역 (필수 체크 대상) */}
          <div className="space-y-2 pt-2 border-t">
            <label className="text-sm font-semibold text-gray-600">배송지 정보</label>
            <div className="flex gap-2">
              <input value={zipCode} readOnly className="w-1/3 px-3 py-2 border rounded-lg bg-gray-50 text-gray-500 text-sm" placeholder="우편번호" />
              <button type="button" onClick={() => setShowPostcode(true)} className="flex-1 bg-gray-800 text-white text-sm font-bold rounded-lg hover:bg-gray-700 transition">주소 검색</button>
            </div>
            {showPostcode && (
              <div className="border rounded-lg p-2 relative shadow-sm z-10 bg-white">
                <button type="button" onClick={() => setShowPostcode(false)} className="absolute top-2 right-2 font-bold p-1 hover:text-red-500 transition">✕</button>
                <div className="pt-6"><DaumPostcode onComplete={handleAddressComplete} style={{ height: '400px' }} /></div>
              </div>
            )}
            <input value={roadAddress} readOnly className="w-full px-4 py-2 border rounded-lg bg-gray-50 text-gray-500 text-sm" placeholder="도로명 주소" />
            <input value={detailAddress} onChange={(e) => setDetailAddress(e.target.value)} className="w-full px-4 py-2 border rounded-lg outline-none focus:ring-2 focus:ring-primary" placeholder="상세 주소를 입력하세요" />
            <div className="grid grid-cols-2 gap-2">
              <input value={recipientName} onChange={(e) => setRecipientName(e.target.value)} className="w-full px-4 py-2 border rounded-lg outline-none focus:ring-2 focus:ring-primary text-sm" placeholder="수령인 성함" />
              <input value={recipientPhone} onChange={(e) => setRecipientPhone(e.target.value)} className="w-full px-4 py-2 border rounded-lg outline-none focus:ring-2 focus:ring-primary text-sm" placeholder="수령인 연락처" />
            </div>
          </div>

          <button
            type="submit"
            disabled={!isFormValid}
            className={`w-full py-4 rounded-xl font-bold text-lg transition shadow-lg ${
              isFormValid 
                ? "bg-primary text-primary-foreground hover:bg-primary/90 shadow-primary/20" 
                : "bg-gray-300 text-gray-500 cursor-not-allowed shadow-none"
            }`}
          >
            가입 및 로그인하기
          </button>
        </form>
      </div>
    </div>
  );
}