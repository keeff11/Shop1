package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.entity.PaymentType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentServiceFactory {

    private final Map<PaymentType, PaymentService> services;


    public PaymentServiceFactory(List<PaymentService> paymentServices) {
        this.services = paymentServices.stream()
                .collect(Collectors.toMap(
                        PaymentService::getPaymentType,
                        Function.identity()
                ));
    }

    public PaymentService getService(PaymentType type) {
        PaymentService service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("지원하지 않는 결제 수단입니다: " + type);
        }
        return service;
    }
}