package com.kkh.shop_1.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // ★ 필수: JSON 파싱을 위한 기본 생성자
@AllArgsConstructor
public class ReviewRequestDto {

    private int rating;
    private String content;

}