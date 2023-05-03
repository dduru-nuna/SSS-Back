package com.example.demo.domain.order.service;

import com.example.demo.domain.member.entity.Address;
import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.member.repository.AddressRepository;
import com.example.demo.domain.member.repository.MemberRepository;
import com.example.demo.domain.order.controller.form.OrderInfoRegisterForm;
import com.example.demo.domain.order.controller.response.OrderInfoListResponse;
import com.example.demo.domain.order.controller.response.OrderItemListResponse;
import com.example.demo.domain.order.entity.*;
import com.example.demo.domain.order.entity.orderItems.OrderItem;
import com.example.demo.domain.order.entity.orderItems.ProductOrderItem;
import com.example.demo.domain.order.entity.orderItems.SelfSaladOrderItem;
import com.example.demo.domain.order.entity.orderItems.SideProductOrderItem;
import com.example.demo.domain.order.repository.*;
import com.example.demo.domain.order.service.request.DeliveryAddressRequest;
import com.example.demo.domain.order.service.request.DeliveryRegisterRequest;
import com.example.demo.domain.order.service.request.OrderItemRegisterRequest;
import com.example.demo.domain.order.service.request.PaymentRequest;
import com.example.demo.domain.products.entity.Product;
import com.example.demo.domain.products.repository.ProductsRepository;
import com.example.demo.domain.selfSalad.entity.SelfSalad;
import com.example.demo.domain.selfSalad.repository.SelfSaladRepository;
import com.example.demo.domain.sideProducts.entity.SideProduct;
import com.example.demo.domain.sideProducts.repository.SideProductsRepository;
import com.example.demo.domain.utility.common.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.Objects.requireNonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInfoServiceImpl implements OrderInfoService {
    final private ProductsRepository productsRepository;
    final private SideProductsRepository sideProductsRepository;
    final private SelfSaladRepository selfSaladRepository;

    final private OrderInfoRepository orderInfoRepository;
    final private OrderItemRepository orderItemRepository;
    final private OrderStateRepository orderStateRepository;
    final private OrderInfoStateRepository orderInfoStateRepository;

    final private PaymentRepository paymentRepository;

    final private AddressRepository addressRepository;
    final private DeliveryRepository deliveryRepository;

    final private MemberRepository memberRepository;


    private Map<Long, Product> checkProducts(List<OrderItemRegisterRequest> productItems){

        Set<Long> productIds = new HashSet<>();

        for(OrderItemRegisterRequest orderItem : productItems ){

            productIds.add(orderItem.getItemId());
        }
        Optional <List<Product>> maybeProducts =
                productsRepository.findByProductIdIn(productIds);

        if(maybeProducts.isPresent()){
            log.info("Products "+productIds+" 번의 상품들이 존재합니다.");

            Map<Long, Product> productMap = new HashMap<>();

            for (Product product : maybeProducts.get()) {
                productMap.put(product.getProductId(), product);
            }
            return productMap;
        }
        return null;
    }

    private Map<Long, SideProduct> checkSideProducts(List<OrderItemRegisterRequest> sideProductItems){

        Set<Long> sideProductIds = new HashSet<>();
        for(OrderItemRegisterRequest orderItem : sideProductItems ){

            sideProductIds.add(orderItem.getItemId());
        }
        Optional <List<SideProduct>> maybeSideProducts =
                sideProductsRepository.findBySideProductIdIn(sideProductIds);

        if(maybeSideProducts.isPresent()){
            log.info("SideProducts "+sideProductIds+" 번의 상품들이 존재합니다.");

            Map<Long, SideProduct> sideProductMap = new HashMap<>();

            for (SideProduct sideProduct : maybeSideProducts.get()) {
                sideProductMap.put(sideProduct.getSideProductId(), sideProduct);
            }
            return sideProductMap;
        }
        return null;
    }

    private Map<Long, SelfSalad> checkSelfSalad(List<OrderItemRegisterRequest> selfSaladItems){

        Set<Long> selfSaladIds = new HashSet<>();
        for(OrderItemRegisterRequest orderItem : selfSaladItems ){

            selfSaladIds.add(orderItem.getItemId());
        }
        Optional <List<SelfSalad>> maybeSelfSalad =
                selfSaladRepository.findByIdIn(selfSaladIds);

        if(maybeSelfSalad.isPresent()){
            log.info("SelfSalads "+selfSaladIds+" 번의 상품들이 존재합니다.");

            Map<Long, SelfSalad> selfSaladMap = new HashMap<>();

            for (SelfSalad selfSalad : maybeSelfSalad.get()) {
                selfSaladMap.put(selfSalad.getId(), selfSalad);
            }
            return selfSaladMap;
        }
        return null;
    }

    private OrderInfo registerOrderInfo(Member member, Long totalOrderPrice){

        OrderInfo newOrderInfo = new OrderInfo(totalOrderPrice, member);
        orderInfoRepository.save(newOrderInfo);
        log.info(member.getNickname() + " 님의 주문 테이블이 생성되었습니다.");

        return newOrderInfo;
    }

    private void registerOrderState(OrderInfo myOrderInfo){

        final OrderState orderState =
                orderStateRepository.findByOrderStateType(OrderStateType.PAYMENT_COMPLETE);

        orderInfoStateRepository.save(
                new OrderInfoState(myOrderInfo, orderState));
    }

    private void registerPayment(PaymentRequest reqPayment, OrderInfo myOrderInfo){

        Payment myPayment = reqPayment.toPayment(myOrderInfo);
        paymentRepository.save(myPayment);
    }

    @Override
    public Long registerNewAddress(Long memberId, DeliveryAddressRequest addressRequest){
        Member member = CommonUtils.getMemberById(memberRepository,memberId);
        Address newAddress = addressRequest.toAddress(member);

        addressRepository.save(newAddress);
        return newAddress.getId();
    }

    private Address getDeliveryAddress(Member member, DeliveryRegisterRequest reqDelivery){
        Long addressId = 2L;
        if(reqDelivery.getAddressId() != null){
            Optional<Address> maybeAddress =
                    addressRepository.findById(addressId);
            return maybeAddress.orElse(null);
        }

        log.info("주문 전에 배송지를 등록해주세요.");
        return null;
    }

    private void registerDelivery(DeliveryRegisterRequest reqDelivery, OrderInfo myOrderInfo,
                                  Address myAddress){
        log.info("1차 확인!!!!" + myAddress.getId());
        Delivery myDelivery =
                reqDelivery.toDelivery(myAddress, myOrderInfo);
        log.info("2차 확인 :"+myDelivery.getDeliveryId());
        deliveryRepository.save(myDelivery);
    }

    @Override
    public void orderRegister(Long memberId, OrderInfoRegisterForm orderForm){
        // { 상품 카테고리, 상품 id, 상품 수량, 상품 가격 } 주문 list
        Member member = CommonUtils.getMemberById(memberRepository,memberId);

        // myOrderInfo 생성
        OrderInfo myOrderInfo = registerOrderInfo(member, orderForm.getTotalOrderPrice());

        // orderInfoState 저장
        registerOrderState(myOrderInfo);

        // payment 저장
        registerPayment(orderForm.getPaymentRequest(), myOrderInfo);

        // orderItem 분류 및 저장
        addOrderItemByCategory(orderForm.getOrderItemRegisterRequestList(), myOrderInfo);

        // 등록했던 배송지 address 반환
        Address myAddress = getDeliveryAddress(member, orderForm.getDeliveryRegisterRequest());

        // delivery 저장
        registerDelivery(orderForm.getDeliveryRegisterRequest(), myOrderInfo, myAddress);
    }

    private void addOrderItemByCategory(List<OrderItemRegisterRequest> orderItems, OrderInfo myOrderInfo){
        List<OrderItemRegisterRequest> productOrderItems = new ArrayList<>();
        List<OrderItemRegisterRequest> sideProductOrderItems = new ArrayList<>();
        List<OrderItemRegisterRequest> selfSaladOrderItems = new ArrayList<>();

        for(OrderItemRegisterRequest item : orderItems){
            switch (item.getItemCategoryType()) {
                case PRODUCT:
                    productOrderItems.add(item); break;
                case SIDE:
                    sideProductOrderItems.add(item); break;
                case SELF:
                    selfSaladOrderItems.add(item); break;
                default:
                    throw new IllegalArgumentException("존재하지 않는 상품 카테고리 입니다. : " + item.getItemCategoryType());
            }
        }
        if( ! productOrderItems.isEmpty()){
            addProductOrderItems( productOrderItems, myOrderInfo);
        }
        if( ! sideProductOrderItems.isEmpty()){
            addSideProductsOrderItems( sideProductOrderItems, myOrderInfo);
        }
        if( ! selfSaladOrderItems.isEmpty()){
            addSelfSaladOrderItems( selfSaladOrderItems, myOrderInfo);
        }
    }

    private void addProductOrderItems(List<OrderItemRegisterRequest> productItems, OrderInfo myOrderInfo) {

        Map<Long, Product> productMap = requireNonNull(checkProducts(productItems));

        List<ProductOrderItem> productOrderItemList = new ArrayList<>();

        for(OrderItemRegisterRequest orderItem : productItems){

            Product product = productMap.get(orderItem.getItemId());

            if(Objects.equals(orderItem.getItemId(), product.getProductId())){

                productOrderItemList.add(orderItem.toProductOrderItem(product, myOrderInfo));
            }
        }
        orderItemRepository.saveAll(productOrderItemList);
        log.info(productOrderItemList + " 상품을 주문상품에 추가하였습니다.");
    }

    private void addSideProductsOrderItems(List<OrderItemRegisterRequest> sideProductItems, OrderInfo myOrderInfo) {

        Map<Long, SideProduct> sideProductMap = requireNonNull(checkSideProducts(sideProductItems));

        List<SideProductOrderItem> sideProductOrderItems = new ArrayList<>();

        for(OrderItemRegisterRequest orderItem : sideProductItems){

            SideProduct sideProduct = sideProductMap.get(orderItem.getItemId());

            if(Objects.equals(orderItem.getItemId(), sideProduct.getSideProductId())){

                sideProductOrderItems.add(orderItem.toSideProductOrderItem(sideProduct, myOrderInfo));
            }
        }
        orderItemRepository.saveAll(sideProductOrderItems);
        log.info(sideProductOrderItems + " 상품을 주문상품에 추가하였습니다.");
    }

    private void addSelfSaladOrderItems(List<OrderItemRegisterRequest> selfSaladItems, OrderInfo myOrderInfo) {

        Map<Long, SelfSalad> selfSaladMap = requireNonNull(checkSelfSalad(selfSaladItems));

        List<SelfSaladOrderItem> selfSaladOrderItems = new ArrayList<>();

        for(OrderItemRegisterRequest orderItem : selfSaladItems){

            SelfSalad selfSalad = selfSaladMap.get(orderItem.getItemId());

            if(Objects.equals(orderItem.getItemId(), selfSalad.getId())){

                selfSaladOrderItems.add(orderItem.toSelfSaladOrderItem(selfSalad, myOrderInfo));
            }
        }
        orderItemRepository.saveAll(selfSaladOrderItems);
        log.info(selfSaladOrderItems + " 상품을 주문상품에 추가하였습니다.");
    }

    @Override
    public Boolean updateOrderState(Long orderId, OrderStateType orderStateType){
        Optional<OrderInfo> myOrderInfo = orderInfoRepository.findById(orderId);
        if(myOrderInfo.isPresent()){

            final OrderState updateState =
                    orderStateRepository.findByOrderStateType(orderStateType);

            final OrderInfoState orderInfoState =
                    orderInfoStateRepository.findByOrderInfo_id(orderId);

            orderInfoState.setOrderState(myOrderInfo.get(), updateState);
            orderInfoStateRepository.save(orderInfoState);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public List<OrderInfoListResponse> orderInfoListResponse(Long memberId){

        List<OrderInfo> orderInfoList = orderInfoRepository.findByMember_memberId(memberId);
        List<OrderInfoListResponse> responseList = new ArrayList<>();
        for(OrderInfo orderInfo : orderInfoList){
            responseList.add(
                    new OrderInfoListResponse(
                            orderInfo.getId(),
                            orderInfo.getPayment().getMerchant_uid(),
                            orderInfo.getPayment().getPaid_amount(),
                            orderInfo.getPayment().getPaid_at(),
                            orderInfo.getDelivery().getRecipient(),
                            orderInfo.getDelivery().getDeliveryMemo(),
                            orderInfo.getDelivery().getAddress().getZipcode(),
                            orderInfo.getDelivery().getAddress().getCity(),
                            orderInfo.getDelivery().getAddress().getStreet(),
                            orderInfo.getDelivery().getAddress().getAddressDetail(),
                            getOrderStateType(orderInfo.getOrderInfoState()),
                            getOrderItems(orderInfo.getOrderItems())
                    )
            );
        }
        return responseList;
    }

    private OrderStateType getOrderStateType(Set<OrderInfoState> orderInfoStates){
        OrderStateType orderStateType = null;
        for(OrderInfoState orderInfoState : orderInfoStates){
            orderStateType = orderInfoState.getOrderState().getOrderStateType();
        }
        return orderStateType;
    }

    private List<OrderItemListResponse> getOrderItems(List<OrderItem> orderItems){
        List<OrderItemListResponse> itemListResponses = new ArrayList<>();
        for(OrderItem orderItem : orderItems){
            itemListResponses.add(
                    new OrderItemListResponse(
                            orderItem.getId(),
                            orderItem.getQuantity(),
                            getOrderItemTitle(orderItem),
                            getOrderItemId(orderItem)
                    )
            );
        }
        return itemListResponses;
    }

    private String getOrderItemTitle(OrderItem orderItem){
        String itemTitle = null;
        if(orderItem instanceof ProductOrderItem){
            itemTitle = ((ProductOrderItem) orderItem).getProduct().getTitle();
        }else if(orderItem instanceof SideProductOrderItem){
            itemTitle = ((SideProductOrderItem) orderItem).getSideProduct().getTitle();
        }else if(orderItem instanceof SelfSaladOrderItem){
            itemTitle = ((SelfSaladOrderItem) orderItem).getSelfSalad().getTitle();
        }
        return itemTitle;
    }

    private Long getOrderItemId(OrderItem orderItem){
        Long itemId = null;
        if(orderItem instanceof ProductOrderItem){
            itemId = ((ProductOrderItem) orderItem).getProduct().getProductId();
        }else if(orderItem instanceof SideProductOrderItem){
            itemId = ((SideProductOrderItem) orderItem).getSideProduct().getSideProductId();
        }else if(orderItem instanceof SelfSaladOrderItem){
            itemId = ((SelfSaladOrderItem) orderItem).getSelfSalad().getId();
        }
        return itemId;
    }
}
