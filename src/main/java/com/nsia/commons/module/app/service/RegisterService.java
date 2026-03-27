package com.nsia.commons.module.app.service;

import com.nsia.commons.module.app.dto.CreateVaRequest;
import com.nsia.commons.module.app.dto.CreateVaResponse;
import com.nsia.commons.module.app.entity.MerchantStatus;
import com.nsia.commons.module.app.entity.Register;
import com.nsia.commons.module.app.entity.RegisterStatus;
import com.nsia.commons.module.app.exception.MerchantException;
import com.nsia.commons.module.app.repository.MerchantRepository;
import com.nsia.commons.module.app.repository.RegisterRepository;
import com.nsia.commons.module.common.model.Srw;
import com.nsia.commons.module.common.util.CommonUtils;
import com.nsia.commons.module.commonsnap.enums.SnapResponse;
import com.nsia.commons.module.distributedlock.DistributedLock;
import com.nsia.commons.module.distributedlock.config.DistributedLockProperties;
import com.nsia.commons.module.httpclient.resttemplatefactory.RestTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RegisterService
{
    private final DistributedLock distributedLock;
    private final DistributedLockProperties distributedLockProperties;

    private final MerchantRepository merchantRepository;
    private final RegisterRepository registerRepository;

    private final RestTemplateFactory restTemplateFactory;

    @SneakyThrows
    @Transactional(rollbackFor = Exception.class)
    public Srw<CreateVaResponse> createVa(String xPartnerId, CreateVaRequest request)
    {
        var lockKey = request.getVirtualAccountNo();
        return distributedLock.executeLocked(
            lockKey,
            distributedLockProperties.getCreateVaWaitingTimeDuration(),
            () -> createVaInLock(xPartnerId, request)
        );
    }

    private Srw<CreateVaResponse> createVaInLock(String xPartnerId, CreateVaRequest request)
    {
        try
        {
            var googleResponse = restTemplateFactory.getForEntity("https://www.google.com?q=test-1", String.class);
        }
        catch(Exception ex)
        {
            log.error("Failed to connect to google.com. Cause:\n{}", CommonUtils.toString(ex));
        }

        var merchant = merchantRepository.findFirstByClientIdAndStatus(xPartnerId, MerchantStatus.ACTIVE)
            .orElseThrow(() -> new MerchantException(SnapResponse.INVALID_MERCHANT));

        var registerExist = registerRepository.existsByVirtualAccountNumberAndStatus(request.getVirtualAccountNo(), RegisterStatus.ACTIVE);

        if(registerExist)
        {
            throw new MerchantException(SnapResponse.VA_NUMBER_IS_IN_USE);
        }

        var register = Register.builder()
            .merchant(merchant)
            .invoiceNumber(request.getInvoiceNumber())
            .virtualAccountNumber(request.getVirtualAccountNo())
            .virtualAccountName(request.getVirtualAccountName())
            .virtualAccountEmail(request.getVirtualAccountEmail())
            .virtualAccountPhone(request.getVirtualAccountPhone())
            .amount(new BigDecimal(request.getAmount().getValue()))
            .currency(request.getAmount().getCurrency())
            .additionalInfo(request.getAdditionalInfo())
            .status(RegisterStatus.ACTIVE)
            .build();

        registerRepository.save(register);

        var snapResponse = SnapResponse.SUCCESSFUL;

        return Srw.<CreateVaResponse>builder()
            .httpStatus(snapResponse.getHttpStatus())
            .body(CreateVaResponse.builder()
                .responseCode(snapResponse.buildResponseCode())
                .responseMessage(snapResponse.getResponseMessage())
                .virtualAccountData(CreateVaResponse.VirtualAccountData.builder()
                    .invoiceNumber(request.getInvoiceNumber())
                    .virtualAccountNo(request.getVirtualAccountNo())
                    .virtualAccountName(request.getVirtualAccountName())
                    .virtualAccountEmail(request.getVirtualAccountEmail())
                    .virtualAccountPhone(request.getVirtualAccountPhone())
                    .amount(request.getAmount())
                    .additionalInfo(request.getAdditionalInfo())
                    .build()
                )
                .build()
            )
            .build();
    }
}
