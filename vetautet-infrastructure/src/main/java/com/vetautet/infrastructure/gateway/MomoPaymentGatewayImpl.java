package com.vetautet.infrastructure.gateway;

import com.vetautet.domain.gateway.MomoPaymentGateway;
import com.vetautet.domain.model.MomoCreatePaymentCommand;
import com.vetautet.domain.model.MomoCreatePaymentResult;
import com.vetautet.domain.model.MomoPaymentResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MomoPaymentGatewayImpl implements MomoPaymentGateway {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final RestTemplate restTemplate;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String endpoint;

    @Value("${momo.partner-code:}")
    private String partnerCode;

    @Value("${momo.access-key:}")
    private String accessKey;

    @Value("${momo.secret-key:}")
    private String secretKey;

    @Value("${momo.redirect-url:}")
    private String redirectUrl;

    @Value("${momo.ipn-url:}")
    private String ipnUrl;

    @Value("${momo.lang:vi}")
    private String lang;

    @Value("${momo.request-type:captureWallet}")
    private String requestType;

    public MomoPaymentGatewayImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    @CircuitBreaker(name = "momoPayment")
    public MomoCreatePaymentResult createPayment(MomoCreatePaymentCommand command) {
        validateConfig();

        String orderId = "BOOKING-" + command.getBookingId() + "-" + System.currentTimeMillis();
        String requestId = "REQ-" + command.getBookingId() + "-" + System.currentTimeMillis();
        String extraData = Base64.getEncoder().encodeToString(
                ("{\"bookingId\":" + command.getBookingId() + "}").getBytes(StandardCharsets.UTF_8)
        );

        String rawSignature = "accessKey=" + accessKey
                + "&amount=" + command.getAmount()
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + command.getOrderInfo()
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("requestId", requestId);
        body.put("amount", command.getAmount());
        body.put("orderId", orderId);
        body.put("orderInfo", command.getOrderInfo());
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("requestType", requestType);
        body.put("extraData", extraData);
        body.put("lang", lang);
        body.put("signature", hmacSha256(rawSignature, secretKey));

        Map<String, String> userInfo = new LinkedHashMap<>();
        putIfNotBlank(userInfo, "name", command.getCustomerName());
        putIfNotBlank(userInfo, "email", command.getCustomerEmail());
        putIfNotBlank(userInfo, "phoneNumber", command.getCustomerPhone());
        if (!userInfo.isEmpty()) {
            body.put("userInfo", userInfo);
        }

        MomoCreatePaymentResult response = restTemplate.postForObject(endpoint, body, MomoCreatePaymentResult.class);
        if (response == null) {
            throw new RuntimeException("MoMo did not return payment response");
        }
        return response;
    }

    @Override
    public boolean verifyPaymentResult(MomoPaymentResult result) {
        validateConfig();

        String rawSignature = "accessKey=" + accessKey
                + "&amount=" + value(result.getAmount())
                + "&extraData=" + value(result.getExtraData())
                + "&message=" + value(result.getMessage())
                + "&orderId=" + value(result.getOrderId())
                + "&orderInfo=" + value(result.getOrderInfo())
                + "&orderType=" + value(result.getOrderType())
                + "&partnerCode=" + value(result.getPartnerCode())
                + "&payType=" + value(result.getPayType())
                + "&requestId=" + value(result.getRequestId())
                + "&responseTime=" + value(result.getResponseTime())
                + "&resultCode=" + value(result.getResultCode())
                + "&transId=" + value(result.getTransId());

        String expectedSignature = hmacSha256(rawSignature, secretKey);
        return expectedSignature.equalsIgnoreCase(value(result.getSignature()));
    }

    private void validateConfig() {
        if (partnerCode.isBlank() || accessKey.isBlank() || secretKey.isBlank() || redirectUrl.isBlank() || ipnUrl.isBlank()) {
            throw new RuntimeException("MoMo sandbox config is missing. Please set MOMO_PARTNER_CODE, MOMO_ACCESS_KEY, MOMO_SECRET_KEY, MOMO_REDIRECT_URL, MOMO_IPN_URL.");
        }
    }

    private void putIfNotBlank(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create MoMo HMAC signature", e);
        }
    }
}
