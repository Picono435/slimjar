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

package io.github.slimjar.resolver;


import io.github.slimjar.logging.LogDispatcher;
import io.github.slimjar.logging.ProcessLogger;
import io.github.slimjar.resolver.data.Repository;
import io.github.slimjar.resolver.data.Dependency;
import io.github.slimjar.resolver.enquirer.RepositoryEnquirer;
import io.github.slimjar.resolver.enquirer.RepositoryEnquirerFactory;
import io.github.slimjar.resolver.pinger.URLPinger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class CachingDependencyResolver implements DependencyResolver {
    private static final String FAILED_RESOLUTION_MESSAGE = "[FAILED TO RESOLVE]";
    private static final ProcessLogger LOGGER = LogDispatcher.getMediatingLogger();
    private final URLPinger urlPinger;
    private final Collection<RepositoryEnquirer> repositories;
    private final Map<Dependency, ResolutionResult> cachedResults = new ConcurrentHashMap<>();
    private final Map<String, ResolutionResult> preResolvedResults;

    public CachingDependencyResolver(final URLPinger urlPinger, final Collection<Repository> repositories, final RepositoryEnquirerFactory enquirerFactory, final Map<String, ResolutionResult> preResolvedResults) {
        this.urlPinger = urlPinger;
        this.preResolvedResults = new ConcurrentHashMap<>(preResolvedResults);
        this.repositories = repositories.stream()
                .map(enquirerFactory::create)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ResolutionResult> resolve(final Dependency dependency) {
        return Optional.ofNullable(cachedResults.computeIfAbsent(dependency, dep -> attemptResolve(dep, Collections.emptyList())));
    }

    public Optional<ResolutionResult> resolve(
        final Dependency dependency,
        final List<RepositoryEnquirer> enforcedRepositories
    ) { return Optional.ofNullable(cachedResults.computeIfAbsent(dependency, dep -> attemptResolve(dep, enforcedRepositories))); }

    private ResolutionResult attemptResolve(
        final Dependency dependency,
        final List<RepositoryEnquirer> enforcedRepositories
    ) {
        final var preResolvedResult = preResolvedResults.get(dependency.toString()) != null ? preResolvedResults.get(dependency.toString()) : cachedResults.get(dependency);

        if (preResolvedResult != null) {
            if (preResolvedResult.isChecked()) return preResolvedResult;
            if (preResolvedResult.isAggregator()) return preResolvedResult;

            final var preResolvedUrl = preResolvedResult.getRepository().url().toString();
            final var isDependencyValid = (enforcedRepositories.isEmpty() || enforcedRepositories.stream().anyMatch(repo -> repo.toString().equals(preResolvedUrl))) && urlPinger.ping(preResolvedResult.getDependencyURL());
            final var isChecksumValid = preResolvedResult.getChecksumURL() == null || urlPinger.ping(preResolvedResult.getChecksumURL());

            if (isDependencyValid && isChecksumValid) {
                preResolvedResult.setChecked();
                return preResolvedResult;
            }
        }

        final var usedRepositories = enforcedRepositories.isEmpty() ? repositories : enforcedRepositories;
        final var result = usedRepositories.stream().parallel()
                .map(repositoryEnquirer -> repositoryEnquirer.enquire(dependency))
                .filter(Objects::nonNull)
                .findFirst();
        final var resolvedResult = result.map(ResolutionResult::getDependencyURL)
                .map(Objects::toString)
                .orElse(FAILED_RESOLUTION_MESSAGE);

        LOGGER.log("Resolved %s @ %s", dependency.artifactId(), resolvedResult);
        return result.orElse(null);
    }
}
