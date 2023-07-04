package pt.haslab.alloy4fun.resources;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import pt.haslab.alloy4fun.data.models.Counter;
import pt.haslab.alloy4fun.data.models.Session;
import pt.haslab.alloy4fun.data.transfer.ExerciseForm;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.services.HintService;
import pt.haslab.alloy4fun.services.SessionService;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

@Path("/hint")
public class AlloyHint {

    private static final Logger LOG = Logger.getLogger(AlloyHint.class);

    @Inject
    HintService hintService;

    @Inject
    SessionService sessionManager;

    @POST
    @Path("/group")
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeGraphAndExercises(List<ExerciseForm> forms, @QueryParam("graph_id") Long graph_id) {
        graph_id = graph_id == null ? Counter.nextGraphId() : Counter.ensureGraphId(graph_id);
        int n = hintService.generateExercisesWithGraphId(graph_id, new HashSet<>(forms)); // Set clears duplicates

        return Response.ok(n == 0 ? "No changes were made" : n < forms.size() ? "Some forms were ignored" : "Sucess").build();
    }

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHint(HintRequest request) {
        LOG.info("Hint requested for session " + request.sessionId);

        Session session = sessionManager.findById(request.sessionId);

        if (session == null)
            return Response.ok(InstanceMsg.error("Invalid Session")).build();

        try {
            Optional<InstanceMsg> response = session.hintRequest.get();

            if (response.isEmpty())
                LOG.debug("NO HINT AVAILABLE FOR " + request.sessionId);

            return Response.ok(response.orElseGet(() -> InstanceMsg.error("Unable to generate hint"))).build();
        } catch (CancellationException | InterruptedException e) {
            LOG.debug("HINT GEN Cancellation/Interruption");
            return Response.ok(InstanceMsg.error("Hint is unavailable")).build();
        } catch (ExecutionException e) {
            LOG.error(e);
            return Response.ok(InstanceMsg.error("Error when generating hint")).build();
        }
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
    }
}
