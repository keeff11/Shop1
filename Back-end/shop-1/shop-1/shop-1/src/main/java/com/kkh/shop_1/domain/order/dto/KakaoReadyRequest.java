package com.kkh.shop_1.domain.order.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class KakaoReadyRequest {
    private final String cid;
    private final String partner_order_id;
    private final String partner_user_id;
    private final String item_name;
    private final Integer quantity;
    private final Integer total_amount;
    private final Integer tax_free_amount = 0;
    private final String approval_url;
    private final String cancel_url;
    private final String fail_url;

    public static KakaoReadyRequest of(String cid, PaymentReadyRequestDTO req) {
        return new KakaoReadyRequest(
                cid,
                req.getPartnerOrderId(),
                req.getPartnerUserId(),
                req.getItemName(),
                req.getQuantity(),
                req.getTotalAmount(),
                req.getApprovalUrl(),
                req.getCancelUrl(),
                req.getFailUrl()
        );
    }
}
