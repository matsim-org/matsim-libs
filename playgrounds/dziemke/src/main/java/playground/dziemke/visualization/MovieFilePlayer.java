package playground.dziemke.visualization;

import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.vis.otfvis.OTFClientFile;

/**
 * @author dziemke
 */
public class MovieFilePlayer {
	
	public static void main(String[] args) {
		// Parameters
		String mviFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/output/counts-stations-50/otfvis.mvi";
		boolean createScreenshots = true; // Snapshots will be stored at run directory
		
		// Run
		if (createScreenshots == false) {
			OTFVis.playMVI(mviFile);
		} else {
//			new OTFClientFile(mviFile).run();
			new MyOTFClientFile(mviFile).run();
		}
	}
}