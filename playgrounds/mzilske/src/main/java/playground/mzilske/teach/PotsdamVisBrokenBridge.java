package playground.mzilske.teach;

import org.matsim.run.OTFVis;

public class PotsdamVisBrokenBridge implements Runnable {
	
	public static void main(String[] args) {
		PotsdamVisBrokenBridge potsdamRun = new PotsdamVisBrokenBridge();
		potsdamRun.run();
	}
	
	@Override
	public void run() {
		OTFVis.main(new String[]{"inputs/brandenburg/config-live-broken-bridge.xml"});
	}
}