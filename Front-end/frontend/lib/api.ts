class ApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function fetchApi<T>(path: string, options: RequestInit = {}): Promise<T> {
  const url = path.startsWith('http') ? path : `${process.env.NEXT_PUBLIC_API_BASE_URL}${path}`;

  // Create a new Headers object from the provided headers
  const headers = new Headers(options.headers);

  // Set Content-Type to application/json if it's not FormData and not already set
  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, {
    credentials: 'include', // [핵심 수정] 모든 요청 시 쿠키(토큰)를 자동으로 포함 전송
    ...options,             // 외부에서 옵션을 덮어쓸 수 있도록 뒤에 배치
    headers,
  });

  if (!response.ok) {
    // Try to parse the error response, but fall back to status text if it fails
    try {
      const errorBody = await response.json();
      throw new ApiError(errorBody.message || `HTTP error! status: ${response.status}`);
    } catch {
      throw new ApiError(`HTTP error! status: ${response.status}`);
    }
  }

  // Handle cases where the response might be empty (e.g., a 204 No Content)
  const responseText = await response.text();
  if (!responseText) {
    return {} as T;
  }
  
  return JSON.parse(responseText) as T;
}

export { fetchApi, ApiError }