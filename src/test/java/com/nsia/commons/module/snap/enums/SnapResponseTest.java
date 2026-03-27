package com.nsia.commons.module.snap.enums;

import com.nsia.commons.module.commonsnap.enums.SnapResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Daniel Joi Partogi Hutapea
 */
class SnapResponseTest
{
    @Test
    void fromCode()
    {
        assertEquals("200XX00", SnapResponse.SUCCESSFUL.buildResponseCode());
    }
}
