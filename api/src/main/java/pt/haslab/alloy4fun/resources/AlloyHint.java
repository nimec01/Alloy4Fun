package pt.haslab.alloy4fun.resources;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.mit.csail.sdg.parser.CompModule;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.data.models.HintGraph.HintGraph;
import pt.haslab.alloy4fun.data.models.Session;
import pt.haslab.alloy4fun.data.transfer.ExerciseForm;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.data.transfer.InstancesRequest;
import pt.haslab.alloy4fun.data.transfer.YearRange;
import pt.haslab.alloy4fun.services.HintService;
import pt.haslab.alloy4fun.services.SessionService;
import pt.haslab.alloy4fun.util.AlloyUtil;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
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
    public Response makeGraphAndExercises(List<ExerciseForm> forms, @QueryParam("graph_id") String graph_id_str, @DefaultValue("Unkown") @QueryParam("name") String graph_name) {
        ObjectId graph_id = (graph_id_str == null || graph_id_str.isEmpty() ? HintGraph.newGraph(graph_name) : HintGraph.findById(graph_id_str).orElseThrow()).id;
        int n = hintService.generateExercisesWithGraphId(graph_id, new HashSet<>(forms)); // Set clears duplicates

        return Response.ok(n == 0 ? "No changes were made" : n < forms.size() ? "Some forms were ignored" : "Sucess").build();
    }

    private static ObjectId getAGraphID(Map<String, ObjectId> graphspace, String prefix, String label) {
        if (!graphspace.containsKey(label))
            graphspace.put(label, HintGraph.newGraph(prefix + "-" + label).id);
        return graphspace.get(label);
    }

    @POST
    @Path("/group-secrets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeGraphAndExercisesFromCommands(List<String> model_ids, @DefaultValue("Unkown") @QueryParam("prefix") String prefix) {
        Map<String, ObjectId> graphspace = new HashMap<>();

        model_ids.forEach(id -> hintService.generateExercisesWithGraphIdFromSecrets(l -> getAGraphID(graphspace, prefix, l), id));

        return Response.ok("Sucess").build();
    }

    @GET
    @Path("/stress-test-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stressHints(@QueryParam("model_id") String model_id, @BeanParam YearRange yearRange) {
        hintService.testAllHintsOfModel(model_id, yearRange);
        return Response.ok().build();
    }

    @POST
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHint(InstancesRequest request) {
        CompModule world = AlloyUtil.parseModel(request.model);
        return Response.ok(hintService.getHint(request.parentId, world.getAllCommands().get(request.commandIndex).label, world).isPresent()).build();
    }

    @GET
    @Path("/scan-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response scanModel(@QueryParam("model_id") String model_id, @BeanParam YearRange yearRange) {
        hintService.parseModelHint(model_id, yearRange);
        return Response.ok().build();
    }

    @GET
    @Path("/scan-models")
    @Produces(MediaType.APPLICATION_JSON)
    public Response scanModels(List<String> model_ids, @BeanParam YearRange yearRange) {
        model_ids.forEach(id -> hintService.parseModelHint(id, yearRange));
        return Response.ok().build();
    }

    @GET
    @Path("/compute-policy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePolicy(@QueryParam("graph_id") String hex_string) {
        ObjectId graphId = new ObjectId(hex_string);
        hintService.computePolicyForGraph(graphId);
        hintService.debloatGraph(graphId);
        return Response.ok().build();
    }

    @GET
    @Path("/compute-policy-for-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePolicyOnModel(@QueryParam("model_id") String modelid) {
        hintService.getModelGraphs(modelid).forEach(id -> {
            hintService.computePolicyForGraph(id);
            hintService.debloatGraph(id);
        });
        return Response.ok().build();
    }

    @GET
    @Path("/full-test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computePolicyOnModel(List<String> model_ids, @DefaultValue("Unkown") @QueryParam("prefix") String prefix, @QueryParam("yearLower") Integer yearLower, @QueryParam("yearMiddle") Integer yearMiddle, @QueryParam("yearTop") Integer yearTop) {
        CompletableFuture.runAsync(() -> { // Allows the task to survive even if the http request is canceled
            long start = System.nanoTime();
            LOG.info("Starting test for " + prefix + " with model ids " + model_ids);
            hintService.deleteExerciseByModelIDs(model_ids, true);
            makeGraphAndExercisesFromCommands(model_ids, prefix).close();
            LOG.debug("Scanning models");
            scanModels(model_ids, new YearRange(yearLower, yearMiddle)).close();
            LOG.debug("Computing policies");
            hintService.getModelGraphs(model_ids.get(0)).forEach(id -> {
                hintService.computePolicyForGraph(id);
                hintService.debloatGraph(id);
            });
            LOG.info("Stressing graph for hints");
            model_ids.forEach(id -> stressHints(id, new YearRange(yearMiddle, yearTop)).close());
            LOG.info("Completed test after " + 1e-9 * (System.nanoTime() - start) + " seconds");
        }).whenComplete((nil, error) -> {
            if (error != null)
                error.printStackTrace();
        });

        return Response.ok("Test started").build();
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
    @Path("/debug-drop-everything")
    @Produces(MediaType.APPLICATION_JSON)
    public Response debug() {
        hintService.dropEverything();
        return Response.ok().build();
    }

    @GET
    @Path("/debug-rm-hint-stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response debug1() {
        HintGraph.removeAllHintStats();
        return Response.ok().build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HintRequest {
        public String sessionId;
    }
}
