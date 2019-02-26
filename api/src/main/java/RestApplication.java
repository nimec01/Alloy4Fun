import javax.ws.rs.core.Application;

import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.ast.Command;

import java.util.HashMap;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class RestApplication extends Application {
	
	private static HashMap<String, A4Solution> answers = new HashMap<String, A4Solution>();
	private static HashMap<String, Integer> count = new HashMap<String, Integer>();
	private static HashMap<String, Command> cmd = new HashMap<String, Command>();
    
	public static void remove(String str) {
		answers.remove(str);
		count.remove(str);
		cmd.remove(str);
	}

	public static void add(String str, A4Solution ans, Command cm) {
		answers.put(str,ans);
		count.put(str,0);
		cmd.put(str,cm);
	}

	public static A4Solution getSol(String str) {
		return answers.get(str);
	}

	public static int getCnt(String str) {
		return count.get(str);
	}

	public static Command getCommand(String str) {
		return cmd.get(str);
	}

	public static void next(String str) {
		answers.put(str,answers.get(str).next());
		count.put(str,count.get(str)+1);
	}

	public static boolean alive(String str) {
		return answers.containsKey(str);
	}

	public RestApplication() {
    	
    }
}

