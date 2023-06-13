package pt.haslab.alloy4fun.util;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.*;

import java.util.*;
import java.util.stream.Stream;

public class AlloyExprDifference {

    List<? extends Expr> lines_tokens, column_tokens;

    List<IntPair> match_path;

    public static AlloyExprDifference create(Expr exprA, Expr exprB) {
        AlloyExprDifference result = new AlloyExprDifference();
        result.lines_tokens = new FlattenTokenizer().visitThis(exprA).toList();
        result.column_tokens = new FlattenTokenizer().visitThis(exprB).toList();
        return result;
    }

    public static boolean shallowMatch(Expr a, Expr b) {
        if (!a.getClass().equals(b.getClass())) return false;

        if (a instanceof ExprBinary)
            return ((ExprBinary) a).op.equals(((ExprBinary) b).op);
        if (a instanceof ExprQt)
            return ((ExprQt) a).op.equals(((ExprQt) b).op);
        if (a instanceof ExprUnary)
            return ((ExprUnary) a).op.equals(((ExprUnary) b).op);
        if (a instanceof ExprHasName)
            return ((ExprHasName) a).label.equals(((ExprHasName) b).label);

        return a.isSame(b);
    }

    private static Pos findDelimiterPos(int from, int to, List<? extends Expr> target) {
        int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE, x2 = 0, y2 = 0;
        for (int i = from; i < to; i++) {
            Pos pos = target.get(i).pos();
            if (pos != null) {
                if (x > pos.x)
                    x = pos.x;
                if (y > pos.y)
                    y = pos.y;
                if (x2 < pos.x2)
                    x2 = pos.x2;
                if (y2 < pos.y2)
                    y2 = pos.y2;
            }
        }
        for (; from > 0 && (x == Integer.MAX_VALUE && y == Integer.MAX_VALUE); from--) {
            Pos pos = target.get(from).pos();
            if (pos != null) {
                x = pos.x2;
                y = pos.y2;
                break;
            }
        }
        for (; to < target.size() && (x2 == 0 && y2 == 0); to++) {
            Pos pos = target.get(to).pos();
            if (pos != null) {
                x2 = pos.x;
                y2 = pos.y;
                break;
            }
        }
        return new Pos(UUID.randomUUID().toString(), x, y, x2, y2);
    }

    public static AlloyExprDifference createHalfA(Expr expr) {
        AlloyExprDifference result = new AlloyExprDifference();
        result.lines_tokens = new FlattenTokenizer().visitThis(expr).toList();
        result.column_tokens = new ArrayList<>();
        return result;
    }

    public AlloyExprDifference compute() {
        int i, j;
        //COMPUTE THE MATCHES (AND THEREFORE THE PATHS) ONTO THE TABLE
        int[][] calc_table = new int[lines_tokens.size()][column_tokens.size()];
        Stack<IntPair> all_matches = new Stack<>();

        for (i = 0; i < lines_tokens.size(); i++) {
            Expr curr_line = lines_tokens.get(i);
            int line_max = i == 0 ? 0 : calc_table[i - 1][0];
            for (j = 0; j < column_tokens.size(); j++) {
                int col_max = i == 0 ? 0 : calc_table[i - 1][j];
                calc_table[i][j] = Integer.max(line_max, col_max);
                if (shallowMatch(curr_line, column_tokens.get(j))) {
                    calc_table[i][j]++;
                    all_matches.add(new IntPair(i, j));
                }
                line_max = calc_table[i][j];
            }
        }

        //COMPUTE THE LONGEST MATCH PATH
        match_path = new ArrayList<>();

        i = lines_tokens.size() - 1;
        j = column_tokens.size() - 1;
        while (i >= 0 || j >= 0) {
            try {
                int finalI = i, finalJ = j; //Lambdas require effective final references
                while (all_matches.peek().test((a, b) -> a > finalI || b > finalJ))
                    all_matches.pop();
                if (all_matches.peek().test((a, b) -> a == finalI && b == finalJ))
                    match_path.add(all_matches.pop());
            } catch (EmptyStackException e) {
                break;
            }
            if (calc_table[i == 0 ? 0 : i - 1][j] > calc_table[i][j == 0 ? 0 : j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        Collections.reverse(match_path);
        return this;
    }

    public List<TokenDifference> extractTokensFromIndexDifferences(List<IndexRangeDifference> indexRangeDifferences) {
        return indexRangeDifferences.stream().map(x -> new TokenDifference(lines_tokens.subList(x.fromA(), x.toA()), column_tokens.subList(x.fromB(), x.toB()))).toList();
    }

    public List<TokenDifference> getTokenDifferences(List<IndexRangeDifference> indexRangeDifferences) {
        return extractTokensFromIndexDifferences(getIndexDifferences());
    }

    public List<IndexRangeDifference> getIndexDifferences() {
        // I'm using an array because I can only pass effective final fields to Lambdas.
        // Using "AtomicInteger()"s would also work, but I don't need the mutual exclusivity and its overhead
        int[] nextIJ = new int[]{0, 0};
        List<IndexRangeDifference> result = new ArrayList<>();

        match_path.forEach(match -> {
            if (match.fst > nextIJ[0] || match.snd > nextIJ[1]) {
                result.add(new IndexRangeDifference(nextIJ[0], match.fst, nextIJ[1], match.snd));
            }
            nextIJ[0] = match.fst + 1;
            nextIJ[1] = match.snd + 1;
        });

        if (nextIJ[0] < lines_tokens.size() || nextIJ[1] < column_tokens.size()) {
            result.add(new IndexRangeDifference(nextIJ[0], lines_tokens.size(), nextIJ[1], column_tokens.size()));
        }

        return result;
    }

    public Pos findDelimiterPosA(IndexRangeDifference diff) {
        return findDelimiterPos(diff.fromA(), diff.toA(), lines_tokens);
    }

    public Pos findDelimiterPosB(IndexRangeDifference diff) {
        return findDelimiterPos(diff.fromB(), diff.toB(), column_tokens);
    }

    public static Integer getEditDifference(List<IndexRangeDifference> differences) {
        return differences.stream().map(x -> Math.abs(x.fromA() - x.toA()) + Math.abs(x.fromB() - x.toB())).reduce(0, Integer::sum);
    }

    public String getHintMessage(IndexRangeDifference indexRangeDiff) { // TODO
        if (indexRangeDiff.fromA() < indexRangeDiff.toA() && indexRangeDiff.fromB() < indexRangeDiff.toB())
            return "Change something in the highlighted range";
        else if (indexRangeDiff.fromA() < indexRangeDiff.toA()) {
            return "Add something in the highlighted range";
        } else {
            return "Remove the highlighted range";
        }
    }

    public static class IndexRangeDifference {
        public IntPair AChanges, BChanges;

        public IndexRangeDifference(int inclusiveFromA, int exclusiveToA, int inclusiveFromB, int exclusiveToB) {
            this.AChanges = new IntPair(inclusiveFromA, exclusiveToA);
            this.BChanges = new IntPair(inclusiveFromB, exclusiveToB);
        }

        public int fromA() {
            return AChanges.fst;
        }

        public int toA() {
            return AChanges.snd;
        }

        public int fromB() {
            return BChanges.fst;
        }

        public int toB() {
            return BChanges.snd;
        }
    }

    public static class TokenDifference {
        public List<? extends Expr> changesToA, changesToB;

        public TokenDifference(List<? extends Expr> ATokens, List<? extends Expr> BTokens) {
            this.changesToA = ATokens;
            this.changesToB = BTokens;
        }

        public Pos getPosA() {
            if (changesToA.isEmpty())
                return null;

            //posArr = [x,y,x2,y2]
            //they must be pointers because of lambdas
            int[] posArr = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};

            changesToA.stream()
                    .map(Expr::pos)
                    .filter(Objects::nonNull)
                    .forEach(pos -> {
                        if (posArr[0] > pos.x)
                            posArr[0] = pos.x;
                        if (posArr[1] > pos.y)
                            posArr[1] = pos.y;
                        if (posArr[2] < pos.x2)
                            posArr[2] = pos.x2;
                        if (posArr[3] < pos.y2)
                            posArr[3] = pos.y2;
                    });
            if (posArr[2] > Integer.MIN_VALUE || posArr[3] > Integer.MIN_VALUE)
                return new Pos(UUID.randomUUID().toString(), posArr[0], posArr[1], posArr[2], posArr[3]);
            return new Pos(UUID.randomUUID().toString(), posArr[0], posArr[1]);
        }
    }


    public static class FlattenTokenizer extends VisitReturn<Stream<? extends Expr>> {
        @Override
        public Stream<? extends Expr> visit(ExprBinary exprBinary) throws Err {
            return Stream.of(Stream.of(exprBinary), visitThis(exprBinary.left), visitThis(exprBinary.right)).flatMap(i -> i);
        }

        @Override
        public Stream<? extends Expr> visit(ExprList exprList) throws Err {
            return Stream.concat(Stream.of(exprList), exprList.args.stream().flatMap(this::visitThis));
        }

        @Override
        public Stream<? extends Expr> visit(ExprCall exprCall) throws Err {
            return Stream.of(exprCall);
        }

        @Override
        public Stream<? extends Expr> visit(ExprConstant exprConstant) throws Err {
            return Stream.of(exprConstant);
        }

        @Override
        public Stream<? extends Expr> visit(ExprITE exprITE) throws Err {
            return Stream.of(Stream.of(exprITE), visitThis(exprITE.cond), visitThis(exprITE.left), visitThis(exprITE.right)).flatMap(i -> i);
        }

        @Override
        public Stream<? extends Expr> visit(ExprLet exprLet) throws Err {
            return Stream.of(Stream.of(exprLet), visitThis(exprLet.var), visitThis(exprLet.expr), visitThis(exprLet.sub)).flatMap(i -> i);
        }

        @Override
        public Stream<? extends Expr> visit(ExprQt exprQt) throws Err {
            return Stream.of(Stream.of(exprQt), exprQt.decls.stream().map(x -> x.expr).flatMap(this::visitThis), visitThis(exprQt.sub)).flatMap(i -> i);
        }

        @Override
        public Stream<? extends Expr> visit(ExprUnary exprUnary) throws Err {
            if (exprUnary.op.equals(ExprUnary.Op.NOOP))
                return visitThis(exprUnary.sub);
            return Stream.of(Stream.of(exprUnary), visitThis(exprUnary.sub)).flatMap(i -> i);
        }

        @Override
        public Stream<? extends Expr> visit(ExprVar exprVar) throws Err {
            return Stream.of(exprVar);
        }

        @Override
        public Stream<? extends Expr> visit(Sig sig) throws Err {
            return Stream.of(sig);
        }

        @Override
        public Stream<? extends Expr> visit(Sig.Field field) throws Err {
            return Stream.of(field);
        }
    }

}
