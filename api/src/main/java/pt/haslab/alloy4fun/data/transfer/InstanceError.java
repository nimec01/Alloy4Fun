package pt.haslab.alloy4fun.data.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public class InstanceError {

    @JsonInclude(NON_DEFAULT)
    public Boolean warning_error = false;
    @JsonInclude(NON_DEFAULT)
    public Boolean alloy_error = false;
    @JsonInclude(NON_NULL)
    public String msg;
    @JsonInclude(NON_NULL)
    public Integer line;
    @JsonInclude(NON_NULL)
    public Integer column;
    @JsonInclude(NON_NULL)
    public Integer line2;
    @JsonInclude(NON_NULL)
    public Integer column2;

    public InstanceError() {
    }

    public static InstanceError error(String msg) {
        InstanceError response = new InstanceError();
        response.alloy_error = true;
        response.msg = msg;
        return response;
    }

    public static InstanceError from(Err err) {
        InstanceError response = new InstanceError();

        if (err instanceof ErrorWarning) {
            response.warning_error = true;
        } else {
            response.alloy_error = true;
        }
        response.msg = err.getMessage();
        response.line = err.pos.y;
        response.column = err.pos.x;
        response.line2 = err.pos.y2;
        response.column2 = err.pos.x2;

        return response;
    }

    public static InstanceError from(Err err, String msg) {
        InstanceError response = from(err);
        response.msg = msg;

        return response;
    }
}
