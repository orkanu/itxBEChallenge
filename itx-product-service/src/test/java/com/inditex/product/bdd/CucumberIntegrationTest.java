package com.inditex.product.bdd;

import org.junit.platform.suite.api.*;

import static io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.inditex.product.bdd")
@ConfigurationParametersResource("cucumber.properties")
public class CucumberIntegrationTest {
}
