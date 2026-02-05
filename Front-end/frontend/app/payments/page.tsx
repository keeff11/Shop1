"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchApi } from "@/lib/api";
import dynamic from "next/dynamic";
import { toast } from "sonner";
import Script from "next/script";

const DaumPostcode = dynamic(() => import("react-daum-postcode"), { ssr: false });

const KAKAO_PAY_LOGO = "/kakao_pay.png";
const NAVER_PAY_LOGO = "/naver_pay.svg";
const TOSS_PAY_LOGO = "/toss_pay.png";

// [수정] 환경 변수에서 클라이언트 키 로드
const TOSS_CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY!;

interface OrderItem {
  itemId: number;
  itemName: string;
  price: number;
  quantity: number;
  imageUrl?: string;
}

interface Address {
  id: number;
  zipCode: string;
  roadAddress: string;
  detailAddress: string;
  recipientName: string;
  recipientPhone: string;
  isDefault: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface OrderResponse {
  orderId: number;
  tid: string;          
  redirectUrl?: string; 
  orderDate: string;
}

export default function PaymentPage() {
  const router = useRouter();
  const [orderItems, setOrderItems] = useState<OrderItem[]>([]);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  
  const [useNewAddress, setUseNewAddress] = useState(false);
  const [newAddress, setNewAddress] = useState({
    zipCode: "",
    roadAddress: "",
    detailAddress: "",
    recipientName: "",
    recipientPhone: "",
  });
  
  // [토스페이 포함] 결제 수단 상태 관리
  const [paymentType, setPaymentType] = useState<"KAKAO_PAY" | "NAVER_PAY" | "TOSS_PAY">("KAKAO_PAY");
  const [showPostcode, setShowPostcode] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // 1. 주문 정보 로드
    const saved = sessionStorage.getItem("checkoutData");
    if (!saved) {
      return;
    }
    try {
      const parsed = JSON.parse(saved);
      setOrderItems(parsed.itemOrders || []);
    } catch (e) {
      console.error(e);
    }

    // 2. 배송지 목록 조회
    fetchApi<ApiResponse<Address[]>>("/user/addresses", { credentials: "include" })
      .then(res => {
        if (res.success && Array.isArray(res.data) && res.data.length > 0) {
          setAddresses(res.data);
          const def = res.data.find(a => a.isDefault) || res.data[0];
          setSelectedAddressId(def.id);
        } else {
          setUseNewAddress(true);
        }
      })
      .catch((err) => {
        console.warn("배송지 로드 실패:", err);
        setUseNewAddress(true);
      });
  }, [router]);

  const totalPrice = orderItems.reduce((acc, i) => acc + i.price * i.quantity, 0);
  const finalPrice = totalPrice + 3000; 

  const handlePayment = async () => {
    // 유효성 검사
    if (orderItems.length === 0) return toast.error("결제할 상품이 없습니다.");
    if (!useNewAddress && !selectedAddressId) return toast.error("배송지를 선택해주세요.");
    if (useNewAddress && (!newAddress.zipCode || !newAddress.recipientName)) {
      return toast.error("배송지 정보를 모두 입력해주세요.");
    }
    
    // 환경 변수 로드 체크
    if (paymentType === "TOSS_PAY" && !TOSS_CLIENT_KEY) {
      return toast.error("토스 클라이언트 키가 설정되지 않았습니다.");
    }

    setLoading(true);
    try {
      const body = {
        paymentType,
        itemOrders: orderItems.map(i => ({ itemId: i.itemId, quantity: i.quantity })),
        
        addressId: useNewAddress ? null : selectedAddressId,
        zipCode: useNewAddress ? newAddress.zipCode : null,
        roadAddress: useNewAddress ? newAddress.roadAddress : null,
        detailAddress: useNewAddress ? newAddress.detailAddress : null,
        recipientName: useNewAddress ? newAddress.recipientName : null,
        recipientPhone: useNewAddress ? newAddress.recipientPhone : null,
        
        approvalUrl: `${window.location.origin}/orders/success`,
        cancelUrl: `${window.location.origin}/cart`,
        failUrl: `${window.location.origin}/cart`,
      };

      // 1. 주문 생성 요청
      const res = await fetchApi<ApiResponse<OrderResponse>>("/orders/create", {
        method: "POST",
        body: JSON.stringify(body),
        credentials: "include"
      });

      if (!res.success || !res.data) {
        throw new Error(res.message || "주문 생성 실패");
      }

      const { tid, redirectUrl } = res.data;

      // 2. 결제 수단별 처리
      if (paymentType === "KAKAO_PAY" || paymentType === "NAVER_PAY") {
        if (redirectUrl) {
          window.location.href = redirectUrl;
        } else {
          throw new Error("결제 페이지 URL을 받지 못했습니다.");
        }
      } else if (paymentType === "TOSS_PAY") {
        // [토스] SDK 호출
        const tossPayments = (window as any).TossPayments(TOSS_CLIENT_KEY);
        
        await tossPayments.requestPayment("카드", {
          amount: finalPrice,
          orderId: tid, // 백엔드에서 생성한 TID 사용
          orderName: orderItems[0].itemName + (orderItems.length > 1 ? ` 외 ${orderItems.length - 1}건` : ""),
          customerName: "구매자", 
          successUrl: `${window.location.origin}/orders/success`,
          failUrl: `${window.location.origin}/orders/fail`,
        });
      }

    } catch (err: any) {
      console.error(err);
      // 토스 결제 취소 에러 무시
      if (err.code === "USER_CANCEL") {
        toast.info("결제를 취소했습니다.");
      } else {
        toast.error(err.message || "결제 요청 중 오류가 발생했습니다.");
      }
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <Script src="https://js.tosspayments.com/v1/payment" strategy="lazyOnload" />
      
      <div className="max-w-5xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-10">
        
        {/* 왼쪽 섹션: 상품 & 배송지 */}
        <div className="space-y-8">
          {/* 상품 목록 */}
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
            <h2 className="font-bold text-xl mb-6 text-gray-900 flex items-center gap-2">
              🛒 주문 상품 <span className="text-gray-400 text-sm font-normal">({orderItems.length}개)</span>
            </h2>
            <div className="space-y-4">
              {orderItems.map((item, idx) => (
                <div key={idx} className="flex gap-4 p-3 hover:bg-gray-50 rounded-xl transition-colors">
                  <div className="w-20 h-20 bg-gray-100 rounded-lg overflow-hidden shrink-0 border border-gray-100">
                    <img src={item.imageUrl || "/no_image.jpg"} className="w-full h-full object-cover" alt={item.itemName} />
                  </div>
                  <div className="flex-1 flex flex-col justify-center">
                    <p className="font-medium text-gray-900 line-clamp-1 mb-1">{item.itemName}</p>
                    <p className="text-sm text-gray-500 mb-1">수량 {item.quantity}개</p>
                    <p className="font-bold text-gray-900">{item.price.toLocaleString()}원</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* 배송지 정보 */}
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
            <div className="flex justify-between items-center mb-6">
              <h2 className="font-bold text-xl text-gray-900">📍 배송 정보</h2>
              <div className="flex bg-gray-100 p-1 rounded-lg">
                <button 
                  onClick={() => setUseNewAddress(false)}
                  className={`px-3 py-1.5 text-sm font-medium rounded-md transition-all ${!useNewAddress ? "bg-white text-gray-900 shadow-sm" : "text-gray-500 hover:text-gray-900"}`}
                >
                  기존 배송지
                </button>
                <button 
                  onClick={() => setUseNewAddress(true)}
                  className={`px-3 py-1.5 text-sm font-medium rounded-md transition-all ${useNewAddress ? "bg-white text-gray-900 shadow-sm" : "text-gray-500 hover:text-gray-900"}`}
                >
                  신규 입력
                </button>
              </div>
            </div>

            {!useNewAddress ? (
              <div className="space-y-3">
                {addresses.map(addr => (
                  <label key={addr.id} className={`group flex items-start gap-4 p-4 border-2 rounded-xl cursor-pointer transition-all ${selectedAddressId === addr.id ? "border-black bg-gray-50/50" : "border-gray-100 hover:border-gray-300 hover:bg-gray-50"}`}>
                    <div className="mt-1">
                      <input type="radio" checked={selectedAddressId === addr.id} onChange={() => setSelectedAddressId(addr.id)} className="w-4 h-4 text-black focus:ring-black cursor-pointer" />
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-bold text-gray-900">{addr.recipientName}</span>
                        {addr.isDefault && <span className="text-[10px] bg-gray-200 text-gray-600 px-1.5 py-0.5 rounded">기본</span>}
                      </div>
                      <p className="text-sm text-gray-500 mb-1">{addr.recipientPhone}</p>
                      <p className="text-sm text-gray-700 leading-snug">[{addr.zipCode}] {addr.roadAddress} {addr.detailAddress}</p>
                    </div>
                  </label>
                ))}
                {addresses.length === 0 && (
                  <div className="text-center py-8 text-gray-500 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                    저장된 배송지가 없습니다.<br/>새로 입력해주세요.
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex gap-2">
                  <input 
                    type="text" 
                    value={newAddress.zipCode} 
                    placeholder="우편번호" 
                    readOnly 
                    className="w-32 p-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none" 
                  />
                  <button onClick={() => setShowPostcode(true)} className="flex-1 bg-gray-900 text-white rounded-xl text-sm font-bold hover:bg-black transition-colors">
                    주소 검색
                  </button>
                </div>
                <input type="text" value={newAddress.roadAddress} placeholder="기본 주소" readOnly className="w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none" />
                <input type="text" placeholder="상세 주소 (동/호수)" onChange={e => setNewAddress({...newAddress, detailAddress: e.target.value})} className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:border-black focus:ring-1 focus:ring-black outline-none transition-all" />
                <div className="grid grid-cols-2 gap-4">
                  <input type="text" placeholder="수령인 이름" onChange={e => setNewAddress({...newAddress, recipientName: e.target.value})} className="p-3 border border-gray-200 rounded-xl text-sm focus:border-black focus:ring-1 focus:ring-black outline-none transition-all" />
                  <input type="text" placeholder="연락처 (- 없이)" onChange={e => setNewAddress({...newAddress, recipientPhone: e.target.value})} className="p-3 border border-gray-200 rounded-xl text-sm focus:border-black focus:ring-1 focus:ring-black outline-none transition-all" />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* 오른쪽 섹션: 결제 정보 */}
        <div className="space-y-6">
          <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 h-fit sticky top-24">
            <h2 className="font-bold text-xl mb-6 text-gray-900">💳 결제 정보</h2>
            
            <div className="space-y-4 mb-8">
              <div className="flex justify-between text-gray-500">
                <span>총 상품금액</span>
                <span>{totalPrice.toLocaleString()}원</span>
              </div>
              <div className="flex justify-between text-gray-500">
                <span>배송비</span>
                <span>3,000원</span>
              </div>
              <div className="h-px bg-gray-100 my-4"></div>
              <div className="flex justify-between items-center">
                <span className="font-bold text-lg text-gray-900">최종 결제 금액</span>
                <span className="font-black text-3xl text-red-600">{finalPrice.toLocaleString()}원</span>
              </div>
            </div>

            <div className="space-y-3 mb-8">
              <label className="text-sm font-bold text-gray-700 block mb-2">결제 수단 선택</label>
              
              <label className={`group relative flex items-center p-4 border rounded-xl cursor-pointer transition-all duration-200 ${paymentType === "KAKAO_PAY" ? "border-yellow-400 bg-yellow-50 ring-1 ring-yellow-400 shadow-sm" : "border-gray-200 hover:border-gray-400 hover:bg-gray-50"}`}>
                <input type="radio" name="pay" checked={paymentType === "KAKAO_PAY"} onChange={() => setPaymentType("KAKAO_PAY")} className="hidden" />
                <img src={KAKAO_PAY_LOGO} className="h-6 w-auto mr-3" alt="Kakao" />
                <span className="font-bold text-gray-800">카카오페이</span>
                {paymentType === "KAKAO_PAY" && <div className="absolute right-4 w-2 h-2 bg-yellow-500 rounded-full"></div>}
              </label>

              <label className={`group relative flex items-center p-4 border rounded-xl cursor-pointer transition-all duration-200 ${paymentType === "NAVER_PAY" ? "border-[#03C75A] bg-[#03C75A]/5 ring-1 ring-[#03C75A] shadow-sm" : "border-gray-200 hover:border-gray-400 hover:bg-gray-50"}`}>
                <input type="radio" name="pay" checked={paymentType === "NAVER_PAY"} onChange={() => setPaymentType("NAVER_PAY")} className="hidden" />
                <img src={NAVER_PAY_LOGO} className="h-6 w-auto mr-3" alt="Naver" />
                <span className="font-bold text-gray-800">네이버페이</span>
                {paymentType === "NAVER_PAY" && <div className="absolute right-4 w-2 h-2 bg-[#03C75A] rounded-full"></div>}
              </label>

              {/* [토스페이 버튼] */}
              <label className={`group relative flex items-center p-4 border rounded-xl cursor-pointer transition-all duration-200 ${paymentType === "TOSS_PAY" ? "border-blue-500 bg-blue-50 ring-1 ring-blue-500 shadow-sm" : "border-gray-200 hover:border-gray-400 hover:bg-gray-50"}`}>
                <input type="radio" name="pay" checked={paymentType === "TOSS_PAY"} onChange={() => setPaymentType("TOSS_PAY")} className="hidden" />
                <img src={TOSS_PAY_LOGO} className="h-6 w-auto mr-3 object-contain" alt="Toss" />
                <span className="font-bold text-gray-800">토스페이</span>
                {paymentType === "TOSS_PAY" && <div className="absolute right-4 w-2 h-2 bg-blue-500 rounded-full"></div>}
              </label>
            </div>

            <button
              onClick={handlePayment}
              disabled={loading}
              className="w-full py-4 bg-gray-900 text-white font-bold rounded-xl text-lg hover:bg-black transition-all shadow-md active:scale-[0.98] disabled:bg-gray-300 disabled:cursor-not-allowed disabled:active:scale-100"
            >
              {loading ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  <span>처리 중...</span>
                </div>
              ) : (
                "결제하기"
              )}
            </button>
          </div>
        </div>
      </div>

      {showPostcode && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white w-full max-w-md rounded-2xl overflow-hidden shadow-2xl relative">
            <div className="p-4 border-b flex justify-between items-center bg-gray-50">
              <span className="font-bold text-lg text-gray-900">주소 검색</span>
              <button onClick={() => setShowPostcode(false)} className="text-gray-400 hover:text-black transition-colors p-1">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
              </button>
            </div>
            <div className="h-[450px]">
              <DaumPostcode 
                onComplete={(data: any) => {
                  setNewAddress(prev => ({ ...prev, zipCode: data.zonecode, roadAddress: data.roadAddress }));
                  setShowPostcode(false);
                }}
                style={{ height: "100%" }}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}