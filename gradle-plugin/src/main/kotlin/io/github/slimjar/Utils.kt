package io.github.slimjar // ktlint-disable filename

import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.* // ktlint-disable no-wildcard-imports

internal val TaskContainer.targetedJarTask: TaskProvider<Jar> get() {
    return when {
        findByName("reobfJar") != null -> named<Jar>("reobfJar")
        findByName("shadowJar") != null -> named<Jar>("shadowJar")
        findByName("jar") != null -> named<Jar>("jar")
        else -> error("No slimJar target found")
    }
}

internal val TaskContainerScope.targetedJarTask: TaskProvider<Jar> get() = this.container.targetedJarTask

internal val Project.slimExtension: SlimJarExtension get() = extensions.getByType()
