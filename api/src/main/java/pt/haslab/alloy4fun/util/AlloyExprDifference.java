package pt.haslab.alloy4fun.util;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.*;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

public class AlloyExprDifference {

    public static List<Changes> getChanges(Expr before, Expr after) {
        int i, j;

        List<? extends Expr> lines_tokens = new FlattenTokenizer().visitThis(before).toList();
        List<? extends Expr> column_tokens = new FlattenTokenizer().visitThis(after).toList();

        //COMPUTE THE MATCHES AND MATCH PATHS ONTO THE TABLE
        int[][] calc_table = new int[lines_tokens.size()][column_tokens.size()];
        Stack<Pair<Integer, Integer>> matches = new Stack<>();

        for (i = 0; i < lines_tokens.size(); i++) {
            Expr curr_line = lines_tokens.get(i);
            int line_max = i == 0 ? 0 : calc_table[i - 1][0];
            for (j = 0; j < column_tokens.size(); j++) {
                int col_max = i == 0 ? 0 : calc_table[i - 1][j];
                calc_table[i][j] = Integer.max(line_max, col_max);
                if (shallowMatch(curr_line, column_tokens.get(j))) {
                    calc_table[i][j]++;
                    matches.add(new Pair<>(i, j));
                }
                line_max = calc_table[i][j];
            }
        }

        //COMPUTE THE LONGEST MATCH PATH

        Stack<Pair<Integer, Integer>> longest_match_path = new Stack<>();

        i = lines_tokens.size() - 1;
        j = column_tokens.size() - 1;
        while (i >= 0 || j >= 0) {
            try {
                int finalI = i, finalJ = j; //Lambdas require effective final references
                while (matches.peek().test((a, b) -> a > finalI || b > finalJ))
                    matches.pop();
                if (matches.peek().test((a, b) -> a == finalI && b == finalJ))
                    longest_match_path.push(matches.pop());
            } catch (EmptyStackException e) {
                break;
            }
            if (calc_table[i == 0 ? 0 : i - 1][j] > calc_table[i][j == 0 ? 0 : j - 1]) {
                i--;
            } else {
                j--;
            }
        }


        //COMPUTE THE DIFFERENCES FROM THE UNMATCHED TOKENS
        i = j = 0;
        List<Changes> result = new ArrayList<>();

        while (!longest_match_path.isEmpty()) {
            Pair<Integer, Integer> current_match = longest_match_path.pop();
            if (current_match.a > i + 1) {
                if (current_match.b > j + 1) {
                    result.add(new Changes(lines_tokens.subList(i + 1, current_match.a), column_tokens.subList(j + 1, current_match.b)));
                } else {
                    result.add(new Changes(lines_tokens.subList(i + 1, current_match.a), List.of()));
                }
            } else if (current_match.b > j + 1)
                result.add(new Changes(List.of(), column_tokens.subList(j + 1, current_match.b)));
            i = current_match.a;
            j = current_match.b;
        }

        if (i + 1 < lines_tokens.size() || j + 1 < column_tokens.size())
            result.add(new Changes(lines_tokens.subList(i + 1, lines_tokens.size()), column_tokens.subList(j + 1, column_tokens.size())));

        return result;
    }

    public static Pair<Integer, Integer> getEditCounts(List<Changes> edit) {
        Pair<Integer, Integer> result = new Pair<>(0, 0);
        edit.forEach(change -> {
            result.a += change.added.size();
            result.b += change.removed.size();
        });
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

    public static class Changes {
        List<? extends Expr> removed, added;

        public Changes(List<? extends Expr> removed, List<? extends Expr> added) {
            this.removed = removed;
            this.added = added;
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
