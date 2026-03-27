package com.nsia.commons.cucumber.glue.text.lookup;

import com.nsia.commons.cucumber.glue.ScenarioManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * IMPORTANT:
 * Make sure all custom methods in this class using String as an argument type.
 * The custom method must convert the String to a specific type manually.
 *
 * @author Daniel Joi Partogi Hutapea
 */
@SuppressWarnings("unused")
@Slf4j
@Scope(io.cucumber.spring.CucumberTestContext.SCOPE_CUCUMBER_GLUE)
@Component("fn")
public class FnStringLookup implements AppStringLookup
{
    private static final DateTimeFormatter ISO_8601_DEFAULT_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Lazy @Autowired private ScenarioManager scenarioManager; // Must use @Lazy to avoid circular dependency.

    @Override
    public String lookup(String key)
    {
        var keyParts = key.split(":", 2);
        var methodName = keyParts[0];
        var argsRaw = keyParts.length==2? keyParts[1] : null;
        var args = new Object[0];

        if(argsRaw!=null)
        {
            args = argsRaw.split(",");
        }
        var result = ReflectionTestUtils.invokeMethod(this, methodName, args);
        return result==null? null : result.toString();
    }

    /**
     * Sample usage: "${fn:nowAsIso8601}"
     */
    public String nowAsIso8601()
    {
        return nowAsIso8601("0");
    }

    /**
     * Sample usage: "${fn:nowAsIso8601:3600}" or "${fn:nowAsIso8601: -3600}"
     * Make sure there is 1 <SPACE> after ":" if your parameter value contains "-".
     */
    public String nowAsIso8601(String plusSecondsFromNowAsString)
    {
        try
        {
            var plusSecondsFromNow = Long.parseLong(plusSecondsFromNowAsString.trim());
            var zdtNow = scenarioManager.<ZonedDateTime>getSharedVar(ScenarioManager.SHARED_VARS_ZDT_NOW);
            return ISO_8601_DEFAULT_DTF.format(zdtNow.plusSeconds(plusSecondsFromNow));
        }
        catch(Exception ex)
        {
            log.warn("Failed on method 'nowAsIso8601'. Cause: {}", ex.getMessage());
            return null; // Return null to make the ${foo} in the template not replaced.
        }
    }

    /**
     * Sample usage: "${fn:nowAsIso8601WithZone:Asia/Jakarta}"
     */
    public String nowAsIso8601WithZone(String zoneId)
    {
        return nowAsIso8601WithZone(zoneId, "0");
    }

    /**
     * Sample usage: "${fn:nowAsIso8601WithZone:Asia/Jakarta,3600}" or "${fn:nowAsIso8601WithZone:Asia/Jakarta,-3600}"
     * Make sure there is 1 <SPACE> after ":" if your parameter value contains "-".
     */
    public String nowAsIso8601WithZone(String zoneId, String plusSecondsFromNowAsString)
    {
        try
        {
            var plusSecondsFromNow = Long.parseLong(plusSecondsFromNowAsString.trim());
            var zdtNow = scenarioManager.<ZonedDateTime>getSharedVar(ScenarioManager.SHARED_VARS_ZDT_NOW);
            var zdtNowAtParamZone = zdtNow.withZoneSameInstant(ZoneId.of(zoneId));
            return ISO_8601_DEFAULT_DTF.format(zdtNowAtParamZone.plusSeconds(plusSecondsFromNow));
        }
        catch(Exception ex)
        {
            log.warn("Failed on method 'nowAsIso8601WithZone'. Cause: {}", ex.getMessage());
            return null; // Return null to make the ${foo} in the template not replaced.
        }
    }

    /**
     * Sample usage: "${fn:trim:    MY-VAlUE    }" or "${fn:trim:${another-expression}}"
     */
    public String trim(String value)
    {
        try
        {
            if(value.contains("${"))
            {
                var valueReplaced = scenarioManager.replaceTokens(value);

                if(value.equals(valueReplaced))
                {
                    // Return null to make the ${fn:trim:${another-expression}} in the template not replaced.
                    return null;
                }

                return StringUtils.trim(valueReplaced);
            }
            else
            {
                return StringUtils.trim(value);
            }
        }
        catch(Exception ex)
        {
            log.warn("Failed on method 'trim'. Cause: {}", ex.getMessage());
            return null; // Return null to make the ${foo} in the template not replaced.
        }
    }

    /**
     * Sample usage: "${fn:leftPad:MY-VAlUE,8}" or "${fn:leftPad:${another-expression},8}"
     */
    public String leftPad(String value, String sizeAsString)
    {
        try
        {
            var size = Integer.parseInt(sizeAsString.trim());

            if(value.contains("${"))
            {
                var valueReplaced = scenarioManager.replaceTokens(value);

                if(value.equals(valueReplaced))
                {
                    // Return null to make the ${fn:leftPad:${another-expression},8} in the template not replaced.
                    return null;
                }

                return StringUtils.leftPad(valueReplaced, size);
            }
            else
            {
                return StringUtils.leftPad(value, size);
            }
        }
        catch(Exception ex)
        {
            log.warn("Failed on method 'leftPad'. Cause: {}", ex.getMessage());
            return null; // Return null to make the ${foo} in the template not replaced.
        }
    }

    /**
     * Sample usage: "${fn:genRandomString:10}"
     */
    public String genRandomString(String lengthAsString)
    {
        try
        {
            var length = Integer.parseInt(lengthAsString.trim());
            return RandomStringUtils.secure().nextAlphanumeric(length);
        }
        catch(Exception ex)
        {
            log.warn("Failed on method 'genRandomString'. Cause: {}", ex.getMessage());
            return null; // Return null to make the ${foo} in the template not replaced.
        }
    }
}
