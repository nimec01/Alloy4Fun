package pt.haslab.alloyaddons;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.alloy4viz.AlloyInstance;
import edu.mit.csail.sdg.alloy4viz.StaticInstanceReader;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import pt.haslab.alloyaddons.exceptions.UncheckedIOException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Util {

    public static CompModule parseModel(String model) throws UncheckedIOException, Err {
        return parseModel(model, A4Reporter.NOP);
    }

    public static CompModule parseModel(String model, A4Reporter rep) throws UncheckedIOException, Err {
        try {
            String prefix_name = "thr-%s.alloy_heredoc".formatted(Thread.currentThread().threadId());
            File file = File.createTempFile(prefix_name, ".als");
            file.deleteOnExit();

            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
                out.write(model.getBytes());
                out.flush();
            }
            return CompUtil.parseEverything_fromFile(rep, null, file.getAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static AlloyInstance parseInstance(A4Solution solution) throws IOException, Err {
        return parseInstance(solution, 0);
    }

    public static Optional<Func> getFunByLabelName(CompModule world, String label) {
        String label_name = label.replace("this/", "");
        return world.getAllFunc()
                .makeConstList()
                .stream()
                .filter(x -> x.label.replace("this/", "").equals(label_name))
                .findFirst();
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


    public static Expr parseOneExprFromString(CompModule world, String value) {
        try {
            return world.parseOneExpressionFromString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Stream<Func> streamFuncsWithNames(Collection<Func> allFunctions, Set<String> targetNames) {
        return allFunctions.stream().filter(x -> targetNames.contains(x.label.replace("this/", "")));
    }

    public static String stripThisFromLabel(String str) {
        if (str != null)
            str = str.replace("this/", "");
        return str;
    }

    public static Map<String, Set<String>> getSecretFunctionTargetsOf(CompModule module, List<Pos> secret_positions) {
        Map<String, Set<String>> result = new HashMap<>();

        module.getAllCommands().forEach(cmd -> {
            if (posIn(cmd.pos, secret_positions)) {
                Set<String> targets = FunctionSearch
                        .search(f -> f.pos.sameFile(cmd.pos) && notPosIn(f.pos, secret_positions), cmd.formula)
                        .stream()
                        .map(f -> f.label)
                        .map(Util::stripThisFromLabel)
                        .collect(Collectors.toSet());
                if (!targets.isEmpty())
                    result.put(stripThisFromLabel(cmd.label), targets);
            }
        });
        return result;
    }

    public static boolean posIn(Pos pos, Collection<Pos> collection) {
        return collection.stream().anyMatch(p -> p.contains(pos));
    }

    public static boolean notPosIn(Pos pos, Collection<Pos> collection) {
        return collection.stream().noneMatch(p -> p.contains(pos));
    }

    public static List<Pos> offsetsToPos(String code, List<Integer> offsets) {
        return offsetsToPos("alloy_heredoc.als", code, offsets);
    }

    public static List<Pos> offsetsToPos(String filename, String code, List<Integer> offsets) {
        List<Integer> integers = offsets.stream().sorted().distinct().toList();
        Pattern p = Pattern.compile("\\n");
        Matcher m = p.matcher(code);

        List<Pos> result = new ArrayList<>(integers.size());

        int line = 1;
        int curr = 0;
        while (m.find() && curr < integers.size()) {
            int t0 = m.end();
            for (; curr < integers.size() && integers.get(curr) < t0; curr++)
                result.add(new Pos(filename, integers.get(curr) - t0, line));
            line++;
        }

        return result;
    }


}
