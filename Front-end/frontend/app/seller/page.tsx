/**
 * * 판매자 전용 대시보드(Seller Dashboard) - 상품 등록 액션 추가
 * 실무 가이드: 
 * 1. 잦은 빈도의 액션(상품 등록)은 상단 헤더에 퀵 버튼으로 배치하여 접근성을 높임
 * 2. 각 관리 모듈의 카드 내에도 세부 액션 버튼을 유지함
 * */
"use client";

import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Package, Ticket, ArrowRight, Store, Plus } from "lucide-react";

export default function SellerDashboardPage() {
  const router = useRouter();

  const menuItems = [
    {
      title: "상품 관리",
      description: "등록한 상품 목록을 확인하고 수정하거나 삭제합니다.",
      icon: <Package className="w-6 h-6 text-blue-600" />,
      path: "/items/mine",
      buttonText: "목록 보기",
    },
    {
      title: "쿠폰 관리",
      description: "새로운 할인 쿠폰을 생성하고 발행 내역을 관리합니다.",
      icon: <Ticket className="w-6 h-6 text-purple-600" />,
      path: "/coupons/create",
      buttonText: "쿠폰 생성",
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50/50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto">
        
        {/* 상단 헤더 섹션: 상품 등록 버튼 추가 */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-10 gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-primary rounded-lg">
              <Store className="w-8 h-8 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-gray-900 tracking-tight">판매자 센터</h1>
              <p className="text-gray-500">비즈니스 성장을 위한 통합 관리 도구입니다.</p>
            </div>
          </div>
          
          {/* 🚀 퀵 액션: 상품 등록 버튼 */}
          <Button 
            size="lg" 
            onClick={() => router.push("/items/create")}
            className="shadow-md gap-2"
          >
            <Plus className="w-5 h-5" /> 새 상품 등록
          </Button>
        </div>

        {/* 메뉴 카드 그리드 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {menuItems.map((item, idx) => (
            <Card key={idx} className="group hover:border-primary/50 transition-all hover:shadow-lg">
              <CardHeader className="flex flex-row items-center gap-4 space-y-0">
                <div className="p-3 bg-gray-100 rounded-xl group-hover:bg-primary/5 transition-colors">
                  {item.icon}
                </div>
                <div>
                  <CardTitle className="text-xl font-bold">{item.title}</CardTitle>
                </div>
              </CardHeader>
              <CardContent>
                <CardDescription className="text-base mb-6 min-h-[48px]">
                  {item.description}
                </CardDescription>
                <Button 
                  onClick={() => router.push(item.path)}
                  variant="secondary"
                  className="w-full gap-2 group-hover:bg-primary group-hover:text-white transition-colors"
                >
                  {item.buttonText}
                  <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* 안내 배너 */}
        <div className="mt-12 p-6 bg-blue-50 border border-blue-100 rounded-2xl flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="text-blue-600 font-bold text-lg">💡</div>
            <p className="text-blue-800 text-sm">
              상품 등록 후 <strong>내 상품 관리</strong> 페이지에서 정상 노출 여부를 확인하실 수 있습니다.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}