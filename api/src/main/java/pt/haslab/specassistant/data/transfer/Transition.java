package pt.haslab.specassistant.data.transfer;

import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintNode;

public class Transition {
    public HintEdge action;
    public HintNode from;
    public HintNode to;

    public Transition(HintEdge action, HintNode from, HintNode to) {
        this.action = action;
        this.from = from;
        this.to = to;
    }

}
