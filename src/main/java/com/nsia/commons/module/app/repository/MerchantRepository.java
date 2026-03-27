package com.nsia.commons.module.app.repository;

import com.nsia.commons.module.app.entity.Merchant;
import com.nsia.commons.module.app.entity.MerchantStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * @author Daniel Joi Partogi Hutapea
 */
public interface MerchantRepository extends CrudRepository<Merchant, Long>
{
    Optional<Merchant> findFirstByClientIdAndStatus(String clientId, MerchantStatus status);
}
