package org.example.flowcore.activity;

import org.example.flowcore.config.ActivityOptions;

import java.lang.reflect.Method;

@FunctionalInterface
public interface ActivityInvoker {

    Object invoke(
            Class<?> activityInterface,
            Method method,
            Object[] args,
            ActivityOptions options
    );
}
