package pt.haslab.alloy4fun.util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Static {
    static <K, V> List<Map<K, V>> getArrangements(Map<K, V> unchanged, List<Map.Entry<K, List<V>>> changed) {
        int[] counts = new int[changed.size()];
        int[] factorization = new int[changed.size()];
        factorization[0] = 1;
        counts[0] = changed.get(0).getValue().size();
        for (int i = 1; i < changed.size(); i++) {
            counts[i] = changed.get(i).getValue().size();
            factorization[i] = factorization[i - 1] * counts[i - 1];
        }
        int arrangement_count = Arrays.stream(counts).reduce(1, (x, y) -> x * y);

        List<Map<K, V>> result = new ArrayList<>();

        for (int i = 0; i < arrangement_count; i++) {
            Map<K, V> current = new HashMap<>(unchanged);
            for (int j = 0; j < changed.size(); j++) {
                Map.Entry<K, List<V>> entry = changed.get(j);
                current.put(entry.getKey(), entry.getValue().get((i / factorization[j]) % counts[j]));
            }
            result.add(current);
        }
        return result;
    }

    static <K, V, R> Map<K, R> mapValues(Map<K, V> target, Function<V, R> mapping) {
        return target.entrySet().stream().map(x -> mapValue(x, mapping)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static <K, V, R> Map<R, V> mapKeys(Map<K, V> target, Function<K, R> mapping) {
        return target.entrySet().stream().map(x -> mapKey(x, mapping)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static <K, V, R> Map.Entry<K, R> mapValue(Map.Entry<K, V> target, Function<V, R> mapping) {
        return Map.entry(target.getKey(), mapping.apply(target.getValue()));
    }

    static <K, V, R> Map.Entry<R, V> mapKey(Map.Entry<K, V> target, Function<K, R> mapping) {
        return Map.entry(mapping.apply(target.getKey()), target.getValue());
    }
}
