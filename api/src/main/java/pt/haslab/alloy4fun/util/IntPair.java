package pt.haslab.alloy4fun.util;

import java.util.Objects;
import java.util.function.BiPredicate;

public class IntPair {
    public Integer fst;
    public Integer snd;


    public IntPair() {
    }

    public IntPair(Integer fst, Integer snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public Integer getFst() {
        return fst;
    }

    public void setFst(Integer fst) {
        this.fst = fst;
    }

    public Integer getSnd() {
        return snd;
    }

    public void setSnd(Integer snd) {
        this.snd = snd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntPair pair = (IntPair) o;

        if (!Objects.equals(fst, pair.fst)) return false;
        return Objects.equals(snd, pair.snd);
    }

    @Override
    public int hashCode() {
        int result = fst != null ? fst.hashCode() : 0;
        result = 31 * result + (snd != null ? snd.hashCode() : 0);
        return result;
    }

    public boolean test(BiPredicate<Integer, Integer> pred) {
        return pred.test(fst, snd);
    }

    @Override
    public String toString() {
        return "(" + fst + ',' + snd + ')';
    }
}
