package com.vetautet.infrastructure.gateway;

import com.vetautet.domain.gateway.VnpayPaymentGateway;
import com.vetautet.domain.model.VnpayCreatePaymentCommand;
import com.vetautet.domain.model.VnpayCreatePaymentResult;
import com.vetautet.domain.model.VnpayPaymentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Component
public class VnpayPaymentGatewayImpl implements VnpayPaymentGateway {

    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${vnpay.return-url:}")
    private String returnUrl;

    @Value("${vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vnpay.version:2.1.0}")
    private String version;

    @Value("${vnpay.command:pay}")
    private String command;

    @Value("${vnpay.order-type:billpayment}")
    private String orderType;

    @Value("${vnpay.locale:vn}")
    private String locale;

    @Value("${vnpay.curr-code:VND}")
    private String currCode;

    @Value("${vnpay.expire-minutes:15}")
    private long expireMinutes;

    @Override
    public VnpayCreatePaymentResult createPayment(VnpayCreatePaymentCommand command) {
        validateConfig();

        String txnRef = command.getBookingId() + String.valueOf(System.currentTimeMillis());
        long vnpAmount = Math.multiplyExact(command.getAmount(), 100L);
        LocalDateTime now = LocalDateTime.now(VNPAY_ZONE);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", version);
        params.put("vnp_Command", this.command);
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", currCode);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", command.getOrderInfo());
        params.put("vnp_OrderType", orderType);
        params.put("vnp_Locale", locale);
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp(command.getClientIp()));
        params.put("vnp_CreateDate", now.format(VNPAY_DATE_FORMAT));
        params.put("vnp_ExpireDate", now.plusMinutes(expireMinutes).format(VNPAY_DATE_FORMAT));

        if (command.getBankCode() != null && !command.getBankCode().isBlank()) {
            params.put("vnp_BankCode", command.getBankCode());
        }

        String hashData = buildHashData(params);
        String secureHash = hmacSha512(hashData, clean(hashSecret));
        String paymentUrl = clean(payUrl) + "?" + buildQuery(params) + "&vnp_SecureHash=" + secureHash;

        System.out.println(">>> [VNPAY] Create payment URL tmnCode=" + clean(tmnCode)
                + ", txnRef=" + txnRef
                + ", amount=" + vnpAmount
                + ", returnUrl=" + clean(returnUrl));
        System.out.println(">>> [VNPAY] Hash data=" + hashData);

        return VnpayCreatePaymentResult.builder()
                .txnRef(txnRef)
                .amount(command.getAmount())
                .orderInfo(command.getOrderInfo())
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    public boolean verifyPaymentResult(VnpayPaymentResult result) {
        validateConfig();

        Map<String, String> params = result.getParams();
        if (params == null || params.isEmpty()) {
            return false;
        }

        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        Map<String, String> hashParams = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key != null
                    && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)
                    && value != null) {
                hashParams.put(key, value);
            }
        });

        String hashData = buildHashData(hashParams);
        String expectedHash = hmacSha512(hashData, clean(hashSecret));
        System.out.println(">>> [VNPAY] Verify txnRef=" + params.get("vnp_TxnRef")
                + ", responseCode=" + params.get("vnp_ResponseCode")
                + ", transactionStatus=" + params.get("vnp_TransactionStatus"));
        System.out.println(">>> [VNPAY] Verify hash data=" + hashData);
        System.out.println(">>> [VNPAY] Verify expectedHash=" + expectedHash
                + ", receivedHash=" + secureHash);
        return expectedHash.equalsIgnoreCase(secureHash);
    }

    private void validateConfig() {
        if (clean(payUrl).isBlank() || clean(returnUrl).isBlank() || clean(tmnCode).isBlank() || clean(hashSecret).isBlank()) {
            throw new RuntimeException("VNPAY sandbox config is missing. Please set VNPAY_TMN_CODE, VNPAY_HASH_SECRET, VNPAY_RETURN_URL.");
        }
    }

    private String clientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "127.0.0.1";
        }
        if ("0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp)) {
            return "127.0.0.1";
        }
        return clientIp;
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        params.forEach((key, value) -> {
            if (value == null || value.isBlank()) {
                return;
            }
            if (hashData.length() > 0) {
                hashData.append('&');
            }
            hashData.append(key).append('=').append(encode(value));
        });
        return hashData.toString();
    }

    private String buildQuery(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        params.forEach((key, value) -> {
            if (value == null || value.isBlank()) {
                return;
            }
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(encode(key)).append('=').append(encode(value));
        });
        return query.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(clean(value), StandardCharsets.US_ASCII);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String hmacSha512(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create VNPAY HMAC signature", e);
        }
    }
}
