package com.nsia.commons.module.app.repository;

import com.nsia.commons.module.app.entity.Acquirer;
import com.nsia.commons.module.app.entity.AcquirerStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * @author Daniel Joi Partogi Hutapea
 */
public interface AcquirerRepository extends CrudRepository<Acquirer, Long>
{
    Optional<Acquirer> findFirstByClientIdAndStatus(String clientId, AcquirerStatus status);
}
