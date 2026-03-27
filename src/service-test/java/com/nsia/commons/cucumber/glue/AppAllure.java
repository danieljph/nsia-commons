package com.nsia.commons.cucumber.glue;

import io.qameta.allure.Allure;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter
public class AppAllure
{
    private final String stepName;

    @Getter(AccessLevel.NONE)
    private final Map<String, String> mapOfAttachment = new LinkedHashMap<>();

    public AppAllure(String stepName)
    {
        this.stepName = stepName;
    }

    public synchronized void addAttachment(String name, String content)
    {
        mapOfAttachment.put(name, Optional.ofNullable(content).orElse(""));
    }

    public synchronized void writeStep()
    {
        Allure.step(stepName, () -> mapOfAttachment.forEach(Allure::addAttachment));
    }
}
