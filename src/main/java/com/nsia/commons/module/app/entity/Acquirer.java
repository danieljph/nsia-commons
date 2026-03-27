package com.nsia.commons.module.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.ZonedDateTime;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@SuppressWarnings("JpaDataSourceORMInspection")
@Builder @Getter @Setter @NoArgsConstructor @AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "acquirer")
public class Acquirer
{
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret", length = 256)
    private String clientSecret;

    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AcquirerStatus status;

    @Column(name = "created_date")
    private ZonedDateTime createdDate;

    @Column(name = "updated_date")
    private ZonedDateTime updatedDate;

    @PrePersist
    public void prePersist()
    {
        createdDate = ZonedDateTime.now();
    }
}
