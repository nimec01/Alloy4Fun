import javax.ws.rs.core.Application;

import edu.mit.csail.sdg.translator.A4Solution;

import java.util.HashMap;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class RestApplication extends Application {
	
	private static HashMap<String, A4Solution> answers = new HashMap<String, A4Solution>();
	private static HashMap<String, Integer> count = new HashMap<String, Integer>();
    
	public static void remove(String str) {
		answers.remove(str);
		count.remove(str);
	}

	public static void add(String str, A4Solution ans) {
		answers.put(str,ans);
		count.put(str,0);
	}

	public static A4Solution getSol(String str) {
		return answers.get(str);
	}

	public static int getCnt(String str) {
		return count.get(str);
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

