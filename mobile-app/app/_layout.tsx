import { DarkTheme, DefaultTheme, ThemeProvider } from "@react-navigation/native";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import "react-native-reanimated";

import { useColorScheme } from "@/hooks/use-color-scheme";
import Header from "../components/Header";
import { AuthProvider } from "../contexts/AuthContext"; // ✅ AuthProvider import

export const unstable_settings = {
  anchor: "(tabs)",
};

export default function RootLayout() {
  const colorScheme = useColorScheme();

  return (
    // ✅ 1. AuthProvider로 전체를 감싸야 로그인 상태가 공유됩니다.
    <AuthProvider>
      <ThemeProvider value={colorScheme === "dark" ? DarkTheme : DefaultTheme}>
        <Stack
          screenOptions={{
            // ✅ 2. 모든 화면에 공통 Header 적용
            header: () => <Header />, 
          }}
        >
          {/* 탭 네비게이션 */}
          <Stack.Screen
            name="(tabs)"
            options={{
              headerShown: true, 
            }}
          />

          {/* 모달 화면 */}
          <Stack.Screen
            name="modal"
            options={{
              presentation: "modal",
              headerShown: false, // 모달은 헤더 숨김
            }}
          />
          
          {/* (참고) 로그인 페이지 등 헤더를 숨기고 싶은 페이지가 있다면 여기에 추가 */}
           {/* <Stack.Screen 
             name="register/index" 
             options={{ headerShown: false }} 
           /> 
           */}

        </Stack>

        <StatusBar style="auto" />
      </ThemeProvider>
    </AuthProvider>
  );
}