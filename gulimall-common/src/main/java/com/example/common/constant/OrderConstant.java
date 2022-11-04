package com.example.common.constant;

/**
 * @Date: 2022/10/26 20:11
 */
public class OrderConstant {

    //redis中存放令牌的前缀，用于防止订单重复提交
    public static final String USER_ORDER_TOKEN_PREFIX = "order:token:";

    public enum  OrderStatusEnum {
        CREATE_NEW(0,"待付款"),
        PAYED(1,"已付款"),
        SENDED(2,"已发货"),
        RECIEVED(3,"已完成"),
        CANCLED(4,"已取消"),
        SERVICING(5,"售后中"),
        SERVICED(6,"售后完成");
        private Integer code;
        private String msg;

        OrderStatusEnum(Integer code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public Integer getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

}
