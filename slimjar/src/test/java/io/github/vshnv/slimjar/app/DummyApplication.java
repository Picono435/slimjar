package io.github.vshnv.slimjar.app;

public final class DummyApplication extends Application {
    public DummyApplication() {
        System.out.println(getClass().getClassLoader());
    }
}
