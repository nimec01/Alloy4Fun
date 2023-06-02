package pt.haslab.alloy4fun.resources;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import pt.haslab.alloy4fun.data.models.Session;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.repositories.ModelRepository;
import pt.haslab.alloy4fun.services.HintService;
import pt.haslab.alloy4fun.services.SessionService;
import pt.haslab.alloy4fun.util.AlloyExprNormalizer;
import pt.haslab.alloy4fun.util.AlloyExprStringify;
import pt.haslab.alloy4fun.util.AlloyUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Path("/hint")
public class AlloyHint {

    private static final Logger LOG = Logger.getLogger(AlloyHint.class);

    @Inject
    HintService hintService;

    @Inject
    SessionService sessionManager;

    @Inject
    ModelRepository mm;

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
    public Response debug(@RestQuery String id) throws IOException, ExecutionException, InterruptedException {
        //List<Model> mmmm = mm.findAll().page(Page.ofSize(3)).stream().toList();
        //hintService.generateExercisesFromAllCommands(hintService.getOriginal(id));
        //hintService.countWalkTree(hintService.getOriginal(id)).get();
        //var f = mm.findByGraphIdAndFormula(8L, Map.of("this/inv7", "(all ref0:(one Product)|ref0 . (^ (Product <: parts)) in Dangerous => ref0 in Dangerous)"));


        mm.find(new Document("sat", new Document("$gte", 0))).page(1, 1000).stream().forEach(m -> {
            CompModule world;
            try {
                world = AlloyUtil.parseModel(m.code);
                try {

                    Func func = world.getAllFunc().get(m.cmd_i);

                    Expr original = func.getBody();

                    Expr norm = AlloyExprNormalizer.normalize(original);

                    String mid = AlloyExprStringify.stringify(norm);

                    if (Objects.equals(mid, "true"))
                        mid = "";

                    Expr parsed = CompUtil.parseOneExpression_fromString(world, mid);


                    String bread = "";
                } catch (Err e) {
                    throw new RuntimeException(e);
                }
            } catch (Err | IOException e) {
            }

        /*
        Expr aaa = CompUtil.parseOneExpression_fromString(world, "all x : File, y : x.link | no y ").deNOP();

        Expr eee = CompUtil.parseOneExpression_fromString(world, "all x, y : File | no y ").deNOP();
        Expr uuu = CompUtil.parseOneExpression_fromString(world, "{x, y : File | no y }").deNOP();

        Expr iii = CompUtil.parseOneExpression_fromString(world, AlloyExprStringify.stringify(AlloyExprNormalizer.normalize(eee))).deNOP();
        */
        });

        return Response.ok().build();
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HintRequest {
        public String sessionId;
        public String originId;
    }
}
