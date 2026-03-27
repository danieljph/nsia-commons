package com.nsia.commons.module.app.repository;

import com.nsia.commons.module.app.entity.Register;
import com.nsia.commons.module.app.entity.RegisterStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * @author Daniel Joi Partogi Hutapea
 */
public interface RegisterRepository extends CrudRepository<Register, Long>
{
    Optional<Register> findFirstByVirtualAccountNumberAndStatus(String virtualAccountNumber, RegisterStatus status);

    boolean existsByVirtualAccountNumberAndStatus(String virtualAccountNumber, RegisterStatus status);
}
