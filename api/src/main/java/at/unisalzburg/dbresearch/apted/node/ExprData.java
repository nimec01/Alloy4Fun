package at.unisalzburg.dbresearch.apted.node;

import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Expr;

public record ExprData(Expr expr, Pos position) {
}
