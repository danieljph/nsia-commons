package com.nsia.commons.module.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@SuppressWarnings({"JpaDataSourceORMInspection", "DefaultAnnotationParam"})
@Builder @Getter @Setter @NoArgsConstructor @AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "register")
public class Register
{
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "invoice_number", nullable = false, length = 64)
    private String invoiceNumber;

    @Column(name = "virtual_account_number", nullable = false, length = 28)
    private String virtualAccountNumber;

    @Column(name = "virtual_account_name", nullable = false, length = 255)
    private String virtualAccountName;

    @Column(name = "virtual_account_email", nullable = true, length = 255)
    private String virtualAccountEmail;

    @Column(name = "virtual_account_phone", nullable = true, length = 30)
    private String virtualAccountPhone;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = true, length = 3)
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_info", columnDefinition = "JSONB")
    private Map<String, Object> additionalInfo;

    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RegisterStatus status;

    @Column(name = "created_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime createdDate;

    @Column(name = "updated_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime updatedDate;

    @PrePersist
    public void prePersist()
    {
        createdDate = ZonedDateTime.now();
    }
}
