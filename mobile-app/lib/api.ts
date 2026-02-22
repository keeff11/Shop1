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

  // 모바일 특성상 백엔드에서 쿠키(JSESSIONID 등)를 통한 인증을 사용한다면
  // 백엔드 CORS 및 RN fetch 설정 점검이 필요합니다.
  // (JWT 토큰 방식을 쓴다면 이 부분에 AsyncStorage/SecureStore에서 토큰을 꺼내 헤더에 담아야 합니다.)
  const response = await fetch(url, {
    credentials: 'omit', // RN 환경에 맞게 기본값 조정 (필요시 'include' 유지하되 헤더 토큰 권장)
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
