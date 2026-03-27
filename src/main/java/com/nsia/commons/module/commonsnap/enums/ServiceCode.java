package com.nsia.commons.module.commonsnap.enums;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@Getter
public enum ServiceCode
{
    UNKNOWN("99"),
    INQUIRY("24", "/**/v*/transfer-va/inquiry"),
    PAYMENT("25", "/**/v*/transfer-va/payment"),
    INQUIRY_STATUS("26", "/**/v*/transfer-va/status"),
    CREATE_VA("27", "/**/bi-snap-va/v*/transfer-va/create-va"),
    UPDATE_VA("28", "/**/bi-snap-va/v*/transfer-va/update-va"),
    DELETE_VA("31", "/**/bi-snap-va/v*/transfer-va/delete-va"),

    // This is custom Service-Code (currently not available in BI SNAP).
    FC_REFUND("00", "/**/v*/refund"), // FC-Team want to use serviceCode = 00 for REFUND, so we need to change the UNKNOWN serviceCode from 00 to 99.
    BCA_REFUND_STATUS("58", "/**/v*/transfer-va/refund/status");

    private final String code;
    private final String[] pathPatterns;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    ServiceCode(String code, String... pathPatterns)
    {
        this.code = code;
        this.pathPatterns = pathPatterns;
    }

    public static ServiceCode findByServletPath(String servletPath)
    {
        return Arrays.stream(ServiceCode.values())
            .filter(serviceCode -> serviceCode!=UNKNOWN)
            .filter(serviceCode -> serviceCode.pathPatterns!=null)
            .filter(serviceCode ->
                Arrays.stream(serviceCode.pathPatterns)
                    .anyMatch(pathPattern -> PATH_MATCHER.match(pathPattern, servletPath))
            )
            .findFirst()
            .orElseGet(()-> {
                log.error("ServiceCode not found for servlet-path = '{}'. We'll set ServiceCode = '{}'.", servletPath, ServiceCode.UNKNOWN.getCode());
                return ServiceCode.UNKNOWN;
            });
    }
}
