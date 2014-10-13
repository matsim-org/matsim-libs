package playground.dziemke.visualization;

import org.matsim.contrib.otfvis.OTFVis;

/**
 * @author dziemke
 */
public class MovieFilePlayer {
	
	public static void main(String[] args) {
		// Parameters
		String runId = "run_132";
		int iteration = 150;
		boolean createScreenshots = false;
		// Snapshots will be stored at "D:/Workspace/playgrounds/dziemke"
		
		// Input file			
		String mviFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iteration
				+ "/" + runId + "." + iteration + ".otfvis.mvi";
		
		// Play
		if (createScreenshots == false) {
			OTFVis.playMVI(mviFile);
		} else {
			new MyOTFClientFile(mviFile).run();
		}
	}
}
