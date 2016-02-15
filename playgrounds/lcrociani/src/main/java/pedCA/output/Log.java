package pedCA.output;

public class Log {

	public static void log(String message){
		print("LOG: "+message);
	}
	
	public static void warning(String message){
		print("WARNING: "+message);
	}

	public static void step(int step) {
		print("---------- STEP "+step+" ----------");	
	}

	public static void debug(String message) {
		print("DEBUG: "+message);
		
	}
	
	public static void print(String message){
		System.out.println(message);
	}

	public static void error(String string) {
		print("ERROR: "+string);	
	}
	
}
