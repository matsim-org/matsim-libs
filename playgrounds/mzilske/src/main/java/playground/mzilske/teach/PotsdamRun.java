package playground.mzilske.teach;

import org.matsim.core.api.experimental.controller.Controller;

public class PotsdamRun implements Runnable {
	
	public static void main(String[] args) {
		PotsdamRun potsdamRun = new PotsdamRun();
		potsdamRun.run();
	}
	
	@Override
	public void run() {
		final Controller controller = new Controller("inputs/brandenburg/config.xml");
		controller.setOverwriteFiles(true);
		controller.run();
	}
}