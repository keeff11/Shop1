import Constants from "expo-constants";

export const API_BASE =
  Constants.expoConfig?.extra?.apiBaseUrl ?? "http://192.168.0.69:8080";
