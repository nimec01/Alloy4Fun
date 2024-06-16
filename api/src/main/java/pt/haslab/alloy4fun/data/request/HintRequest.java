package pt.haslab.alloy4fun.data.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HintRequest {

    @JsonAlias({"challenge", "parentId", "modelId", "sessionId"})
    public String challenge;
    @JsonAlias({"predicate", "command_label", "cmd_n"})
    public String predicate;
    public String model;

    public boolean isComplete() {
        return challenge != null && predicate != null && model != null;
    }

}
