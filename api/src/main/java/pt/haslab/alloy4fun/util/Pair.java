package pt.haslab.alloy4fun.util;

import java.util.Objects;
import java.util.function.BiPredicate;

public class Pair<A, B> {

    public A a;
    public B b;


    public Pair() {
    }

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }

    public B getB() {
        return b;
    }

    public void setB(B b) {
        this.b = b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (!Objects.equals(a, pair.a)) return false;
        return Objects.equals(b, pair.b);
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        return result;
    }

    public boolean test(BiPredicate<A, B> pred) {
        return pred.test(a, b);
    }

    @Override
    public String toString() {
        return "(" + a + ',' + b + ')';
    }
}
