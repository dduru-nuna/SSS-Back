package com.example.demo.domain.products.service;

import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.member.repository.MemberRepository;
import com.example.demo.domain.order.entity.OrderInfo;
import com.example.demo.domain.order.repository.OrderInfoRepository;
import com.example.demo.domain.products.controller.form.ReviewImgResponse;
import com.example.demo.domain.products.entity.Product;
import com.example.demo.domain.products.entity.Review;
import com.example.demo.domain.products.entity.ReviewImg;
import com.example.demo.domain.products.repository.ProductsRepository;
import com.example.demo.domain.products.repository.ReviewImgRepository;
import com.example.demo.domain.products.repository.ReviewRepository;
import com.example.demo.domain.products.service.request.ReviewRequest;
import com.example.demo.domain.products.service.response.ReviewListResponse;
import com.example.demo.domain.utility.member.MemberUtils;
import com.example.demo.domain.utility.file.FileUploadUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReviewServiceImpl implements ReviewService {

    final private MemberRepository memberRepository;
    final private ReviewRepository reviewRepository;
    final private ReviewImgRepository reviewImgRepository;
    final private OrderInfoRepository orderInfoRepository;
    final private ProductsRepository productsRepository;
    public Product getProductById( Long productId) {
        return productsRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("등록된 Product 상품이 아닙니다. : " + productId));
    }

    private List<ReviewListResponse> getReviewList(List<Review> reviewList) {
        List<ReviewListResponse> responseList = new ArrayList<>();
        for (Review review : reviewList) {
            List<ReviewImgResponse> reviewImgList = reviewImgRepository.findReviewImgById(review.getReviewId());
            responseList.add(
                    new ReviewListResponse(
                            review.getReviewId(),
                            review.getProduct().getProductId(),
                            review.getOrderInfo().getMember().getNickname(),
                            reviewImgList,
                            review.getRating(),
                            review.getContent(),
                            review.getRegDate(),
                            review.getUpdDate()
                    )
            );
        }
        return responseList;
    }

    private void deleteReviewImages(List<ReviewImgResponse> reviewImgs, String imgPath) {
        for (ReviewImgResponse img : reviewImgs) {
            String fileName = img.getEditedImg();
            System.out.println(fileName);

            File file = new File(imgPath + fileName);

            if (file.exists()) {
                file.delete();
            } else {
                System.out.println("파일 삭제 실패");
            }
        }
    }


    private Review getNewReview(ReviewRequest request){
    
        Member member = MemberUtils.getMemberById(memberRepository,request.getMemberId());
        Product product = getProductById(request.getProductId());

        OrderInfo myOrderInfo = orderInfoRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("등록된 주문 정보가 없습니다..: " + request.getOrderId()));

        Review review =  Review.builder()
                .product(product)
                .member(member)
                .rating(request.getRating())
                .content(request.getContent())
                .orderInfo(myOrderInfo)
                .build();
        return review;
    }


    @Override
    public void registerText(ReviewRequest request) {

        Review newReview = getNewReview(request);
        reviewRepository.save(newReview);
    }

    @Override
    public void register(List<MultipartFile> reviewImgList, ReviewRequest request) throws IOException {

        List<ReviewImg> imgList = new ArrayList<>();

        Review review = getNewReview(request);

        for (MultipartFile multipartFile : reviewImgList) {
            String originImg = multipartFile.getOriginalFilename();
            String editedImg = FileUploadUtils.generateUniqueFileName(originImg);
            String imgPath = "../SSS-Front/src/assets/review/" + editedImg;

            FileUploadUtils.writeFile(multipartFile, imgPath);

            ReviewImg reviewImg = ReviewImg.builder()
                    .originImg(originImg)
                    .editedImg(editedImg)
                    .imgPath(imgPath)
                    .review(review)
                    .build();

            imgList.add(reviewImg);
            log.info(multipartFile.getOriginalFilename());
        }
        reviewRepository.save(review);
        reviewImgRepository.saveAll(imgList);
    }


    @Override
    @Transactional
    public List<ReviewListResponse> productReviewList(Long productId) {

        Product product = getProductById( productId);

        List<Review> reviewList = reviewRepository.findByProduct_ProductId(product.getProductId());

        return getReviewList(reviewList);
    }

    @Override
    @Transactional
    public List<ReviewListResponse> memberReviewList(Long memberId) {
        Member member = MemberUtils.getMemberById(memberRepository,memberId);
        List<Review> reviewList = reviewRepository.findByMember_MemberId(member.getMemberId());

        return getReviewList(reviewList);
    }


    @Override
    public List<ReviewImgResponse> reviewImgList(Long reviewId) {
        List<ReviewImgResponse> reviewImgList = reviewImgRepository.findReviewImgById(reviewId);

        for(ReviewImgResponse reviewImgResponse: reviewImgList) {
            System.out.println("리뷰이미지: " + reviewImgResponse.getEditedImg());
        }

        return reviewImgList;
    }

    private Review getModifyReview(Long reviewId, ReviewRequest request){
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("해당 reviewId 정보 없음:  " + reviewId));

        review.setRating(request.getRating());
        review.setContent(request.getContent());
        return review;
    }

    @Override
    public void modifyText(Long reviewId, ReviewRequest request) {

        Review review = getModifyReview(reviewId, request);
        reviewRepository.save(review);
    }


    @Override
    public void modify(Long reviewId, List<MultipartFile> reviewImgList, ReviewRequest request) {
        final String imgPath = "../SSS-Front/src/assets/review/";

        List<ReviewImgResponse> maybeImgs = reviewImgRepository.findReviewImgById(reviewId);
        if(!maybeImgs.isEmpty()) {
            List<ReviewImgResponse> removeImgs = reviewImgRepository.findReviewImgById(reviewId);

            deleteReviewImages(removeImgs, imgPath);
            reviewImgRepository.deleteReviewImgById(reviewId);
        }

        Review review = getModifyReview(reviewId, request);

        try {
            List<ReviewImg> imgList = new ArrayList<>();
            for (MultipartFile multipartFile: reviewImgList) {
                log.info(multipartFile.getOriginalFilename());

                UUID uuid = UUID.randomUUID();

                String originImg = multipartFile.getOriginalFilename();
                String editedImg = uuid + originImg;

                FileOutputStream writer = new FileOutputStream(imgPath + editedImg);

                writer.write(multipartFile.getBytes());
                writer.close();

                ReviewImg reviewImg = ReviewImg.builder()
                        .originImg(originImg)
                        .editedImg(editedImg)
                        .imgPath(imgPath)
                        .review(review)
                        .build();

                imgList.add(reviewImg);
            }
            reviewImgRepository.saveAll(imgList);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        reviewRepository.save(review);
    }

    @Override
    public void delete(Long reviewId) {
        List<ReviewImgResponse> removeImgs = reviewImgRepository.findReviewImgById(reviewId);
        final String imgPath = "../SSS-Front/src/assets/review/";

        deleteReviewImages(removeImgs, imgPath);

        reviewImgRepository.deleteReviewImgById(reviewId);
        reviewRepository.deleteById(reviewId);
    }
}

