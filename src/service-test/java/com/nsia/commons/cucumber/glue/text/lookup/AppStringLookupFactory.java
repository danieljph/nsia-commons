package com.nsia.commons.cucumber.glue.text.lookup;

import lombok.RequiredArgsConstructor;
import org.apache.commons.text.lookup.StringLookup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@SuppressWarnings("unused")
@RequiredArgsConstructor
@Scope(io.cucumber.spring.CucumberTestContext.SCOPE_CUCUMBER_GLUE)
@Component
public class AppStringLookupFactory implements StringLookup
{
    private final Map<String, AppStringLookup> mapOfStringLookup;

    @Override
    public String lookup(String key)
    {
        var keyParts = key.split(":", 2);

        if(keyParts.length == 1)
        {
            return null; // Return null to make the ${foo} in the template not replaced.
        }

        var stringLookup = mapOfStringLookup.get(keyParts[0]);
        return stringLookup==null? null : stringLookup.lookup(keyParts[1]);
    }
}
