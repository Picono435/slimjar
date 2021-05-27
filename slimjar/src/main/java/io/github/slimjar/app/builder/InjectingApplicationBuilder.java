package io.github.slimjar.app.builder;

import io.github.slimjar.app.AppendingApplication;
import io.github.slimjar.app.Application;
import io.github.slimjar.injector.DependencyInjector;
import io.github.slimjar.injector.loader.Injectable;
import io.github.slimjar.injector.loader.WrappedInjectableClassLoader;
import io.github.slimjar.resolver.data.DependencyData;
import io.github.slimjar.resolver.reader.DependencyDataProvider;
import io.github.slimjar.resolver.reader.DependencyDataProviderFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.security.NoSuchAlgorithmException;

public final class InjectingApplicationBuilder extends ApplicationBuilder {
    private final Injectable classLoader;

    public InjectingApplicationBuilder(final String applicationName, final Injectable classLoader) {
        super(applicationName);
        this.classLoader = classLoader;
    }

    @Override
    public Application build() throws IOException, ReflectiveOperationException, URISyntaxException, NoSuchAlgorithmException {
        final DependencyDataProvider dataProvider = getDataProviderFactory().create(getDependencyFileUrl());
        final DependencyData dependencyData = dataProvider.get();
        final DependencyInjector dependencyInjector = createInjector();
        dependencyInjector.inject(classLoader, dependencyData);
        return new AppendingApplication();
    }

    public static ApplicationBuilder createAppending(final String applicationName) {
        final String version = Runtime.class.getPackage().getSpecificationVersion();
        final String[] parts = version.split("\\.");
        if (parts.length < 2) {
            throw new IllegalStateException("Could not find proper JVM version! Found " + version);
        }
        final int major = Integer.parseInt(parts[0]);
        final int minor = Integer.parseInt(parts[1]);
        final Injectable injectable;
        if (major > 1 || minor > 8) {
            injectable = createInstrumentationInjectable();
        } else {
            injectable = new WrappedInjectableClassLoader((URLClassLoader) ApplicationBuilder.class.getClassLoader());
        }
        return new InjectingApplicationBuilder(applicationName, injectable);
    }

    private static Injectable createInstrumentationInjectable() {
        return null;
    }
}

