package com.nsia.commons.cucumber.config;

import com.nsia.commons.Application;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

import java.nio.charset.Charset;
import java.util.Collection;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@CucumberContextConfiguration
@ActiveProfiles("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class CucumberBaseIntegrationIT extends ContainerBase
{
    @Autowired public JdbcTemplate jdbcTemplate;

    @SneakyThrows
    @Before
    public void setUp(Scenario scenario)
    {
        executeSqlBaseOnSourceTagNames("@BeforeScenarioExecuteSql:", scenario.getSourceTagNames());
    }

    @After
    public void tearDown(Scenario scenario)
    {
        executeSqlBaseOnSourceTagNames("@AfterScenarioExecuteSql:", scenario.getSourceTagNames());
    }

    @SneakyThrows
    private void executeSqlBaseOnSourceTagNames(String prefix, Collection<String> sourceTagNames)
    {
        for(String sourceTagName : sourceTagNames)
        {
            if(sourceTagName.startsWith(prefix))
            {
                var sqlFilenames = sourceTagName
                    .replaceFirst(prefix, "")
                    .split(",");

                for(var sqlFilename : sqlFilenames)
                {
                    var sqlPath = String.format("db/%s", sqlFilename);
                    var sql = StreamUtils.copyToString(new ClassPathResource(sqlPath).getInputStream(), Charset.defaultCharset());

                    jdbcTemplate.execute(sql);
                    log.info("Success Execute: {}", sqlPath);
                }
            }
        }
    }
}
