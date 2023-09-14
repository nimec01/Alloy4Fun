package pt.haslab.alloy4fun.resources;


import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.data.models.Session;
import pt.haslab.alloy4fun.data.request.ExerciseForm;
import pt.haslab.alloy4fun.data.request.HintRequest;
import pt.haslab.alloy4fun.data.request.YearRange;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.repositories.SessionRepository;
import pt.haslab.alloyaddons.ParseUtil;
import pt.haslab.specassistant.data.models.HintGraph;
import pt.haslab.specassistant.data.transfer.HintMsg;
import pt.haslab.specassistant.services.GraphInjestor;
import pt.haslab.specassistant.services.GraphManager;
import pt.haslab.specassistant.services.HintGenerator;
import pt.haslab.specassistant.services.PolicyManager;
import pt.haslab.specassistant.services.policy.ProbabilityEvaluation;
import pt.haslab.specassistant.services.policy.RewardEvaluation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Path("/hint")
public class AlloyHint {

    private static final Logger LOG = Logger.getLogger(AlloyHint.class);

    @Inject
    GraphManager graphManager;

    @Inject
    HintGenerator hintGenerator;
    @Inject
    GraphInjestor graphInjestor;
    @Inject
    PolicyManager policyManager;

    @Inject
    SessionRepository sessionManager;

    @POST
    @Path("/group")
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeGraphAndExercises(List<ExerciseForm> forms, @QueryParam("graph_id") String hexstring, @DefaultValue("Unkown") @QueryParam("name") String graph_name) {
        ObjectId graph_id;
        if (hexstring == null || hexstring.isEmpty())
            graph_id = HintGraph.newGraph(graph_name).id;
        else
            graph_id = new ObjectId(hexstring);

        forms.forEach(f -> graphManager.generateExercise(graph_id, f.modelId, f.secretCommandCount, f.cmd_n, f.targetFunctions));
        return Response.ok("Sucess").build();
    }

    @GET
    @Path("/scan-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response scanModel(@QueryParam("model_id") String model_id, @BeanParam YearRange yearRange) {
        graphInjestor.parseModelTree(model_id, yearRange::testDate);
        return Response.ok().build();
    }

    @GET
    @Path("/scan-models")
    @Produces(MediaType.APPLICATION_JSON)
    public Response scanModels(List<String> model_ids, @BeanParam YearRange yearRange) {
        model_ids.forEach(id -> graphInjestor.parseModelTree(id, yearRange::testDate));
        return Response.ok().build();
    }

    @GET
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHint(HintRequest request) {
        LOG.info("Hint requested for session " + request.challenge);

        Session session = sessionManager.findById(request.challenge);

        if (session == null)
            return Response.ok(InstanceMsg.error("Invalid Session")).build();

        try {
            Optional<HintMsg> response = session.hintRequest.get();

            if (response.isEmpty())
                LOG.debug("NO HINT AVAILABLE FOR " + request.challenge);

            return Response.ok(response.map(InstanceMsg::from).orElseGet(() -> InstanceMsg.error("Unable to generate hint"))).build();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e);
            return Response.ok(InstanceMsg.error(e instanceof ExecutionException ? "Error when generating hint" : "Hint is unavailable")).build();
        }
    }

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHint(HintRequest request) {
        Optional<HintMsg> result = hintGenerator.getHint(request.challenge, request.predicate, ParseUtil.parseModel(request.model));
        return result.map(r -> Response.ok(InstanceMsg.from(r))).orElseGet(() -> Response.status(Response.Status.NO_CONTENT)).build();
    }

    @POST
    @Path("/compute-popular-edge-policy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePopularEdgePolicy(@QueryParam("model_id") String modelid) {
        graphManager.getModelGraphs(modelid).forEach(id -> {
            policyManager.computePolicyForGraph(id, 1.0, RewardEvaluation.NONE, ProbabilityEvaluation.EDGE);
        });
        return Response.ok("Popular policy computed.").build();
    }

    @POST
    @Path("/compute-policy-for-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computeTedEdge(@QueryParam("model_id") String modelid) {
        graphManager.getModelGraphs(modelid).forEach(id -> {
            policyManager.computePolicyForGraph(id, 0.99, RewardEvaluation.TED, ProbabilityEvaluation.EDGE);
        });
        return Response.ok("Popular policy computed.").build();
    }


}
