"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { fetchApi } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";
import { CheckCircle2, XCircle } from "lucide-react"; // 아이콘 추가 (선택사항)

export default function MyPageEdit() {
  const router = useRouter();
  const [nickname, setNickname] = useState("");
  const [originalNickname, setOriginalNickname] = useState("");
  const [profileImg, setProfileImg] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>("/default_profile.png");
  
  const [loading, setLoading] = useState(false);
  const [isChecked, setIsChecked] = useState(true);

  // 1. 기존 유저 정보 불러오기
  useEffect(() => {
    fetchApi<any>("/user/me")
      .then((response) => {
        const userData = response.data || response; 
        setNickname(userData.nickname);
        setOriginalNickname(userData.nickname);
        if (userData.profileImg) setPreviewUrl(userData.profileImg);
      })
      .catch(() => toast.error("사용자 정보를 불러오는데 실패했습니다."));
  }, []);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setProfileImg(file);
      setPreviewUrl(URL.createObjectURL(file));
    }
  };

  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setNickname(value);

    if (value === originalNickname) {
      setIsChecked(true);
    } else {
      setIsChecked(false);
    }
  };

  const checkNickname = async () => {
    if (!nickname.trim()) {
      toast.error("닉네임을 입력해주세요.");
      return;
    }

    if (nickname === originalNickname) {
      toast.success("현재 사용 중인 닉네임입니다.");
      setIsChecked(true);
      return;
    }

    try {
      const response = await fetchApi<any>(`/auth/check-nickname?nickname=${nickname}`);
      const isDuplicate = response.data;

      if (isDuplicate) {
        toast.error("이미 사용 중인 닉네임입니다.");
        setIsChecked(false);
      } else {
        toast.success("사용 가능한 닉네임입니다.");
        setIsChecked(true);
      }
    } catch (error) {
      console.error(error);
      toast.error("중복 확인 중 오류가 발생했습니다.");
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isChecked) {
      toast.error("닉네임 중복 확인을 해주세요.");
      return;
    }

    setLoading(true);

    const formData = new FormData();
    formData.append("nickname", nickname);
    if (profileImg) {
      formData.append("profileImg", profileImg);
    }

    try {
      await fetchApi("/user/profile", {
        method: "PUT",
        body: formData,
      });
      toast.success("프로필이 수정되었습니다.");
      router.push("/mypage");
      router.refresh();
    } catch (error: any) {
      toast.error(error.message || "수정 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 헬퍼: 입력창 테두리 색상 결정
  const getInputBorderColor = () => {
    if (nickname === originalNickname) return ""; // 변경 없음
    if (isChecked) return "border-green-500 focus-visible:ring-green-500"; // 확인 완료 (초록)
    return "border-red-500 focus-visible:ring-red-500"; // 미확인 (빨강)
  };

  return (
    <div className="container mx-auto py-10 max-w-lg">
      <Card>
        <CardHeader>
          <CardTitle>프로필 수정</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="flex flex-col items-center space-y-4">
              <img
                src={previewUrl}
                alt="Profile Preview"
                className="w-32 h-32 rounded-full object-cover border"
              />
              <div className="grid w-full max-w-sm items-center gap-1.5">
                <Label htmlFor="picture">프로필 사진</Label>
                <Input id="picture" type="file" accept="image/*" onChange={handleImageChange} />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="nickname">닉네임</Label>
              <div className="flex gap-2">
                <Input
                  id="nickname"
                  value={nickname}
                  onChange={handleNicknameChange}
                  required
                  className={getInputBorderColor()} 
                />
                <Button 
                  type="button" 
                  variant="secondary" 
                  onClick={checkNickname}
                  disabled={nickname === originalNickname || !nickname.trim()}
                >
                  중복확인
                </Button>
              </div>
              
              {/* [수정] 상태 메시지 표시 영역 */}
              {nickname !== originalNickname && (
                <div className="flex items-center gap-1 mt-1">
                  {isChecked ? (
                    <>
                      <CheckCircle2 className="w-4 h-4 text-green-600" />
                      <p className="text-sm text-green-600">사용 가능한 닉네임입니다.</p>
                    </>
                  ) : (
                    <>
                      <XCircle className="w-4 h-4 text-red-500" />
                      <p className="text-sm text-red-500">중복 확인이 필요합니다.</p>
                    </>
                  )}
                </div>
              )}
            </div>

            <div className="flex gap-4">
              <Button type="submit" className="flex-1" disabled={loading}>
                {loading ? "저장 중..." : "수정 완료"}
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