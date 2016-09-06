package playground.dziemke.visualization;

import org.matsim.contrib.otfvis.OTFVis;

/**
 * @author dziemke
 */
public class MovieFilePlayer {
	
	public static void main(String[] args) {
		// Parameters
		String mviFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/output/counts-stations-50/otfvis.mvi";
		boolean createScreenshots = false; // Snapshots will be stored at run directory
		
		// Run
		if (createScreenshots == false) {
			OTFVis.playMVI(mviFile);
		} else {
			new MyOTFClientFile(mviFile).run();
		}
	}
}