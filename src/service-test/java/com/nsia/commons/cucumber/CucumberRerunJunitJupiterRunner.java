package com.nsia.commons.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectFile;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Suite
@IncludeEngines("cucumber")
@SelectFile("target/rerun.txt")
@ConfigurationParameter
(
    key = GLUE_PROPERTY_NAME,
    value = "com.nsia.commons.cucumber"
)
@ConfigurationParameter
(
    key = PLUGIN_PROPERTY_NAME,
    value = "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
)
public class CucumberRerunJunitJupiterRunner
{
}
