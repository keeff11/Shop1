"use client";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <main className="flex flex-col justify-center items-center h-[calc(100vh-120px)] gap-6">
        <h1 className="text-4xl font-bold text-gray-700 text-center">
          환영합니다! Shop1에 오신 것을 환영합니다.
        </h1>
        <button
          className="px-6 py-3 bg-primary text-primary-foreground text-lg rounded-lg hover:bg-primary/90 transition-colors shadow-lg"
          onClick={() => {}}
        >
          전체 상품 보러가기
        </button>
      </main>
    </div>
  );
}
