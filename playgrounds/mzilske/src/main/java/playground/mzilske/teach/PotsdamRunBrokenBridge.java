package playground.mzilske.teach;

import org.matsim.core.api.experimental.controller.Controller;

public class PotsdamRunBrokenBridge implements Runnable {
	
	public static void main(String[] args) {
		PotsdamRunBrokenBridge potsdamRun = new PotsdamRunBrokenBridge();
		potsdamRun.run();
	}
	
	@Override
	public void run() {
		final Controller controller = new Controller("inputs/brandenburg/config-broken-bridge.xml");
		controller.setOverwriteFiles(true);
		controller.run();
	}
}