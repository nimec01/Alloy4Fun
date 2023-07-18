package pt.haslab.specassistant.treeedit;

import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Expr;
import pt.haslab.alloyaddons.ExprNodeEquals;
import pt.haslab.alloyaddons.ExprNodeStringify;

public record EditData(Expr expr, Pos position) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EditData editData = (EditData) o;

        return ExprNodeEquals.equals(expr, editData.expr);
    }

    @Override
    public String toString() {
        return "{\"expr\"=" + ExprNodeStringify.stringify(expr) + ", \"position\"=\"" + position + "\"}";
    }
}
