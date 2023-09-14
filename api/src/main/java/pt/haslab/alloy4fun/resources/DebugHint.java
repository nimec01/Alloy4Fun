package pt.haslab.alloy4fun.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.haslab.alloy4fun.data.request.YearRange;
import pt.haslab.specassistant.services.GraphManager;
import pt.haslab.specassistant.services.TestService;
import pt.haslab.specassistant.util.FutureUtil;

import java.util.List;
import java.util.Map;

@Path("/debug-hint")
public class DebugHint {
    @Inject
    GraphManager graphManager;
    @Inject
    TestService testService;

    /**
     * This method is used to set up the graphs for the first time.
     * It will generate the graphs from the exercises for the given models.
     * It will also compute the policies for the graphs and debloat them.
     */
    @GET
    @Path("/setup-graphs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response genGraphs(List<String> model_ids, @DefaultValue("Unkown") @QueryParam("prefix") String prefix, @BeanParam YearRange yearRange) {
        testService.autoSetupJob(model_ids, prefix, yearRange);
        return Response.ok("Setup in progress.").build();
    }


    @GET
    @Path("/setup-multiple-graphs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response genGraphs(Map<String, List<String>> model_ids, @BeanParam YearRange yearRange) {
        FutureUtil.forEachOrderedAsync(model_ids.entrySet(), e -> testService.autoSetupJob(e.getValue(), e.getKey(), yearRange));
        return Response.ok("Setup in progress.").build();
    }

    @GET
    @Path("/debug-drop-everything")
    @Produces(MediaType.APPLICATION_JSON)
    public Response debug() {
        graphManager.dropEverything();
        return Response.ok().build();
    }

    @GET
    @Path("/do-tar-for-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tarModel(@QueryParam("model_id") String model_id, @BeanParam YearRange yearRange) {
        testService.testChallengeWithTAR(model_id, yearRange::testDate);
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
    @Path("/do-tar-for-all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tar(@BeanParam YearRange yearRange) {
        testService.testAllChallengesWithTAR(yearRange::testDate);
        return Response.ok("Test Started").build();
    }

    @GET
    @Path("/test-model")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stressHints(@QueryParam("model_id") String model_id, @BeanParam YearRange yearRange) {
        testService.specTestModel(model_id, yearRange::testDate);
        return Response.ok().build();
    }

    @GET
    @Path("/test-all-models")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stressAll(@BeanParam YearRange yearRange) {
        testService.testAllChallengesWithSpec(yearRange::testDate);
        return Response.ok().build();
    }
}
