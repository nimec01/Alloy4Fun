package pt.haslab.alloy4fun.util;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4viz.AlloyInstance;
import edu.mit.csail.sdg.alloy4viz.StaticInstanceReader;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

public class AlloyUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlloyUtil.class);

    public static CompModule parseModel(String model) throws IOException, Err {
        return parseModel(model, A4Reporter.NOP);
    }

    public static CompModule parseModel(String model, A4Reporter rep) throws IOException, Err {
        String prefix_name = "thr-%s.alloy_heredoc".formatted(Thread.currentThread().threadId());
        File file = File.createTempFile(prefix_name, ".als");
        file.deleteOnExit();

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
            out.write(model.getBytes());
            out.flush();
        }
        return CompUtil.parseEverything_fromFile(rep, null, file.getAbsolutePath());
    }


    public static AlloyInstance parseInstance(A4Solution solution) throws IOException, Err {
        return parseInstance(solution, 0);
    }

    public static AlloyInstance parseInstance(A4Solution solution, Integer state) throws IOException, Err {
        String prefix_name = "thr-%s.a4f".formatted(Thread.currentThread().threadId());
        File file = File.createTempFile(prefix_name, ".als");
        file.deleteOnExit();
        solution.writeXML(file.getAbsolutePath());

        return StaticInstanceReader.parseInstance(file.getAbsoluteFile(), state);
    }

    public static List<AlloyInstance> parseInstances(A4Solution solution, Integer count) throws IOException, Err {
        return parseInstances(solution, 0, count);
    }

    public static List<AlloyInstance> parseInstances(A4Solution solution, Integer from, Integer to) throws IOException, Err {
        String prefix_name = "thr-%s.a4f".formatted(Thread.currentThread().threadId());
        File file = File.createTempFile(prefix_name, ".als");
        file.deleteOnExit();
        solution.writeXML(file.getAbsolutePath());

        return IntStream.range(from, to).boxed().map(i -> StaticInstanceReader.parseInstance(file.getAbsoluteFile(), i)).toList();
    }

    public static A4Options defaultOptions(CompModule world) {
        A4Options opt = new A4Options();
        opt.originalFilename = Path.of(world.path()).getFileName().toString();
        opt.solver = A4Options.SatSolver.SAT4J;

        return opt;
    }


}
