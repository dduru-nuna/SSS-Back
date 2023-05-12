package com.example.demo.domain.products.service.response;

import com.example.demo.domain.products.controller.form.ReviewImgResponse;
import com.example.demo.domain.products.entity.ReviewImg;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Date;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class ReviewListResponse {

    final private Long reviewId;
    final private Long productId;
    final private String nickName;
    final private List<ReviewImgResponse> reviewImgs;
    final private int rating;
    final private String content;
    final private Date regDate;
    final private Date updDate;

}
