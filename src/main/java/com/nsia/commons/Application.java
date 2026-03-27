package com.nsia.commons;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Optional;
import java.util.Properties;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@SpringBootApplication
(
	scanBasePackages =
	{
		"com.doku.au.security.module.encryption",
		"com.nsia.commons"
	}
)
public class Application
{
	public static void main(String[] args) throws Exception
	{
		startEmbeddedPostgres();
		SpringApplication.run(Application.class, args);
	}

	private static void startEmbeddedPostgres() throws Exception
	{
		var properties = new Properties();
		String resourceName = "application.properties";

		try(var inputStream = Application.class.getClassLoader().getResourceAsStream(resourceName))
		{
			if(inputStream == null)
			{
				throw new RuntimeException("Failed to load %s.".formatted(resourceName));
			}

			properties.load(inputStream);

			var isEmbeddedPostgresEnabled = Optional.ofNullable(properties.getProperty("app.embedded-postgres.enabled"))
				.map(Boolean::parseBoolean)
				.orElse(false);

			if(isEmbeddedPostgresEnabled)
			{
				var embeddedPostgres = EmbeddedPostgres.builder()
					.setPort(55432)
					.setCleanDataDirectory(true)
					.setRegisterShutdownHook(true)
					.start();

				log.info("Embedded Postgres started successfully with port {}.", embeddedPostgres.getPort());
			}
		}
	}
}
