package com.nsia.commons.module.distributedlock;

import com.nsia.commons.module.distributedlock.config.DistributedLockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.integration.util.CheckedCallable;
import org.springframework.integration.util.CheckedRunnable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DistributedLock
{
    private final ExpirableLockRegistry lockRegistry;
    private final DistributedLockProperties distributedLockProperties;

    public Lock obtain(Object lockKey)
    {
        return lockRegistry.obtain(lockKey);
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
    public <E extends Throwable> void executeLocked(Object lockKey, Duration waitLockDuration, CheckedRunnable<E> runnable) throws E, InterruptedException, TimeoutException
    {
        executeLocked(lockKey, waitLockDuration,
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
    public <T, E extends Throwable> T executeLocked(Object lockKey, Duration waitLockDuration, CheckedCallable<T, E> callable) throws E, InterruptedException, TimeoutException
    {
        if(distributedLockProperties.isEnable())
        {
            log.info("Lock[{}] - Obtaining.", lockKey);
            Lock lock = obtain(lockKey);

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
