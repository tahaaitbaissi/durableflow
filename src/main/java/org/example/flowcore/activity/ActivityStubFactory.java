
package org.example.flowcore.activity;

import org.example.flowcore.config.ActivityOptions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

public final class ActivityStubFactory {

    private final ActivityInvoker invoker;

    public ActivityStubFactory(ActivityInvoker invoker) {
        this.invoker = Objects.requireNonNull(invoker);
    }

    public <T> T create(
            Class<T> activityInterface,
            ActivityOptions options) {

        Objects.requireNonNull(activityInterface);
        Objects.requireNonNull(options);

        if (!activityInterface.isInterface()) {
            throw new IllegalArgumentException(
                    "Activity type must be an interface: "
                            + activityInterface.getName()
            );
        }

        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if (method.isDefault()) {
                throw new UnsupportedOperationException(
                        "Default activity methods are not supported: "
                                + method.getName()
                );
            }

            return invoker.invoke(
                    activityInterface,
                    method,
                    args == null ? new Object[0] : args,
                    options
            );
        };

        Object proxy = Proxy.newProxyInstance(
                activityInterface.getClassLoader(),
                new Class<?>[]{activityInterface},
                handler
        );

        return activityInterface.cast(proxy);
    }

    private Object handleObjectMethod(
            Object proxy,
            Method method,
            Object[] args) {

        return switch (method.getName()) {
            case "toString" ->
                    "ActivityStub[" + proxy.getClass().getInterfaces()[0].getName() + "]";
            case "hashCode" ->
                    System.identityHashCode(proxy);
            case "equals" ->
                    proxy == args[0];
            default ->
                    throw new UnsupportedOperationException(
                            "Unsupported Object method: " + method.getName()
                    );
        };
    }
}
