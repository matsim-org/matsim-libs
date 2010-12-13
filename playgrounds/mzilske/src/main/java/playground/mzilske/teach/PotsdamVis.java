package playground.mzilske.teach;

import org.matsim.run.OTFVis;

public class PotsdamVis implements Runnable {
	
	public static void main(String[] args) {
		PotsdamVis potsdamRun = new PotsdamVis();
		potsdamRun.run();
	}
	
	@Override
	public void run() {
		OTFVis.main(new String[]{"mzilske/inputs/brandenburg/config-live.xml"});
	}
}