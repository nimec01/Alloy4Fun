package pt.haslab.specassistant.edittree;

import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Expr;

public record EditData(Expr expr, Pos position) {
}
