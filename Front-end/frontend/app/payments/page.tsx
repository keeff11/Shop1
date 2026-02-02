"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchApi } from "@/lib/api";
import dynamic from "next/dynamic";

// 다음 주소 찾기 모듈 (SSR 제외)
const DaumPostcode = dynamic(() => import("react-daum-postcode"), { ssr: false });

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

export default function PaymentPage() {
  const router = useRouter();
  const [orderItems, setOrderItems] = useState<OrderItem[]>([]);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  
  // 신규 배송지 입력 모드 여부
  const [useNewAddress, setUseNewAddress] = useState(false);
  
  // 신규 배송지 데이터
  const [newAddress, setNewAddress] = useState({
    zipCode: "",
    roadAddress: "",
    detailAddress: "",
    recipientName: "",
    recipientPhone: "",
  });
  
  const [showPostcode, setShowPostcode] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // 1. 세션 스토리지에서 결제할 상품 정보 로드
    const saved = sessionStorage.getItem("checkoutData");
    if (!saved) {
      alert("잘못된 접근입니다.");
      router.push("/cart");
      return;
    }
    setOrderItems(JSON.parse(saved).itemOrders || []);

    // 2. 사용자의 기존 배송지 목록 조회
    fetchApi<{ data: Address[] }>("/user/addresses", { credentials: "include" })
      .then(res => {
        if (res.data && res.data.length > 0) {
          setAddresses(res.data);
          // 기본 배송지 혹은 첫 번째 배송지 선택
          const def = res.data.find(a => a.isDefault) || res.data[0];
          setSelectedAddressId(def.id);
        } else {
          // 배송지가 없으면 신규 입력 모드로 전환
          setUseNewAddress(true);
        }
      })
      .catch((err) => {
        console.error("배송지 조회 실패:", err);
        setUseNewAddress(true);
      });
  }, [router]);

  // 총 결제 금액 계산
  const totalPrice = orderItems.reduce((acc, i) => acc + i.price * i.quantity, 0);

  /**
   * 결제 요청 함수
   */
  const requestPayment = async (paymentType: "KAKAO_PAY" | "NAVER_PAY") => {
    if (orderItems.length === 0) return alert("상품 정보가 없습니다.");
    if (!useNewAddress && !selectedAddressId) return alert("배송지를 선택해주세요.");
    
    // 신규 배송지 입력 시 유효성 검사 (간단 예시)
    if (useNewAddress && (!newAddress.zipCode || !newAddress.recipientName)) {
      return alert("배송지 정보를 모두 입력해주세요.");
    }

    setLoading(true);
    try {
      // 결제 요청 데이터 구성
      const body = {
        paymentType,
        itemOrders: orderItems.map(i => ({ itemId: i.itemId, quantity: i.quantity })),
        totalAmount: totalPrice,
        
        // 배송지 정보 (기존 선택 vs 신규 입력)
        addressId: useNewAddress ? null : selectedAddressId,
        zipCode: useNewAddress ? newAddress.zipCode : "",
        roadAddress: useNewAddress ? newAddress.roadAddress : "",
        detailAddress: useNewAddress ? newAddress.detailAddress : "",
        recipientName: useNewAddress ? newAddress.recipientName : "",
        recipientPhone: useNewAddress ? newAddress.recipientPhone : "",
        
        // approvalUrl에 {orderId} 플레이스홀더 포함
        approvalUrl: `${window.location.origin}/orders/success?orderId={orderId}`,
        cancelUrl: `${window.location.origin}/cart`,
        failUrl: `${window.location.origin}/cart`,
      };

      // 백엔드 주문 생성 API 호출
      const res = await fetchApi<{ data: { redirectUrl: string } }>("/orders/create", {
        method: "POST",
        body: JSON.stringify(body),
        credentials: "include"
      });

      // PG사 결제 페이지로 리다이렉트
      if (res.data?.redirectUrl) {
        window.location.href = res.data.redirectUrl;
      }
    } catch (err) {
      console.error(err);
      alert("결제 요청 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-10 px-4">
      <div className="max-w-2xl mx-auto space-y-6">
        <h1 className="text-2xl font-bold text-center mb-8">주문 / 결제</h1>

        {/* 1. 주문 상품 리스트 */}
        <div className="bg-white rounded-2xl p-6 border shadow-sm">
          <h2 className="font-bold text-lg mb-4 border-b pb-2 text-gray-700">주문 상품 ({orderItems.length})</h2>
          <div className="divide-y">
            {orderItems.map((item, idx) => (
              <div key={idx} className="py-4 flex justify-between items-center">
                <div className="flex items-center gap-4">
                  {item.imageUrl && (
                    <img 
                      src={item.imageUrl} 
                      className="w-14 h-14 object-cover rounded-lg bg-gray-100" 
                      alt={item.itemName} 
                    />
                  )}
                  <div>
                    <p className="font-semibold text-gray-800">{item.itemName}</p>
                    <p className="text-sm text-gray-500">{item.quantity}개</p>
                  </div>
                </div>
                <span className="font-bold">{(item.price * item.quantity).toLocaleString()}원</span>
              </div>
            ))}
          </div>
          <div className="mt-4 pt-4 border-t flex justify-between items-center text-xl font-black">
            <span className="text-gray-600">총 결제 금액</span>
            <span className="text-blue-600">{totalPrice.toLocaleString()}원</span>
          </div>
        </div>

        {/* 2. 배송 정보 입력/선택 */}
        <div className="bg-white rounded-2xl p-6 border shadow-sm space-y-5">
          <h2 className="font-bold text-lg border-b pb-2">배송 정보</h2>
          
          {/* 탭 버튼 */}
          <div className="flex gap-2 p-1 bg-gray-100 rounded-xl">
            <button 
              onClick={() => setUseNewAddress(false)} 
              className={`flex-1 py-2 rounded-lg font-bold transition ${!useNewAddress ? "bg-white shadow-sm text-gray-900" : "text-gray-500"}`}
            >
              기존 배송지
            </button>
            <button 
              onClick={() => setUseNewAddress(true)} 
              className={`flex-1 py-2 rounded-lg font-bold transition ${useNewAddress ? "bg-white shadow-sm text-gray-900" : "text-gray-500"}`}
            >
              신규 입력
            </button>
          </div>

          {!useNewAddress ? (
            // 기존 배송지 목록
            <div className="space-y-2">
              {addresses.map(addr => (
                <label key={addr.id} className={`flex items-center gap-4 border p-4 rounded-xl cursor-pointer transition ${selectedAddressId === addr.id ? "border-gray-800 bg-gray-50" : "border-gray-100"}`}>
                  <input 
                    type="radio" 
                    checked={selectedAddressId === addr.id} 
                    onChange={() => setSelectedAddressId(addr.id)} 
                    className="w-4 h-4 accent-gray-800" 
                  />
                  <div className="text-sm flex-1">
                    <p className="font-bold text-gray-900">{addr.recipientName} ({addr.recipientPhone})</p>
                    <p className="text-gray-600">{addr.roadAddress} {addr.detailAddress}</p>
                  </div>
                </label>
              ))}
              {addresses.length === 0 && <p className="text-center text-gray-500 py-4">등록된 배송지가 없습니다.</p>}
            </div>
          ) : (
            // 신규 배송지 입력 폼
            <div className="space-y-3">
              <div className="flex gap-2">
                <input type="text" placeholder="우편번호" value={newAddress.zipCode} readOnly className="flex-1 p-3 border rounded-xl bg-gray-50 outline-none" />
                <button onClick={() => setShowPostcode(true)} className="px-5 bg-gray-800 text-white rounded-xl text-sm font-bold">주소 찾기</button>
              </div>
              <input type="text" placeholder="기본 주소" value={newAddress.roadAddress} readOnly className="w-full p-3 border rounded-xl bg-gray-50 outline-none" />
              <input 
                type="text" 
                placeholder="상세 주소" 
                onChange={e => setNewAddress({...newAddress, detailAddress: e.target.value})} 
                className="w-full p-3 border rounded-xl outline-none focus:border-gray-400" 
              />
              <input 
                type="text" 
                placeholder="수령인 성함" 
                onChange={e => setNewAddress({...newAddress, recipientName: e.target.value})} 
                className="w-full p-3 border rounded-xl outline-none focus:border-gray-400" 
              />
              <input 
                type="text" 
                placeholder="수령인 연락처" 
                onChange={e => setNewAddress({...newAddress, recipientPhone: e.target.value})} 
                className="w-full p-3 border rounded-xl outline-none focus:border-gray-400" 
              />
            </div>
          )}
        </div>

        {/* 3. 결제 수단 선택 버튼 (수정됨: 배경 투명, 테두리 추가) */}
        <div className="grid grid-cols-1 gap-3">
          {/* 카카오페이 버튼 */}
          <button 
            onClick={() => requestPayment("KAKAO_PAY")} 
            disabled={loading} 
            className="w-full h-16 bg-transparent border border-gray-300 rounded-2xl flex items-center justify-center gap-3 hover:bg-gray-50 transition shadow-sm"
          >
            <img src="/kakao_pay.png" className="h-6" alt="카카오페이" />
            <span className="font-bold text-lg text-gray-800">카카오페이 결제</span>
          </button>
          
          {/* 네이버페이 버튼 (수정됨: 흰색 필터 제거, 글자색 변경) */}
          <button 
            onClick={() => requestPayment("NAVER_PAY")} 
            disabled={loading} 
            className="w-full h-16 bg-transparent border border-gray-300 rounded-2xl flex items-center justify-center gap-3 hover:bg-gray-50 transition shadow-sm"
          >
            <img src="/naver_pay.svg" className="h-6" alt="네이버페이" />
            <span className="font-bold text-lg text-gray-800">네이버페이 결제</span>
          </button>
        </div>
      </div>

      {/* 다음 주소 찾기 모달 */}
      {showPostcode && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-white w-full max-w-md rounded-2xl overflow-hidden relative shadow-2xl">
            <div className="p-4 border-b flex justify-between items-center bg-gray-50">
              <span className="font-bold">주소 찾기</span>
              <button onClick={() => setShowPostcode(false)} className="text-2xl text-gray-400 hover:text-gray-600">&times;</button>
            </div>
            <DaumPostcode 
              onComplete={(data: any) => { 
                setNewAddress({...newAddress, zipCode: data.zonecode, roadAddress: data.roadAddress}); 
                setShowPostcode(false); 
              }} 
            />
          </div>
        </div>
      )}
    </div>
  );
}