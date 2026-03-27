package com.nsia.commons.module.distributedlock.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.ExpirableLockRegistry;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@RequiredArgsConstructor
@Configuration
public class DistributedLockConfig
{
    private final DistributedLockProperties distributedLockProperties;

    /**
     * Starting with version 5.5.13, the RedisLockRegistry exposes a setRedisLockType(RedisLockType) option to determine in which mode a Redis lock acquisition should happen:
     * - RedisLockType.SPIN_LOCK - the lock is acquired by periodic loop (100ms) checking whether the lock can be acquired. Default.
     * - RedisLockType.PUB_SUB_LOCK - The lock is acquired by redis pub-sub subscription.
     * <br/>
     * The pub-sub is preferred mode - less network chatter between client Redis server, and more performant - the lock is acquired immediately when subscription is notified about unlocking in the other process.
     * However, the Redis does not support pub-sub in the Master/Replica connections (for example, in AWS ElastiCache environment); therefore, a busy-spin mode is chosen as a default to make the registry working in any environment.
     * <br/>
     * If the lock is not unlocked until distributedLockProperties.getReleaseTimeDuration().toMillis() reached, then this exception below will be thrown:
     * java.lang.IllegalStateException: Lock was released in the store due to expiration. The integrity of data protected by this lock may have been compromised.
     */
    @Bean
    public ExpirableLockRegistry expirableLockRegistry(RedisConnectionFactory redisConnectionFactory)
    {
        var redisLockRegistry = new RedisLockRegistry(
            redisConnectionFactory,
            distributedLockProperties.getRegistryKey(),
            distributedLockProperties.getReleaseTimeDuration().toMillis()
        );
        redisLockRegistry.setRedisLockType(distributedLockProperties.getRedisLockType());
        return redisLockRegistry;
    }
}
