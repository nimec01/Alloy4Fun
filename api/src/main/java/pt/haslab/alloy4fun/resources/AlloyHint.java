package pt.haslab.alloy4fun.resources;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import pt.haslab.alloy4fun.data.models.Session;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.services.HintService;
import pt.haslab.alloy4fun.services.SessionService;

import java.io.IOException;

@Path("/hint")
public class AlloyHint {

    private static final Logger LOG = Logger.getLogger(AlloyHint.class);

    @Inject
    HintService hintService;

    @Inject
    SessionService sessionManager;

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHint(HintRequest request) {
        LOG.info("Hint requested for model " + request.originId);

        Session session = sessionManager.findById(request.sessionId);

        if (session == null)
            return Response.ok(InstanceMsg.error("Invalid Session")).build();

        return Response.ok(hintService.getHint(request.originId, session.cmd.label, session.skolem)).build();

    }

    @GET
    @Path("/debug")
    @Produces(MediaType.APPLICATION_JSON)
    public Response debug(@RestQuery String id) throws IOException {
        hintService.dropEverything();
        hintService.fullSetupFor(id);
        return Response.ok().build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HintRequest {
        public String sessionId;
        public String originId;
    }
}
