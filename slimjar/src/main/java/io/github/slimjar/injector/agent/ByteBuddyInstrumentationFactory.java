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

package io.github.slimjar.injector.agent;

import io.github.slimjar.app.builder.ApplicationBuilder;
import io.github.slimjar.app.module.ModuleExtractor;
import io.github.slimjar.app.module.TemporaryModuleExtractor;
import io.github.slimjar.injector.loader.InjectableClassLoader;
import io.github.slimjar.injector.loader.InstrumentationInjectable;
import io.github.slimjar.injector.loader.IsolatedInjectableClassLoader;
import io.github.slimjar.injector.loader.manifest.JarManifestGenerator;
import io.github.slimjar.relocation.JarFileRelocator;
import io.github.slimjar.relocation.PassthroughRelocator;
import io.github.slimjar.relocation.RelocationRule;
import io.github.slimjar.relocation.Relocator;
import io.github.slimjar.relocation.facade.JarRelocatorFacadeFactory;
import io.github.slimjar.resolver.data.Dependency;
import io.github.slimjar.resolver.data.DependencyData;
import io.github.slimjar.resolver.data.Repository;
import io.github.slimjar.util.Packages;
import sun.management.VMManagement;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.UUID;

public final class ByteBuddyInstrumentationFactory implements InstrumentationFactory {
    public static final String AGENT_JAR = "loader-agent.isolated-jar";
    private static final String AGENT_PACKAGE = "io#github#slimjar#injector#agent";
    private static final String AGENT_CLASS = "ClassLoaderAgent";
    private static final String BYTE_BUDDY_AGENT_CLASS = "net#bytebuddy#agent#ByteBuddyAgent";

    private final URL agentJarUrl;
    private final ModuleExtractor extractor;
    private final JarRelocatorFacadeFactory relocatorFacadeFactory;

    public ByteBuddyInstrumentationFactory(final ApplicationBuilder applicationBuilder, final JarRelocatorFacadeFactory relocatorFacadeFactory) {
        this.relocatorFacadeFactory = relocatorFacadeFactory;
        URL agentJarUrlTemp;
        agentJarUrlTemp = InstrumentationInjectable.class.getClassLoader().getResource(AGENT_JAR);
        if(agentJarUrlTemp.getProtocol().equals("modjar")) {
            try {
                agentJarUrlTemp = new URL("jar:" + applicationBuilder.getJarURL().toString() + "!/" + AGENT_JAR);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        this.agentJarUrl = agentJarUrlTemp;
        this.extractor = new TemporaryModuleExtractor();
    }


    public ByteBuddyInstrumentationFactory(final URL agentJarUrl, final ModuleExtractor extractor, final JarRelocatorFacadeFactory relocatorFacadeFactory) {
        this.agentJarUrl = agentJarUrl;
        this.extractor = extractor;
        this.relocatorFacadeFactory = relocatorFacadeFactory;
    }

    @Override
    public Instrumentation create() throws IOException, ReflectiveOperationException, URISyntaxException, NoSuchAlgorithmException, InterruptedException {
        final URL extractedURL = extractor.extractModule(agentJarUrl, "loader-agent");

        final String pattern = generatePattern();
        final String relocatedAgentClass = String.format("%s.%s", pattern, AGENT_CLASS);
        final RelocationRule relocationRule = new RelocationRule(Packages.fix(AGENT_PACKAGE), pattern, Collections.emptySet(), Collections.emptySet());
        final Relocator relocator = new JarFileRelocator(Collections.singleton(relocationRule), relocatorFacadeFactory);
        final File inputFile = new File(extractedURL.toURI());
        final File relocatedFile = File.createTempFile("slimjar-agent", ".jar");

        final InjectableClassLoader classLoader = new IsolatedInjectableClassLoader();
        relocator.relocate(inputFile, relocatedFile);

        JarManifestGenerator.with(relocatedFile.toURI())
                .attribute("Manifest-Version", "1.0")
                .attribute("Agent-Class", relocatedAgentClass)
                .generate();

        ApplicationBuilder.injecting("SlimJar-Agent", classLoader)
                .dataProviderFactory((dataUrl) -> ByteBuddyInstrumentationFactory::getDependency)
                .relocatorFactory((rules) -> new PassthroughRelocator())
                .relocationHelperFactory((rel) -> (dependency, file) -> file)
                .build();

        final Class<?> byteBuddyAgentClass = Class.forName(Packages.fix(BYTE_BUDDY_AGENT_CLASS), true, classLoader);
        final Method attachMethod = byteBuddyAgentClass.getMethod("attach", File.class, String.class, String.class);

        Long processId;
        try {
            final Class<?> processHandle = Class.forName("java.lang.ProcessHandle");
            final Method currentMethod = processHandle.getMethod("current");
            final Method pidMethod = processHandle.getMethod("pid");
            final Object currentProcess = currentMethod.invoke(processHandle);
            processId = (Long) pidMethod.invoke(currentProcess);
        } catch (Exception exception) {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            Field jvm = runtime.getClass().getDeclaredField("jvm");
            jvm.setAccessible(true);
            VMManagement management = (VMManagement) jvm.get(runtime);
            Method method = management.getClass().getDeclaredMethod("getProcessId");
            method.setAccessible(true);
            processId = Long.valueOf((Integer) method.invoke(management));
        }

        attachMethod.invoke(null, relocatedFile, String.valueOf(processId), "");

        final Class<?> agentClass = Class.forName(relocatedAgentClass, true, ClassLoader.getSystemClassLoader());
        final Method instrMethod = agentClass.getMethod("getInstrumentation");
        return (Instrumentation) instrMethod.invoke(null);
    }


    private static DependencyData getDependency() throws MalformedURLException {
        final Dependency byteBuddy = new Dependency(
                "net.bytebuddy",
                "byte-buddy-agent",
                "1.11.0",
                null,
                Collections.emptyList()
        );
        final Repository centralRepository = new Repository(new URL(Repository.CENTRAL_URL));
        return new DependencyData(
                Collections.emptySet(),
                Collections.singleton(centralRepository),
                Collections.singleton(byteBuddy),
                Collections.emptyList()
        );
    }

    private static String generatePattern() {
        return String.format("slimjar.%s", UUID.randomUUID().toString());
    }
}
