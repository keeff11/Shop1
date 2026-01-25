"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useDaumPostcodePopup } from "react-daum-postcode"; // 1. 훅 임포트
import { fetchApi } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";

export default function CreateAddressPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  // 2. 다음 우편번호 팝업 훅 설정 (스크립트 URL은 카카오 공식 CDN 사용)
  const open = useDaumPostcodePopup("https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js");

  // 폼 상태 관리
  const [formData, setFormData] = useState({
    recipientName: "",
    recipientPhone: "",
    zipCode: "",
    roadAddress: "",
    detailAddress: "",
  });

  // 3. 주소 검색 완료 핸들러
  const handleComplete = (data: any) => {
    let fullAddress = data.address;
    let extraAddress = "";

    // 도로명 주소 선택 시 참고항목 조합 (법정동, 건물명 등)
    if (data.addressType === "R") {
      if (data.bname !== "") {
        extraAddress += data.bname;
      }
      if (data.buildingName !== "") {
        extraAddress += extraAddress !== "" ? `, ${data.buildingName}` : data.buildingName;
      }
      fullAddress += extraAddress !== "" ? ` (${extraAddress})` : "";
    }

    // 선택된 정보를 폼 상태에 반영
    setFormData((prev) => ({
      ...prev,
      zipCode: data.zonecode,
      roadAddress: fullAddress,
      // 상세주소는 초기화하거나 커서 포커스를 위해 비워둠
      detailAddress: "", 
    }));
  };

  // 4. 주소 찾기 버튼 클릭 핸들러
  const handleSearchClick = () => {
    open({ onComplete: handleComplete });
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setFormData((prev) => ({ ...prev, [id]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    // 유효성 검사
    if (!formData.zipCode || !formData.roadAddress) {
      toast.error("주소 정보를 입력해주세요.");
      setLoading(false);
      return;
    }

    try {
      await fetchApi("/user/addresses", {
        method: "POST",
        body: JSON.stringify(formData),
      });
      toast.success("배송지가 등록되었습니다.");
      router.push("/mypage"); 
      router.refresh();
    } catch (error: any) {
      toast.error(error.message || "배송지 등록에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mx-auto py-10 max-w-lg">
      <Card>
        <CardHeader>
          <CardTitle>새 배송지 등록</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* 수령인 정보 */}
            <div className="space-y-2">
              <Label htmlFor="recipientName">수령인 이름</Label>
              <Input
                id="recipientName"
                placeholder="홍길동"
                value={formData.recipientName}
                onChange={handleChange}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="recipientPhone">전화번호</Label>
              <Input
                id="recipientPhone"
                placeholder="010-1234-5678"
                value={formData.recipientPhone}
                onChange={handleChange}
                required
              />
            </div>

            {/* 주소 정보 */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="zipCode">우편번호</Label>
                <Input
                  id="zipCode"
                  placeholder="12345"
                  value={formData.zipCode}
                  readOnly // 직접 입력 방지 (검색 이용 유도)
                  required
                />
              </div>
              <div className="flex items-end">
                {/* 5. 버튼에 클릭 핸들러 연결 */}
                <Button 
                  type="button" 
                  variant="outline" 
                  className="w-full" 
                  onClick={handleSearchClick}
                >
                  우편번호 찾기
                </Button>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="roadAddress">도로명 주소</Label>
              <Input
                id="roadAddress"
                placeholder="서울시 강남구 테헤란로 123"
                value={formData.roadAddress}
                readOnly // 직접 입력 방지
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="detailAddress">상세 주소</Label>
              <Input
                id="detailAddress"
                placeholder="101동 101호"
                value={formData.detailAddress}
                onChange={handleChange}
                required
              />
            </div>

            <div className="flex gap-4 pt-4">
              <Button type="submit" className="flex-1" disabled={loading}>
                {loading ? "등록 중..." : "등록하기"}
              </Button>
              <Button type="button" variant="outline" onClick={() => router.back()}>
                취소
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}