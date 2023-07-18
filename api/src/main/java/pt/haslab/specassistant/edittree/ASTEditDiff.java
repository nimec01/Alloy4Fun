package pt.haslab.specassistant.edittree;

import at.unisalzburg.dbresearch.apted.distance.APTED;
import edu.mit.csail.sdg.ast.Expr;

public class ASTEditDiff extends APTED<EditData, EditDataCostModel> {
    public ASTEditDiff() {
        super(new EditDataCostModel());
    }


    public ASTEditDiff initFrom(Expr from, Expr to) {
        this.init(ExprToEditData.parseOrDefault(from), ExprToEditData.parseOrDefault(to));
        return this;
    }
}
