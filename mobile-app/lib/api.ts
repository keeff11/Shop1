import * as SecureStore from 'expo-secure-store';
import { API_BASE } from '../config/api';

class ApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function fetchApi<T>(path: string, options: RequestInit = {}): Promise<T> {
  const url = path.startsWith('http') ? path : `${API_BASE}${path}`;

  const headers = new Headers(options.headers);

  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  // [핵심 수정] SecureStore에서 토큰을 꺼내서 Bearer 형식으로 자동 추가
  const token = await SecureStore.getItemAsync('accessToken');
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(url, {
    credentials: 'omit', 
    ...options,
    headers,
  });

  if (!response.ok) {
    try {
      const errorBody = await response.json();
      throw new ApiError(errorBody.message || `HTTP error! status: ${response.status}`);
    } catch {
      throw new ApiError(`HTTP error! status: ${response.status}`);
    }
  }

  const responseText = await response.text();
  if (!responseText) {
    return {} as T;
  }
  
  return JSON.parse(responseText) as T;
}

export { ApiError, fetchApi };
