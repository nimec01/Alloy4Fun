package pt.haslab.alloy4fun.util;

import java.util.concurrent.Callable;

public interface Catch {

    static <V> V atRuntime(Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    static <V> V asNull(Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            return null;
        }
    }
}
