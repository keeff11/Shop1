// Front-end/frontend/lib/api.ts

class ApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function fetchApi<T>(path: string, options: RequestInit = {}): Promise<T> {
  const url = path.startsWith('http') ? path : `${process.env.NEXT_PUBLIC_API_BASE_URL}${path}`;

  const headers = new Headers(options.headers);

  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, {
    credentials: 'include', 
    ...options,             
    headers,
  });

  if (!response.ok) {
    let errorMessage = `HTTP error! status: ${response.status}`;
    
    try {
      const errorText = await response.text();
      try {
        const errorBody = JSON.parse(errorText);
        // 스프링이 message 필드를 내려준다면 해당 메시지 사용
        if (errorBody.message) {
          errorMessage = errorBody.message;
        }
      } catch {
        // 백엔드 에러가 JSON이 아닌 HTML 페이지로 올 경우 텍스트 안에서 감지
        if (errorText.includes("존재하지 않는 사용자입니다")) {
          errorMessage = "존재하지 않는 사용자입니다.";
        }
      }
    } catch (e) {
      // 파싱 실패 시 기본 errorMessage 유지
    }

    // ⭐ [추가됨] 스프링 보안 설정으로 메시지가 넘어오지 않아 여전히 "HTTP error..." 인 경우
    // 로그인 API 경로라면 안내 메시지를 강제로 지정해줍니다.
    if (errorMessage.startsWith("HTTP error") && url.includes("/auth/local/login")) {
      errorMessage = "존재하지 않는 사용자이거나 비밀번호가 틀렸습니다.";
    }

    throw new ApiError(errorMessage);
  }

  const responseText = await response.text();
  if (!responseText) {
    return {} as T;
  }
  
  return JSON.parse(responseText) as T;
}

export { fetchApi, ApiError }