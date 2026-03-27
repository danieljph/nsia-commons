package com.nsia.commons.module.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class contains all common utils that not specified for any standard (e.g.: SNAP) and not need any Spring Bean dependencies.
 *
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
public class CommonUtils
{
    public static final ZoneId ZONE_ID_ASIA_JAKARTA = ZoneId.of("Asia/Jakarta");

    public static final String DEFAULT_ISO_4217_CURRENCY_CODE = "IDR";

    public static final DateTimeFormatter JDM_CLIENT_CREATION_TIME_DTF = DateTimeFormatter.ISO_DATE_TIME.withZone(ZONE_ID_ASIA_JAKARTA);
    public static final DateTimeFormatter DOKU_CORE_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(ZoneId.of("UTC"));

    public static final String DATE_FORMAT_PATTERN_DD_MM_YYYY = "dd-MM-yyyy";

    public static final String IDENTIFIER_NAME_BIN = "BIN";

    private static final int MINIMUM_NUMBER_OF_LOG_FROM_DOKU = 3;

    public static final int CUSTOMER_NAME_MAX_LENGTH = 128;

    private static final TriConsumer<List<Map<String, Object>>, String, Optional<String>> LIST_OF_CORE_ACQ_REFERENCE_ADDER;

    static
    {
        LIST_OF_CORE_ACQ_REFERENCE_ADDER = (listOfReference, refName, refItemOpt) ->
            refItemOpt.ifPresent(
                it -> listOfReference.add(
                    Map.of(
                        "name", refName,
                        "value", it
                    )
                )
            );
    }

    private CommonUtils()
    {
    }

    @SuppressWarnings("unused")
    public static String toString(Throwable th)
    {
        return toString(th, 10);
    }

    /**
     * This method will limit the log by printed only ${limit} lines per cause,
     * BUT will ensure at least MINIMUM_NUMBER_OF_LOG_FROM_DOKU exist on the log per cause.
     */
    @SuppressWarnings("java:S3776")
    public static String toString(Throwable th, int limit)
    {
        var result = new ArrayList<String>();

        do
        {
            result.add(th.getClass().getName() + ": " + th.getMessage());

            var arrayOfStackTraceElement = th.getStackTrace();
            var counterLogFromDoku = 0;

            for(int i=0; i<arrayOfStackTraceElement.length; i++)
            {
                var stackTrace = arrayOfStackTraceElement[i].toString();

                if(i<limit)
                {
                    result.add("\t" + stackTrace);

                    if(stackTrace.startsWith("com.doku"))
                    {
                        counterLogFromDoku++;
                    }
                }
                else
                {
                    if(counterLogFromDoku>=MINIMUM_NUMBER_OF_LOG_FROM_DOKU)
                    {
                        break;
                    }

                    if(stackTrace.startsWith("com.doku"))
                    {
                        counterLogFromDoku++;
                        result.add("\t" + stackTrace + String.format(" [%d]", i));
                    }
                }
            }

            th = th.getCause();
        }
        while(th!=null);

        return String.join("\n", result);
    }

    /**
     * Sample input -> output:
     * - null   -> null
     * - 1      -> *
     * - 12     -> *2
     * - 123    -> **3
     * - 1234   -> **34
     * - 12345  -> ***45
     * - 123456 -> ***456
     */
    @SuppressWarnings("unused")
    public static String maskHalf(String originalValue)
    {
        if(originalValue==null)
        {
            return null;
        }

        var originalValueLength = originalValue.length();
        var lengthOfMaskChar = originalValueLength % 2 == 0? originalValueLength/2 : originalValueLength/2 + 1;
        var overlay = StringUtils.repeat('*', lengthOfMaskChar);
        return StringUtils.overlay(originalValue, overlay, 0, lengthOfMaskChar);
    }

    public static TriConsumer<List<Map<String, Object>>, String, Optional<String>> getListOfCoreAcqReferenceAdder()
    {
        return LIST_OF_CORE_ACQ_REFERENCE_ADDER;
    }

    /**
     * Convert ZonedDateTime to format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX
     */
    public static String toIso8601(ZonedDateTime zdt)
    {
        return Optional.ofNullable(zdt)
            .map(it -> it.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .orElse(null);
    }

    /**
     * Convert ZonedDateTime to format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX with ZoneId.of("UTC")
     * Sample Output: 2023-12-15T05:01:02.000Z
     */
    public static String toCoreIso8601(ZonedDateTime zdt)
    {
        return zdt.format(DOKU_CORE_DTF);
    }

    public static ZonedDateTime toZonedDateTime(String formattedDate, SimpleDateFormat sdf)
    {
        return toZonedDateTime(formattedDate, sdf, null);
    }

    public static ZonedDateTime toZonedDateTime(String formattedDate, SimpleDateFormat sdf, ZonedDateTime defaultValue)
    {
        try
        {
            if(formattedDate==null || sdf==null)
            {
                log.warn("Param 'formattedDate' and 'sdf' usually must not be null.");
                return defaultValue;
            }

            var date = sdf.parse(formattedDate);
            var zdt = date.toInstant().atZone(ZoneId.systemDefault());
            log.debug("toZonedDateTime result: {}", zdt);
            return zdt;
        }
        catch(ParseException ex)
        {
            log.warn("Failed to parse formattedDate to ZonedDateTime. Cause: {}", ex.getMessage());
            return defaultValue;
        }
    }

    public static String toJdmClientCreationTime(LocalDateTime ldt)
    {
        return toJdmClientCreationTime(ldt==null? null : ldt.atZone(ZONE_ID_ASIA_JAKARTA));
    }

    /**
     * Important:
     * - PostgreSQL only support until microseconds precision.
     * - ZonedDateTime saved to PostgreSQL must be normalized to microseconds precision.
     * - JPA (With Hibernate Implementation) has rounding behaviour like this below when saving to PostgreSQL timestamp with zone:
     * -- 2024-12-25T00:00:00.000000001+07:00 -> 2024-12-25T00:00:00.000000+07:00
     * -- 2024-12-25T00:00:00.000000499+07:00 -> 2024-12-25T00:00:00.000000+07:00
     * -- 2024-12-25T00:00:00.000000500+07:00 -> 2024-12-25T00:00:00.000001+07:00
     * -- 2024-12-25T00:00:00.000000999+07:00 -> 2024-12-25T00:00:00.000001+07:00
     * -- 2024-12-25T00:00:00.999999499+07:00 -> 2024-12-25T00:00:00.999999+07:00
     * -- 2024-12-25T00:00:00.999999500+07:00 -> 2024-12-25T00:00:01.000000+07:00
     * -- 2024-12-25T00:00:00.999999999+07:00 -> 2024-12-25T00:00:01.000000+07:00
     * -- 2024-12-25T00:00:01.999999999+07:00 -> 2024-12-25T00:00:02.000000+07:00
     */
    public static String toJdmClientCreationTime(ZonedDateTime zdt)
    {
        if(zdt == null)
        {
            return null;
        }

        var nanoseconds = zdt.getNano();
        var microseconds = (int) Math.round(nanoseconds/1000.0); // Why do we need to use Math.round? Check this method doc on "Important" section.
        var nanosecondsNormalized = microseconds * 1000;

        var zdtNormalized = zdt
                .withNano(0) // Reset nanoseconds to zero.
                .plusNanos(nanosecondsNormalized); // And then added nanosecondsNormalized to existing ZoneDateTime so if nanosecondsNormalized > 999_999_999, it will be added 1 second to Zdt.

        return JDM_CLIENT_CREATION_TIME_DTF.format(zdtNormalized);
    }
}
