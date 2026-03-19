
/**
 * Redis cache configuration for user data.
 * Activation: Only active when the remote-cache Spring profile is enabled.
 * Usage: Provides a Redis-backed CacheManager bean for Spring's caching abstraction, with a 30-minute TTL and JSON serialization for cache values.
 * Consumers: Application code does not use this class directly. It is used by Spring's caching infrastructure to support @Cacheable, @CacheEvict, etc. 
 * in adapters such as UserDirectoryAdapter and UserManagementAdapter (see USERS_CACHE).
 */
package com.agentscustomerstickets.users.infra;

import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@Configuration
@EnableCaching
@Profile("remote-cache")
class RedisCacheConfig implements CachingConfigurer {

      private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

      static final String USERS_CACHE = "users";

      /**
       * Configures and provides a Redis-backed RedisCacheManager bean for Spring's caching abstraction.
       * Behavior: Sets a 30-minute TTL for all cache entries and uses JSON serialization for cache values.
       * Consumers: Used automatically by Spring when the remote-cache profile is active and a RedisConnectionFactory is present. Enables caching for components using @Cacheable, @CacheEvict, etc. (not called directly by application code).
       * Relevant caches: The USERS_CACHE is referenced by adapters such as UserDirectoryAdapter and UserManagementAdapter.
       * @param connectionFactory the Redis connection factory
       * @return a configured RedisCacheManager
       */
      @Bean
      @ConditionalOnBean(RedisConnectionFactory.class)
      @NonNull
      RedisCacheManager cacheManager(@NonNull RedisConnectionFactory connectionFactory) {
            Duration ttl = Objects.requireNonNull(Duration.ofMinutes(30));
            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(ttl).serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
            return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
      }

      /**
       * Provides a custom CacheErrorHandler bean that logs warnings when cache operations fail.
       * Behavior: Prevents cache errors from propagating and breaking business logic by logging warnings for get, put, evict, and clear failures.
       * Consumers: Used automatically by Spring's cache infrastructure for all cache operations managed by the configured RedisCacheManager. Not called directly by application code.
       * @return a CacheErrorHandler that logs cache operation failures
       */
      @Override
      @Bean
      public @NonNull CacheErrorHandler errorHandler() {
            return new CacheErrorHandler() {
                  @Override
                  public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                        log.warn("Cache get failed for cache={} key={}", cache != null ? cache.getName() : "unknown", key, exception);
                  }

                  @Override
                  public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key,
                              @Nullable Object value) {
                        log.warn("Cache put failed for cache={} key={}", cache != null ? cache.getName() : "unknown", key, exception);
                  }

                  @Override
                  public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                        log.warn("Cache evict failed for cache={} key={}", cache != null ? cache.getName() : "unknown", key, exception);
                  }

                  @Override
                  public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                        log.warn("Cache clear failed for cache={}", cache != null ? cache.getName() : "unknown", exception);
                  }
            };
      }
}
