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

import io.github.slimjar.resolver.data.Repository;
import io.github.slimjar.util.Connections;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ResolutionResult {
    private final Repository repository;
    private final URL dependencyURL;
    private final String checksum;
    private final boolean isAggregator;
    private transient boolean checked;

    public ResolutionResult(
            final Repository repository,
            final URL dependencyURL,
            final URL checksumURL,
            final boolean isAggregator,
            final boolean checked
    ) {
        this(repository, dependencyURL, getChecksumFromURL(checksumURL), isAggregator, checked);
    }

    public ResolutionResult(
            final Repository repository,
            final URL dependencyURL,
            final String checksum,
            final boolean isAggregator,
            final boolean checked
    ) {
        this.repository = repository;
        this.dependencyURL = dependencyURL;
        this.checksum = checksum;
        this.isAggregator = isAggregator;
        this.checked = checked;

        if (!isAggregator) {
            Objects.requireNonNull(dependencyURL, "Resolved URL must not be null for non-aggregator dependencies");
        }
    }

    public Repository getRepository() {
        return repository;
    }

    public URL getDependencyURL() {
        return dependencyURL;
    }

    public String getChecksum() {
        return checksum;
    }

    public boolean isAggregator() {
        return isAggregator;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked() {
        this.checked = true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolutionResult that = (ResolutionResult) o;
        // String comparison to avoid all blocking calls
        return dependencyURL.toString().equals(that.toString()) &&
                Objects.equals(checksum, that.checksum) &&
                isAggregator == that.isAggregator &&
                checked == that.checked;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencyURL.toString(), checksum, isAggregator, checked);
    }

    private static String getChecksumFromURL(URL checksumURL) {
        if(checksumURL == null) {
            return null;
        }
        try {
            final URLConnection connection = Connections.createDownloadConnection(checksumURL);
            final InputStream inputStream = connection.getInputStream();
            int bufferSize = 1024;
            char[] buffer = new char[bufferSize];
            StringBuilder out = new StringBuilder();
            Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
                out.append(buffer, 0, numRead);
            }
            return out.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
