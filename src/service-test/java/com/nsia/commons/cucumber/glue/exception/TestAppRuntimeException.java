package com.nsia.commons.cucumber.glue.exception;

import lombok.experimental.StandardException;

/**
 * Created to avoid Sonar code-smell when using pure Exception or RuntimeException
 *
 * @author Daniel Joi Partogi Hutapea
 */
@StandardException
public class TestAppRuntimeException extends RuntimeException
{
}
