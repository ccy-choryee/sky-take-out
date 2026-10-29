package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 订单
 */
@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
@Api(tags = "C端-订单接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Value("${sky.pay.mock}")
    private Boolean payMock;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO;
        if (payMock) {
            // 模拟支付模式
            log.info("【模拟支付模式开启】，订单号：{}", ordersPaymentDTO.getOrderNumber());
            orderPaymentVO = new OrderPaymentVO();
            orderPaymentVO.setNonceStr(UUID.randomUUID().toString().replace("-", ""));
            orderPaymentVO.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
            // 重点：packageStr带上mock标识，前端可以识别
            orderPaymentVO.setPackageStr("prepay_id=mock_" + ordersPaymentDTO.getOrderNumber());
            orderPaymentVO.setSignType("RSA");
            orderPaymentVO.setPaySign("mock_pay_sign");

            // 直接执行支付成功逻辑，修改订单状态
            orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
            log.info("模拟自动支付完成，订单号：{}", ordersPaymentDTO.getOrderNumber());
        } else {
            // 真实微信支付，原有逻辑保留不动
            orderPaymentVO = orderService.payment(ordersPaymentDTO);
            log.info("生成预支付交易单：{}", orderPaymentVO);
        }
        return Result.success(orderPaymentVO);
    }

    /**
     * 模拟支付成功回调接口（备用，测试用）
     * @param orderNumber 订单号
     * @return
     */
    @PostMapping("/mockPaySuccess")
    @ApiOperation("模拟支付成功（测试专用）")
    public Result mockPaySuccess(String orderNumber) {
        log.info("手动触发模拟支付成功，订单号：{}", orderNumber);
        orderService.paySuccess(orderNumber);
        return Result.success();
    }

    /**
     * 历史订单查询
     *
     * @param page
     * @param pageSize
     * @param status   订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> page(int page, int pageSize, Integer status) {
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
        return Result.success(pageResult);
    }
    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }



    /**
     * 用户取消订单
     *
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable("id") Long id) throws Exception {
        orderService.userCancelById(id);
        return Result.success();
    }

    /**
     * 再来一单
     *
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 用户催单
     *
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("用户催单")
    public Result reminder(@PathVariable("id") Long id) {
        orderService.reminder(id);
        return Result.success();
    }
}
