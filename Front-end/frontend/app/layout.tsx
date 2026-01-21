import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import Header from "@/components/Header";
import { Toaster } from "react-hot-toast"; // ★ 임포트 확인

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Shop1",
  description: "Shop1",
  icons: {
    icon: "/shop1-logo.png",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko"> 
      <body className={`${geistSans.variable} ${geistMono.variable} antialiased`}>
        {/* Toaster 설정: 
          zIndex를 99999로 설정하여 헤더보다 무조건 위에 뜨게 만듭니다.
        */}
        <Toaster 
          position="top-center" 
          containerStyle={{
            top: 40,
            zIndex: 99999, // ★ 헤더에 가려지지 않도록 최상단 설정
          }}
          toastOptions={{
            duration: 3000,
            style: {
              background: "#333",
              color: "#fff",
              borderRadius: "16px",
              padding: "16px 24px",
              fontSize: "15px",
              fontWeight: "600",
              boxShadow: "0 10px 25px rgba(0,0,0,0.15)",
            },
            success: {
              iconTheme: {
                primary: "#10B981",
                secondary: "#fff",
              },
            },
          }}
        />
        
        <Header />
        <main>{children}</main>
      </body>
    </html>
  );
}