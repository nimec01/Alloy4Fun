package services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4compiler.ast.Command;
import edu.mit.csail.sdg.alloy4compiler.ast.Module;
import edu.mit.csail.sdg.alloy4compiler.parser.CompUtil;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Options;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;
import edu.mit.csail.sdg.alloy4compiler.translator.TranslateAlloyToKodkod;

@WebService
public class AlloyService {

	@Resource
	private static HashMap<String, ClientSession> answers = new HashMap<>();

	@WebMethod
	public String getProjection(String sessid, String[] type) {
		return answers.get(sessid).projectOver(type);
	} 

	@WebMethod 
	public String getInstance(String model, final String sessionId, int instanceNumber, String commandLabel,
			boolean forceInterpretation) {
		if (answers.containsKey(sessionId) && !forceInterpretation) {
			answers.get(sessionId).setIteration(instanceNumber);

			System.out.println(model);
			System.out.println(commandLabel);

			return answers.get(sessionId).getInstance();
		} else {
			A4Reporter rep = new A4Reporter() {
				@Override
				public void warning(ErrorWarning msg) {
					System.out.println(msg.getLocalizedMessage());
					System.out.print("Relevance Warning:\n" + (msg.toString().trim()) + "\n\n");
					System.out.flush();
				}
			};
			String filename = null;
			File file = null;
			try {
				file = new File(System.getProperty("java.io.tmpdir") + sessionId + ".als");
				file.createNewFile();
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(model);
				bw.flush();
				filename = file.getAbsolutePath();
				bw.close();
			} catch (Exception e1) {
				e1.printStackTrace();
				return e1.getMessage();
			}
			Module world = null;
			try {
				world = CompUtil.parseEverything_fromFile(rep, null, filename);

			} catch (Err e) {
				String message = e.msg.replace("\"", "\'");
				return ("{\"syntax_error\": true, \"line\":" + e.pos.y + ", \"column\": " + e.pos.x + ", \"msg\" :\""
						+ message + "\"}").replace("\n", "");
			}

			A4Options options = new A4Options();

			options.solver = A4Options.SatSolver.SAT4J;
			for (Command command : world.getAllCommands()) {
				if (command.label.equals(commandLabel)) {
					A4Solution ans;
					try {
						ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command,
								options);

					} catch (Err e) {
						return e.getMessage();
					}
					if (ans.satisfiable()) {
						answers.put(sessionId, new ClientSession(ans, file, sessionId, instanceNumber));
						ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
						scheduler.schedule(new Runnable() {
							public void run() {
								answers.get(sessionId).deleteModel();
								answers.remove(sessionId);
							}
						}, 7200, TimeUnit.SECONDS);
						// System.out.println(answers.get(sessionId).getInstance());
						return answers.get(sessionId).getInstance();
					} else
						return "{\"unsat\" : true}";
				}
			}
			return "";
		}
	}
}
