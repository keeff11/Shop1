"use client";

import Link from "next/link";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-white">
      {/* --- 히어로 섹션 (Hero Section) --- */}
      <main className="relative overflow-hidden">
        {/* 배경 효과: 세련된 그라데이션 포인트 */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-full -z-10">
          <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[60%] bg-blue-100 rounded-full blur-[120px] opacity-50"></div>
          <div className="absolute bottom-[10%] right-[-5%] w-[30%] h-[50%] bg-purple-100 rounded-full blur-[100px] opacity-40"></div>
        </div>

        <section className="flex flex-col justify-center items-center h-[calc(100vh-120px)] px-6 text-center">
          {/* 배지 스타일 */}
          <span className="px-4 py-1.5 mb-6 text-sm font-semibold tracking-wide text-primary bg-primary/10 rounded-full animate-bounce">
            New Concept Shopping Mall
          </span>

          <h1 className="text-5xl md:text-6xl font-extrabold text-gray-900 leading-tight mb-6">
            모든 가치를 한 곳에, <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-indigo-500">
              Shop1
            </span>에서 시작하세요
          </h1>

          <p className="text-lg text-gray-600 max-w-2xl mb-10 leading-relaxed">
            엄선된 제품과 차별화된 경험을 제공합니다. <br className="hidden md:block" />
            지금 바로 Shop1의 다양한 카테고리를 탐험해보세요.
          </p>

          <div className="flex flex-col sm:flex-row gap-4">
            <Link
              href="/products" // 실제 상품 페이지 경로로 수정하세요
              className="px-8 py-4 bg-gray-900 text-white text-lg font-bold rounded-xl hover:bg-gray-800 transition-all shadow-xl hover:-translate-y-1"
            >
              전체 상품 보러가기
            </Link>
            <button className="px-8 py-4 bg-white text-gray-700 text-lg font-bold rounded-xl border border-gray-200 hover:bg-gray-50 transition-all shadow-sm">
              이벤트 확인하기
            </button>
          </div>

          {/* 하단 스크롤 유도 화살표 */}
          <div className="absolute bottom-10 animate-pulse text-gray-400">
            <p className="text-xs uppercase tracking-widest">Scroll Down</p>
          </div>
        </section>
      </main>

      {/* --- 서비스 특징 섹션 (간단 추가) --- */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-6xl mx-auto px-6 grid grid-cols-1 md:grid-cols-3 gap-12 text-center">
          <div>
            <div className="text-3xl mb-4">🚀</div>
            <h3 className="text-xl font-bold mb-2">빠른 배송</h3>
            <p className="text-gray-500 text-sm">주문 후 24시간 이내에 발송을 원칙으로 합니다.</p>
          </div>
          <div>
            <div className="text-3xl mb-4">💎</div>
            <h3 className="text-xl font-bold mb-2">엄선된 품질</h3>
            <p className="text-gray-500 text-sm">모든 제품은 전문가의 검수를 거쳐 입고됩니다.</p>
          </div>
          <div>
            <div className="text-3xl mb-4">🔒</div>
            <h3 className="text-xl font-bold mb-2">안전한 결제</h3>
            <p className="text-gray-500 text-sm">당신의 소중한 정보는 강력하게 보호됩니다.</p>
          </div>
        </div>
      </section>
    </div>
  );
}