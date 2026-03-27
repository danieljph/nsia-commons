package com.nsia.commons.module.distributedlockv2;

import com.nsia.commons.module.distributedlockv2.config.DistributedLockV2Properties;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.integration.util.CheckedCallable;
import org.springframework.integration.util.CheckedRunnable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DistributedLockV2
{
    private final DistributedLockV2Properties distributedLockV2Properties;
    private final RedisConnectionFactory redisConnectionFactory;

    private final Map<String, Config> mapOfProcessToConfig = new HashMap<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Config
    {
        private ExpirableLockRegistry lockRegistry;
        private boolean enable;
    }

    @PostConstruct
    public void postConstruct()
    {
        for(var it : distributedLockV2Properties.getMapOfDistributedLockV2Config().entrySet())
        {
            var config = it.getValue();

            var redisLockRegistry = new RedisLockRegistry(
                redisConnectionFactory,
                config.getRegistryKey(),
                config.getReleaseTimeDuration().toMillis()
            );

            redisLockRegistry.setRedisLockType(config.getRedisLockType());

            if(config.getSuitableProcesses()!=null)
            {
                for(String process : config.getSuitableProcesses())
                {
                    mapOfProcessToConfig.put(process, new Config(redisLockRegistry, config.isEnable()));
                }
            }
        }
    }

    public Lock obtain(String process, Object lockKey)
    {
        return mapOfProcessToConfig.get(process).getLockRegistry().obtain(lockKey);
    }

    /**
     * Perform the provided task when the lock for the key is locked.
     * @param lockKey the lock key to use
     * @param waitLockDuration the {@link Duration} for {@link Lock#tryLock(long, TimeUnit)}
     * @param runnable the {@link CheckedRunnable} to execute within a lock
     * @param <E> type of exception runnable throws
     * @throws InterruptedException from a lock operation
     * @throws TimeoutException when {@link Lock#tryLock(long, TimeUnit)} has elapsed
     */
    public <E extends Throwable> void executeLocked(String process, Object lockKey, Duration waitLockDuration, CheckedRunnable<E> runnable) throws E, InterruptedException, TimeoutException
    {
        executeLocked(process, lockKey, waitLockDuration,
            () ->
            {
                runnable.run();
                return null;
            }
        );
    }

    /**
     * Perform the provided task when the lock for the key is locked.
     * @param lockKey the lock key to use
     * @param waitLockDuration the {@link Duration} for {@link Lock#tryLock(long, TimeUnit)}
     * @param callable the {@link CheckedCallable} to execute within a lock
     * @param <E> type of exception callable throws
     * @throws InterruptedException from a lock operation
     * @throws TimeoutException when {@link Lock#tryLock(long, TimeUnit)} has elapsed
     */
    @SuppressWarnings("UnusedReturnValue")
    public <T, E extends Throwable> T executeLocked(String process, Object lockKey, Duration waitLockDuration, CheckedCallable<T, E> callable) throws E, InterruptedException, TimeoutException
    {
        if(mapOfProcessToConfig.get(process).isEnable())
        {
            log.info("Lock[{}] - Obtaining.", lockKey);
            Lock lock = obtain(process, lockKey);

            if(!lock.tryLock(waitLockDuration.toMillis(), TimeUnit.MILLISECONDS))
            {
                throw new TimeoutException("The lock [%s] was not acquired in time: %s".formatted(lockKey, waitLockDuration));
            }

            log.info("Lock[{}] - Obtained.", lockKey);

            try
            {
                return callable.call();
            }
            finally
            {
                log.info("Lock[{}] - Unlocking.", lockKey);
                lock.unlock(); // If process time of "callable.call()" > DistributedLockProperties.releaseTimeDuration, unlocking will throw java.lang.IllegalStateException: Lock was released in the store due to expiration. The integrity of data protected by this lock may have been compromised.
                log.info("Lock[{}] - Unlocked.", lockKey);
            }
        }
        else
        {
            log.warn("DistributedLock is disabled.");
            return callable.call();
        }
    }
}
