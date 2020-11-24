package pt.haslab.alloy4fun;
import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.translator.A4Solution;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class RestApplication extends Application {
	
	private static Logger LOGGER = LoggerFactory.getLogger(AlloyGetInstances.class);
	
	private static Map<String, List<A4Solution>> answers = new HashMap<String, List<A4Solution>>();
	private static Map<String, Integer> count = new HashMap<String, Integer>();
	private static Map<String, Command> cmd = new HashMap<String, Command>();
    
	public static void remove(String str) {
		answers.remove(str);
		count.remove(str);
		cmd.remove(str);
		LOGGER.info("Closing session for "+str+", now active: "+answers.size());
	    Runtime rt = Runtime.getRuntime();
	    long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		LOGGER.debug("Memory used: " + usedMB+"mb.");
		LOGGER.debug("Number of active threads: " + Thread.getAllStackTraces());
	}

	public static void add(String str, A4Solution ans, Command cm) {
		answers.put(str,new ArrayList<A4Solution>());
		answers.get(str).add(ans);
		count.put(str,0);
		cmd.put(str,cm);
		LOGGER.info("Opening session for "+str+", now active: "+answers.size());
	    Runtime rt = Runtime.getRuntime();
	    long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		LOGGER.debug("Memory used: " + usedMB+"mb.");
		LOGGER.debug("Number of active threads: " + Thread.getAllStackTraces());
	}

	public static A4Solution getSol(String str) {
		List<A4Solution> sols = answers.get(str);
		return sols.get(sols.size()-1);
	}

	public static A4Solution getSol(String str, int n) {
		List<A4Solution> sols = answers.get(str);
		return sols.get(n);
	}

	public static int getCnt(String str) {
		return count.get(str);
	}

	public static Command getCommand(String str) {
		return cmd.get(str);
	}

	public static void next(String str) throws Err {
		answers.get(str).add(getSol(str).next());
		count.put(str,count.get(str)+1);
	}

	public static boolean alive(String str) {
		return answers.containsKey(str);
	}

	public RestApplication() {
    	
    }
}

