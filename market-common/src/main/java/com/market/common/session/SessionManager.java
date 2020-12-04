package com.market.common.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author yjt
 * @since 2020/9/28 下午4:56
 */
public class SessionManager<T> implements Cache<String, T> {

    private final Cache<String, T> DELEGATE;

    public SessionManager(int initialCapacity) {
        DELEGATE = Caffeine.newBuilder()
                .expireAfter(new Expiry<String, T>() {
                    @Override
                    public long expireAfterCreate(@NonNull String key, @NonNull T value, long currentTime) {
                        return TimeUnit.SECONDS.toNanos(30) + currentTime;
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull String key, @NonNull T value, long currentTime, @NonNegative long currentDuration) {
                        return TimeUnit.SECONDS.toNanos(30) + currentTime;
                    }

                    @Override
                    public long expireAfterRead(@NonNull String key, @NonNull T value, long currentTime, @NonNegative long currentDuration) {
                        return TimeUnit.SECONDS.toNanos(30) + currentTime;
                    }
                })
                .initialCapacity(initialCapacity)
                .build();
    }

    @Nullable
    @Override
    public T getIfPresent(Object key) {
        return DELEGATE.getIfPresent(key);
    }

    public T putIfAbsent(Object key, T value) {
        return DELEGATE.asMap().computeIfAbsent(key.toString(), (k) -> value);
    }

    @Nullable
    @Override
    public T get(@NonNull String key, @NonNull Function<? super String, ? extends T> mappingFunction) {
        return DELEGATE.get(key, mappingFunction);
    }


    @Override
    public @NonNull Map<String, T> getAllPresent(@NonNull Iterable<?> keys) {
        return DELEGATE.getAllPresent(keys);
    }


    @Override
    public void put(@NonNull String key, @NonNull T value) {
        DELEGATE.put(key, value);
    }

    @Override
    public void putAll(@NonNull Map<? extends String, ? extends T> map) {
        DELEGATE.putAll(map);
    }


    @Override
    public void invalidate(@NonNull Object key) {
        DELEGATE.invalidate(key);
    }


    @Override
    public void invalidateAll(@NonNull Iterable<?> keys) {
        DELEGATE.invalidateAll(keys);
    }


    @Override
    public void invalidateAll() {
        DELEGATE.invalidateAll();
    }

    @Override
    public @NonNegative long estimatedSize() {
        return DELEGATE.estimatedSize();
    }


    @Override
    public @NonNull CacheStats stats() {
        return DELEGATE.stats();
    }


    @Override
    public @NonNull ConcurrentMap<String, T> asMap() {
        return DELEGATE.asMap();
    }


    @Override
    public void cleanUp() {
        DELEGATE.cleanUp();
    }


    @Override
    public @NonNull Policy<String, T> policy() {
        return DELEGATE.policy();
    }
}
