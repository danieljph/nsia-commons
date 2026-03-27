package com.nsia.commons.cucumber.glue;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @RequiredArgsConstructor
@Scope(io.cucumber.spring.CucumberTestContext.SCOPE_CUCUMBER_GLUE)
public class DefaultHooks
{
    private final ScenarioManager scenarioManager;

    @Before
    public void setUp(Scenario scenario)
    {
        scenarioManager.setScenario(scenario);
        Allure.feature(scenarioManager.getFeatureName());
    }
}
