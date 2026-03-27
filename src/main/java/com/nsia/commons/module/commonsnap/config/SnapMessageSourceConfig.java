package com.nsia.commons.module.commonsnap.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Configuration
public class SnapMessageSourceConfig
{
    public static MessageSource SNAP_MESSAGE_SOURCE;
    public static Locale LOCALE_id_ID = Locale.of("id", "ID");

    @Bean
    public MessageSource snapMessageSource()
    {
        var messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("snap.messages");
        SNAP_MESSAGE_SOURCE = messageSource;
        return messageSource;
    }
}
