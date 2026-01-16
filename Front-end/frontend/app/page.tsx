import { redirect } from "next/navigation";

export default function RootPage() {
  // 접속하자마자 /home 경로로 리다이렉트 (서버 사이드 처리)
  redirect("/home");
}