//
// MIT License
//
// Copyright (c) 2021 Vaishnav Anil
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//

package io.github.slimjar.injector;

import io.github.slimjar.injector.helper.InjectionHelper;
import io.github.slimjar.injector.helper.InjectionHelperFactory;
import io.github.slimjar.injector.loader.Injectable;
import io.github.slimjar.resolver.ResolutionResult;
import io.github.slimjar.resolver.data.Dependency;
import io.github.slimjar.resolver.data.DependencyData;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SimpleDependencyInjector implements DependencyInjector {
    private final InjectionHelperFactory injectionHelperFactory;
    private final List<Dependency> processingDependencies = Collections.synchronizedList(new ArrayList<>());

    public SimpleDependencyInjector(final InjectionHelperFactory injectionHelperFactory) {
        this.injectionHelperFactory = injectionHelperFactory;
    }

    @Override
    public void inject(final Injectable injectable, final DependencyData data, final Map<String, ResolutionResult> preResolvedResults) throws ReflectiveOperationException, NoSuchAlgorithmException, IOException, URISyntaxException {
        final InjectionHelper helper = injectionHelperFactory.create(data, preResolvedResults);
        injectDependencies(injectable, helper, data.dependencies());
    }

    // TODO -> Download dependencies in parallel then check the checksums after instead of during the download
    private void injectDependencies(final Injectable injectable, final InjectionHelper injectionHelper, final Collection<Dependency> dependencies) throws RuntimeException {
        dependencies.stream()
            .filter(dependency -> !injectionHelper.isInjected(dependency))
            .forEach(dependency -> {
                if (processingDependencies.contains(dependency)) return;
                processingDependencies.add(dependency);

                try {
                    final java.io.File depJar = injectionHelper.fetch(dependency);

                    if (depJar == null) return;

                    injectable.inject(depJar.toURI().toURL());
                    injectDependencies(injectable, injectionHelper, dependency.transitive());
                } catch (final IOException e) {
                    throw new InjectionFailedException(dependency, e);
                } catch (IllegalAccessException | InvocationTargetException | URISyntaxException e) {
                    e.printStackTrace();
                } catch (ReflectiveOperationException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                processingDependencies.remove(dependency);
            }
        );
    }


}
