package playground.wrashid.PDES;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Executor executor = Executors.newFixedThreadPool(2);
		for (int i=0;i<1000;i++){
			executor.execute (new TaskA (i));
		}
	}

}
