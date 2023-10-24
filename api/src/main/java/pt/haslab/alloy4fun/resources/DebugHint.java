package pt.haslab.alloy4fun.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.data.request.YearRange;
import pt.haslab.specassistant.services.GraphManager;
import pt.haslab.specassistant.services.PolicyManager;
import pt.haslab.specassistant.services.TestService;
import pt.haslab.specassistant.util.FutureUtil;
import pt.haslab.specassistant.util.Text;

import java.util.List;
import java.util.Map;


@Path("/debug-hint")
public class DebugHint {

    @Inject
    Logger log;
    @Inject
    GraphManager graphManager;
    @Inject
    TestService testService;

    @Inject
    PolicyManager policyManager;

    @GET
    public Response debug() {



        return Response.ok().build();
    }

    /**
     * This method is used to set up the graphs for the first time.
     * It will generate the graphs from the exercises for the given models.
     * It will also compute the policies for the graphs and debloat them.
     */
    @GET
    @Path("/setup-graphs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response genGraphs(List<String> model_ids, @DefaultValue("Unkown") @QueryParam("prefix") String prefix, @BeanParam YearRange yearRange) {
        testService.autoSetupJob(model_ids, prefix, x -> yearRange.testDate(Text.parseDate(x.getTime())));
        return Response.ok("Setup in progress.").build();
    }


    @GET
    @Path("/setup-multiple-graphs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response genGraphs(Map<String, List<String>> model_ids, @BeanParam YearRange yearRange) {
        FutureUtil.forEachAsync(model_ids.entrySet(), e -> testService.autoSetupJob(e.getValue(), e.getKey(), x -> yearRange.testDate(Text.parseDate(x.getTime()))));
        return Response.ok("Setup in progress.").build();
    }

    @GET
    @Path("/debug-drop-everything")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dropEverything() {
        graphManager.dropEverything();
        return Response.ok().build();
    }

    @GET
    @Path("/do-tar-for-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tarModel(@QueryParam("model_id") String model_id, @BeanParam YearRange yearRange) {
        testService.testChallengeWithTAR(model_id, x -> yearRange.testDate(Text.parseDate(x.getTime())));
        return Response.ok("Test Started").build();
    }

    @GET
    @Path("/fix-test-graph-ids")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fixTest() {
        testService.fixTestGraphIds();
        return Response.ok().build();
    }


    @GET
    @Path("/setup-exercises")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setUpExercises(Map<String, List<String>> model_ids) {
        model_ids.forEach(testService::makeGraphAndChallengesFromCommands);
        return Response.ok().build();
    }

    @GET
    @Path("/do-tar-for-all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tarALL() {
        testService.testAllChallengesWithTAR(x -> true);
        return Response.ok("Test Started").build();
    }

    @GET
    @Path("/do-tar-for-all-date")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tarALLDATE(@BeanParam YearRange yearRange) {
        testService.testAllChallengesWithTAR(x -> yearRange.testDate(Text.parseDate(x.getTime())));
        return Response.ok("Test Started").build();
    }

    @POST
    @Path("/compute-policy-for-all-models")
    @Produces(MediaType.APPLICATION_JSON)
    public Response computeTedEdge(@QueryParam("reward") @DefaultValue("TED") String reward, @QueryParam("probability") @DefaultValue("ARRIVALS") String probability) {

        //testService.computePoliciesForAll(PolicyRule.costPrefMix(Var.Name.ARRIVALS, Var.Name.TED));

        return Response.ok("Policy computation started.").build();
    }


    @GET
    @Path("/spec-test-default")
    @Produces({MediaType.APPLICATION_JSON})
    public Response specSplitDefault(Map<String, List<String>> model_ids) {
        testService.specTestDefaultPolicies(model_ids);
        return Response.ok("Started.").build();
    }

    @GET
    @Path("/retrain")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrain(Map<String, List<String>> model_ids) {
        testService.retrain();
        return Response.ok("Started.").build();
    }


    @GET
    @Path("/retest")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retest(Map<String, List<String>> model_ids) {
        testService.retest();
        return Response.ok("Started.").build();
    }

}
