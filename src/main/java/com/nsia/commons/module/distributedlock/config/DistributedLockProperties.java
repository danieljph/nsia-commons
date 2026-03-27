package com.nsia.commons.module.distributedlock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.time.Duration;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@ConfigurationProperties(prefix = "distributed-lock")
@Configuration
public class DistributedLockProperties
{
    private boolean enable = true;

    /**
     * Starting with version 5.5.13, the RedisLockRegistry exposes a setRedisLockType(RedisLockType) option to determine in which mode a Redis lock acquisition should happen:
     * - RedisLockType.SPIN_LOCK - the lock is acquired by periodic loop (100ms) checking whether the lock can be acquired. Default.
     * - RedisLockType.PUB_SUB_LOCK - The lock is acquired by redis pub-sub subscription.
     * <br/>
     * The pub-sub is preferred mode - less network chatter between client Redis server, and more performant - the lock is acquired immediately when subscription is notified about unlocking in the other process.
     * However, the Redis does not support pub-sub in the Master/Replica connections (for example in AWS ElastiCache environment), therefore a busy-spin mode is chosen as a default to make the registry working in any environment.
     */
    private RedisLockRegistry.RedisLockType redisLockType = RedisLockRegistry.RedisLockType.PUB_SUB_LOCK;

    /**
     * This value will be used as a prefix lock-key.
     * e.g.:
     * - registryKey = va-consumer-cdc
     * - lock-key = dbName-tableName-primaryKey
     * - Then the full-key = va-consumer-cdc:dbName-tableName-primaryKey
     */
    private String registryKey;

    /*
     * Default value is defined here so unit-test & service-test can use this default value without needed to configure it on properties files.
     *
     * You can change this value by putting this property key below:
     * distributed-lock.releaseTimeDuration=120s
     *
     * Supported units are ns, us, ms, s, m, h and d for nanoseconds, microseconds, milliseconds, seconds, minutes, hours, and days, respectively.
     */

    /*
     * If the lock is unlocked after releaseTimeDuration reached, then this exception below will be thrown:
     * java.lang.IllegalStateException: Lock was released in the store due to expiration. The integrity of data protected by this lock may have been compromised.
     */
    private Duration releaseTimeDuration = Duration.ofSeconds(180);

    private Duration createVaWaitingTimeDuration = Duration.ofSeconds(120);
}
