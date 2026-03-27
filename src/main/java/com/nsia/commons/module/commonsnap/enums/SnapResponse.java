package com.nsia.commons.module.commonsnap.enums;

import com.nsia.commons.module.common.util.CommonUtils;
import com.nsia.commons.module.commonsnap.config.SnapMessageSourceConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@Getter
public enum SnapResponse
{
    // HttpStatus.OK (200)
    SUCCESSFUL(HttpStatus.OK, "00", "Successful"),

    // HttpStatus.ACCEPTED (202)
    REQUEST_IN_PROGRESS(HttpStatus.ACCEPTED, "00", "Request In Progress"),

    // HttpStatus.BAD_REQUEST (400)
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "00", "Bad Request"),
    INVALID_FIELD_FORMAT(HttpStatus.BAD_REQUEST, "01", "Invalid Field Format {{param}}"), // {param} contains invalid format field names
    INVALID_MANDATORY_FIELD(HttpStatus.BAD_REQUEST, "02", "Invalid Mandatory Field {{param}}"), // {param} contains invalid mandatory field names

    // HttpStatus.UNAUTHORIZED (401)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "00", "Unauthorized. [{param}]"), // {param} contains reason
    INVALID_TOKEN_B2B(HttpStatus.UNAUTHORIZED, "01", "Invalid Token (B2B)"),
    INVALID_CUSTOMER_TOKEN(HttpStatus.UNAUTHORIZED, "02", "Invalid Customer Token"),
    TOKEN_NOT_FOUND_B2B(HttpStatus.UNAUTHORIZED, "03", "Token Not Found (B2B)"),
    CUSTOMER_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "04", "Customer Token Not Found"),

    // HttpStatus.FORBIDDEN (403)
    TRANSACTION_EXPIRED(HttpStatus.FORBIDDEN, "00", "Transaction Expired"),
    FEATURE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "01", "Feature Not Allowed [{param}]"), // {param} contains Reason
    EXCEEDS_TRANSACTION_AMOUNT_LIMIT(HttpStatus.FORBIDDEN, "02", "Exceeds Transaction Amount Limit"),
    SUSPECTED_FRAUD(HttpStatus.FORBIDDEN, "03", "Suspected Fraud"),
    ACTIVITY_COUNT_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "04", "Activity Count Limit Exceeded"),
    DO_NOT_HONOR(HttpStatus.FORBIDDEN, "05", "Do Not Honor"),
    FEATURE_NOT_ALLOWED_AT_THIS_TIME(HttpStatus.FORBIDDEN, "06", "Feature Not Allowed At This Time. [{param}]"), // {param} contains reason
    CARD_BLOCKED(HttpStatus.FORBIDDEN, "07", "Card Blocked"),
    CARD_EXPIRED(HttpStatus.FORBIDDEN, "08", "Card Expired"),
    DORMANT_ACCOUNT(HttpStatus.FORBIDDEN, "09", "Dormant Account"),
    NEED_TO_SET_TOKEN_LIMIT(HttpStatus.FORBIDDEN, "10", "Need To Set Token Limit"),
    OTP_BLOCKED(HttpStatus.FORBIDDEN, "11", "OTP Blocked"),
    OTP_LIFETIME_EXPIRED(HttpStatus.FORBIDDEN, "12", "OTP Lifetime Expired"),
    OTP_SENT_TO_CARDHOLDER(HttpStatus.FORBIDDEN, "13", "OTP Sent To Cardholder"),
    INSUFFICIENT_FUNDS(HttpStatus.FORBIDDEN, "14", "Insufficient Funds"),
    TRANSACTION_NOT_PERMITTED(HttpStatus.FORBIDDEN, "15", "Transaction Not Permitted.[{param}]", "Transaction Not Permitted. {param}"), // {param} contains reason
    SUSPEND_TRANSACTION(HttpStatus.FORBIDDEN, "16", "Suspend Transaction"),
    TOKEN_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "17", "Token Limit Exceeded"),
    INACTIVE_CARD_OR_ACCOUNT_OR_CUSTOMER(HttpStatus.FORBIDDEN, "18", "Inactive Card/Account/Customer"),
    MERCHANT_BLACKLISTED(HttpStatus.FORBIDDEN, "19", "Merchant Blacklisted"),
    MERCHANT_LIMIT_EXCEED(HttpStatus.FORBIDDEN, "20", "Merchant Limit Exceed"),
    SET_LIMIT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "21", "Set Limit Not Allowed"),
    TOKEN_LIMIT_INVALID(HttpStatus.FORBIDDEN, "22", "Token Limit Invalid"),
    ACCOUNT_LIMIT_EXCEED(HttpStatus.FORBIDDEN, "23", "Account Limit Exceed"),
    DUPLICATE_TRANSACTION_ID(HttpStatus.FORBIDDEN, "51", "Duplicate Transaction ID"), // Custom SNAP Response used in BNI.
    VIRTUAL_ACCOUNT_TRX_TYPE_DOES_NOT_MATCH_TOTAL_AMOUNT(HttpStatus.FORBIDDEN, "53", "Virtual Account Trx Type Does Not Match Total Amount"),  // Custom SNAP Response used in BNI
    TOTAL_AMOUNT_CANNOT_HAVE_DECIMAL_FRACTION(HttpStatus.FORBIDDEN, "54", "Total Amount Cannot Have Decimal Fraction"),  // Custom SNAP Response used in BNI
    VA_NUMBER_IS_IN_USE(HttpStatus.FORBIDDEN, "55", "VA Number Is In Use"),  // Custom SNAP Response used in BNI
    VIRTUAL_ACCOUNT_TRX_TYPE_NOT_SUPPORTED(HttpStatus.FORBIDDEN, "56", "Virtual Account Trx Type Not Supported For This Partner"), // Custom SNAP Response used in gtw-va-snap-bni (legacy SNAP).
    CURRENCY_IS_NOT_MATCH(HttpStatus.FORBIDDEN, "58", "Currency Is Not Match"), // Custom SNAP Response used in BNI.
    AMOUNT_CAN_NOT_BE_CHANGED(HttpStatus.FORBIDDEN, "59", "Amount Can Not Be Changed"), // Custom SNAP Response used in BNI.

    // HttpStatus.NOT_FOUND (404)
    INVALID_TRANSACTION_STATUS(HttpStatus.NOT_FOUND, "00", "Invalid Transaction Status"),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "01", "Transaction Not Found"),
    INVALID_ROUTING(HttpStatus.NOT_FOUND, "02", "Invalid Routing"),
    BANK_NOT_SUPPORTED_BY_SWITCH(HttpStatus.NOT_FOUND, "03", "Bank Not Supported By Switch"),
    TRANSACTION_CANCELLED(HttpStatus.NOT_FOUND, "04", "Transaction Cancelled"),
    MERCHANT_IS_NOT_REGISTERED_FOR_CARD_REGISTRATION_SERVICES(HttpStatus.NOT_FOUND, "05", "Merchant Is Not Registered For Card Registration Services"),
    NEED_TO_REQUEST_OTP(HttpStatus.NOT_FOUND, "06", "Need To Request OTP"),
    JOURNEY_NOT_FOUND(HttpStatus.NOT_FOUND, "07", "Journey Not Found"),
    INVALID_MERCHANT(HttpStatus.NOT_FOUND, "08", "Invalid Merchant"),
    NO_ISSUER(HttpStatus.NOT_FOUND, "09", "No Issuer"),
    INVALID_API_TRANSITION(HttpStatus.NOT_FOUND, "10", "Invalid API Transition"),
    INVALID_CARD_OR_ACCOUNT_OR_CUSTOMER(HttpStatus.NOT_FOUND, "11", "Invalid Card/Account/Customer [info]/Virtual Account"),
    INVALID_BILL_OR_VIRTUAL_ACCOUNT_WITH_REASON(HttpStatus.NOT_FOUND, "12", "Invalid Bill/Virtual Account [{param}]"), // {param} contains Reason
    INVALID_AMOUNT(HttpStatus.NOT_FOUND, "13", "Invalid Amount"),
    PAID_BILL(HttpStatus.NOT_FOUND, "14", "Paid Bill"),
    INVALID_OTP(HttpStatus.NOT_FOUND, "15", "Invalid OTP"),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "16", "Partner Not Found"),
    INVALID_TERMINAL(HttpStatus.NOT_FOUND, "17", "Invalid Terminal"),
    INCONSISTENT_REQUEST(HttpStatus.NOT_FOUND, "18", "Inconsistent Request"),
    INVALID_BILL_OR_VIRTUAL_ACCOUNT(HttpStatus.NOT_FOUND, "19", "Invalid Bill/Virtual Account"),

    // HttpStatus.METHOD_NOT_ALLOWED (405)
    REQUESTED_FUNCTION_IS_NOT_SUPPORTED(HttpStatus.METHOD_NOT_ALLOWED, "00", "Requested Function Is Not Supported"),
    REQUESTED_OPERATION_IS_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "01", "Requested Operation Is Not Allowed"),

    // HttpStatus.CONFLICT (409)
    CONFLICT(HttpStatus.CONFLICT, "00", "Conflict"),
    DUPLICATE_PARTNER_REFERENCE_NO(HttpStatus.CONFLICT, "01", "Duplicate partnerReferenceNo"),

    // HttpStatus.TOO_MANY_REQUESTS (429)
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "00", "Too Many Requests"),

    // HttpStatus.INTERNAL_SERVER_ERROR (500)
    GENERAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "00", "General Error"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "01", "Internal Server Error"),
    EXTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "02", "External Server Error"),

    // HttpStatus.GATEWAY_TIMEOUT (504)
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "00", "Timeout");

    private final HttpStatus httpStatus;
    private final String caseCode;
    private final String responseMessage;
    private final String[] responseMessageVariants;

    SnapResponse(HttpStatus httpStatus, String caseCode, String responseMessage, String... responseMessageVariants)
    {
        this.httpStatus = httpStatus;
        this.caseCode = caseCode;
        this.responseMessage = responseMessage;
        this.responseMessageVariants = responseMessageVariants;
    }

    @SuppressWarnings("unused")
    public String interpolateParam()
    {
        return interpolateParam("");
    }

    @SuppressWarnings("unused")
    public String interpolateParam(String param)
    {
        return getResponseMessage().replaceAll("\\{param}", param);
    }

    /**
     * If dokuErrorMessage already contains correct SNAP responseMessage,
     * then use that message instead of using default enum ResponseMessage.
     * <br/>
     * This is needed to make sure:
     * - dokuErrorMessage = Transaction Not Permitted.[This merchant is fraud.]
     * will be translated to:
     * - SNAP responseMessage = Transaction Not Permitted.[This merchant is fraud.]
     * instead of:
     * - SNAP responseMessage = Transaction Not Permitted.[]
     */
    @SuppressWarnings("unused")
    public String getResponseMessage(String dokuErrorMessage)
    {
        String responseMessageCustom = interpolateParam("");

        if(getResponseMessage().contains("{param}"))
        {
            List<String> listOfResponseMessageVariant = new ArrayList<>();
            listOfResponseMessageVariant.add(getResponseMessage());
            listOfResponseMessageVariant.addAll(Arrays.asList(getResponseMessageVariants()));

            for(String responseMessageVariant : listOfResponseMessageVariant)
            {
                String[] temp = responseMessageVariant.split("\\{param}");

                if(temp.length>0 && dokuErrorMessage.startsWith(temp[0]))
                {
                    responseMessageCustom = dokuErrorMessage;
                    break;
                }
            }
        }

        return responseMessageCustom;
    }

    public String getResponseMessage_en_US(String... params)
    {
        return getResponseMessage(Locale.US, params);
    }

    public String getResponseMessage_id_ID(String... params)
    {
        return getResponseMessage(SnapMessageSourceConfig.LOCALE_id_ID, params);
    }

    public String getResponseMessage(Locale locale, String... params)
    {
        return SnapMessageSourceConfig.SNAP_MESSAGE_SOURCE.getMessage(
            "rc."+buildResponseCode("XX"),
            params,
            interpolateParam(
                Optional.ofNullable(params)
                    .filter(it->it.length>0)
                    .map(it->it[0])
                    .orElse("")
            ),
            locale);
    }

    /**
     * Service Code sample:
     * - 27: Create VA
     * - 15: API Internal Account Inquiry
     *
     * @param serviceCode - SNAP service code
     * @return snapResponseCode - Formula: HTTP-Status + service-code + case-code
     */
    public String buildResponseCode(String serviceCode)
    {
        return httpStatus.value() + serviceCode + caseCode;
    }

    @SuppressWarnings("unused")
    public String buildResponseCode(ServiceCode serviceCode)
    {
        return buildResponseCode(serviceCode.getCode());
    }

    @SuppressWarnings("unused")
    public String buildResponseCode()
    {
        return httpStatus.value() + "XX" + caseCode;
    }

    @SuppressWarnings("unused")
    public String buildResponseCodeByServletPath(String servletPath)
    {
        return buildResponseCode(ServiceCode.findByServletPath(servletPath).getCode());
    }

    public boolean responseCodeEquals(ServiceCode serviceCode, String responseCode)
    {
        return buildResponseCode(serviceCode).equals(responseCode);
    }

    @SuppressWarnings("unused")
    public static HttpStatus getHttpStatusFromResponseCode(String responseCode)
    {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        if(StringUtils.isNotBlank(responseCode) && responseCode.length()>3)
        {
            try
            {
                int statusCode = Integer.parseInt(responseCode.substring(0, 3));
                httpStatus = HttpStatus.valueOf(statusCode);
            }
            catch(Exception ex)
            {
                log.warn("Failed to get HttpStatus from responseCode = {}. HttpStatus will be set to {}. Cause: {}", responseCode, httpStatus.value(), CommonUtils.toString(ex));
            }
        }

        return httpStatus;
    }

    @SuppressWarnings("unused")
    public static SnapResponse findByResponseCode(String responseCode)
    {
        SnapResponse snapResponse = null;

        if(StringUtils.isNotBlank(responseCode) && responseCode.length()==7)
        {
            String httpStatusCode = responseCode.substring(0, 3);
            String caseCode = responseCode.substring(5, 7);

            for(SnapResponse it : SnapResponse.values())
            {
                String httpStatusCodeEnum = String.valueOf(it.getHttpStatus().value());
                String caseCodeEnum = it.getCaseCode();

                if(httpStatusCodeEnum.equals(httpStatusCode) && caseCodeEnum.equals(caseCode))
                {
                    snapResponse = it;
                    break;
                }
            }
        }

        return snapResponse;
    }

    public static String replaceServiceCode(String responseCode, ServiceCode serviceCodeNew)
    {
        if(StringUtils.isNotBlank(responseCode) && responseCode.length()==7)
        {
            var httpStatusCode = responseCode.substring(0, 3);
            var caseCode = responseCode.substring(5, 7);
            return httpStatusCode + serviceCodeNew.getCode() + caseCode;
        }
        else
        {
            log.warn("Param responseCode = '{}' is not a valid SNAP responseCode. Process replace serviceCode will be skipped.", responseCode);
            return responseCode;
        }
    }

    public String toExceptionMessage(String customMessage)
    {
        return customMessage == null?
            "SnapResponse." + name():
            "SnapResponse." + name() + " - " + customMessage;
    }
}
