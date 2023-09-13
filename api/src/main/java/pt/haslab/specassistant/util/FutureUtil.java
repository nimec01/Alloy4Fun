package pt.haslab.specassistant.util;

import org.jboss.logging.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface FutureUtil {

    static <R> R inline(CompletableFuture<R> future) throws Throwable {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    static <R> R inlineRuntime(CompletableFuture<R> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            try {
                throw new RuntimeException(e.getCause());
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static <R> void mergeFutures(Collection<CompletableFuture<R>> futures, Consumer<R> process) {
        for (CompletableFuture<R> future : futures) {
            process.accept(inlineRuntime(future));
        }
    }

    static <K, V> Map<K, V> mergeFutureEntries(Collection<CompletableFuture<Map.Entry<K, V>>> futures) {
        return mergeFutureEntries(futures, (a, b) -> b);
    }

    static <K, V> Map<K, V> mergeFutureEntries(Collection<CompletableFuture<Map.Entry<K, V>>> futures, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Map<K, V> result = new HashMap<>();
        mergeFutures(futures, entry -> result.merge(entry.getKey(), entry.getValue(), remappingFunction));
        return result;
    }


    static <V> CompletableFuture<Void> allFutures(Stream<CompletableFuture<V>> futureStream) {
        return CompletableFuture.allOf(futureStream.toArray(CompletableFuture[]::new));
    }

    static <V> CompletableFuture<Void> allFutures(Collection<CompletableFuture<V>> futureStream) {
        return CompletableFuture.allOf(futureStream.toArray(CompletableFuture[]::new));
    }

    static <V> CompletableFuture<Void> forEachAsync(Stream<V> stream, Consumer<V> consumer) {
        return CompletableFuture.allOf(stream.map(t -> CompletableFuture.runAsync(() -> consumer.accept(t))).toArray(CompletableFuture[]::new));
    }

    static <V, R> CompletableFuture<Void> runEachAsync(Stream<V> stream, Function<V, CompletableFuture<R>> function) {
        return CompletableFuture.allOf(stream.map(function).toArray(CompletableFuture[]::new));
    }

    static <V> BiConsumer<V, Throwable> log(Logger logger) {
        return (nil, error) -> {
            if (error != null)
                logger.error(error);
        };
    }

    static <V> BiConsumer<V, Throwable> logInfo(Logger logger, String msg) {
        return (nil, error) -> {
            if (error != null)
                logger.error(error);
            else
                logger.info(msg);
        };
    }

    static <V> BiConsumer<V, Throwable> logDebug(Logger logger, String msg) {
        return (nil, error) -> {
            if (error != null)
                logger.error(error);
            else
                logger.info(msg);
        };
    }

    static BiConsumer<? super Void,? super Throwable> logTrace(Logger logger, String msg) {
        return (nil, error) -> {
            if (error != null)
                logger.error(error);
            else
                logger.trace(msg);
        };
    }
}
