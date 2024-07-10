package com.verifiablecredentials.javaaadvcapiidtokenhint.helpers;
import com.github.benmanes.caffeine.cache.*;
import java.util.concurrent.TimeUnit;

public class CacheHelper {
    private static Cache<String, String> cache = Caffeine.newBuilder()
                                            .expireAfterWrite(15, TimeUnit.MINUTES)
                                            .maximumSize(100)
                                            .build();

    public static Cache<String, String> getCache() {
        return cache;
    }
} // cls