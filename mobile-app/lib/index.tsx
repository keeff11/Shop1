import { Redirect } from "expo-router";

export default function RootPage() {
  // index를 생략하고 탭의 루트 경로로 이동
  return <Redirect href="/(tabs)" />; 
}