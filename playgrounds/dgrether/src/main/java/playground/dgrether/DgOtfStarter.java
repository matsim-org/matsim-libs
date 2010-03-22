/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dgrether;

import org.matsim.run.OTFVis;


/**
 * @author dgrether
 *
 */
public class DgOtfStarter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String base = "/media/data/work/repos/runs-svn/run";
		 
		String network = DgPaths.IVTCHNET;
		
//		String runNumber = "486";
//	String mvi = "1000.T.mvi";
		
		//base case without beta_earlydeparture 
//		String file = "465/it.500/500.T.mvi";

		//base case with beta_earlydeparture beta_pt= -3
//		String file = "560/500.T.mvi";
		
		//base case ersa 
		String file = "583/run583.it800.T.mvi";
		
		//bkickscoring
		file = "881/it.3000/881.3000.Zurich.otfvis.mvi";
//		file = "749/it.2000/749.2000.Zurich.otfvis.mvi";
//		file = "662/it.500/500.events.mvi";
//		file = "328/output/movie.mvi";
//		file = "1004/output/movie.it500.mvi";
		
		//toll case ersa
//		String file = "585/run585.it800.T.mvi";
		
		//base case stuck=false, no earlyDeparture
//		String file = "596/it.500/500.T.mvi";
//		String file = "596/it.550/550.T.mvi";
		
		//base case portland beta_pt = -6 (nearly 499!)
//  String file = "551/500.T.mvi
		
//	portland roadpricing 2 euro/km
//	String file = "555/500.T.mvi
		
		//cmcf runs for tobias harks
//		String file = "608/it.100/100.events.mvi";
		
		//cmcf runs full activity chains no time first route cmcf
//		String file = "610/it.100/100.events.mvi";
		//cmcf runs full activity chains no time no cmcf routes
//		String file = "612/it.100/100.events.mvi";
		
		//demo bmbf seminar 08
//		String file = "646/it.1000/config.xml";
		
		String filename = base + file;
		
		
		//unit test for coopers withinday:
//		String filename = "/Volumes/data/work/svnWorkspace/matsim/test/output/org/matsim/integration/withinday/CoopersBerlinIntegrationTest/testBerlinReducedSB/ITERS/it.0/0.otfvis.mvi";

	//run 700 first run of estimated utility function
//		filename = "/Users/dgrether/Desktop/bkickRuns/run700/1000.events.mvi";
		
//		filename = "/Users/dgrether/Desktop/bkickRuns/run702/500.events.mvi";
//		filename = "/Users/dgrether/Desktop/bkickRuns/run703/500.events.mvi";
//		filename = "/Users/dgrether/Desktop/bkickRuns/run704/500.events.mvi";
//		filename = DgPaths.RUNBASE + "run709/it.1000/1000.analysis/709.vis.mvi";
//		filename = DgPaths.RUNBASE + "run710/it.1000/1000.analysis/710.vis.mvi";
		
//		filename = DgPaths.RUNBASE + "run710/it.1000/1000.analysis/710.vis.mvi";
		
//		filename = network;
		
//		filename = "/home/dgrether/svnworkspace/matsim/test/input/playground/benjamin/BKickRouterTestIATBR/network.xml";
//		filename = "/media/data/work/programming/rubyWorkspace/demandGeneration/network.xml.gz";
		
      filename = "/home/dgrether/shared-svn/studies/countries/de/prognose_2025/demand/0.otfvis.mvi";

      //really old otf shit
//    filename = DgPaths.VSPCVSBASE + "runs/run493/99.T.mvi";
		
		if (filename.endsWith(".veh.gz")) {
			tVehStarter(filename, network);
		}
		else if (filename.endsWith(".mvi") || filename.endsWith("xml") || filename.endsWith(".xml.gz")) {
			OTFVis.main(new String[] {filename});
		}
		else {
			throw new IllegalArgumentException("files ending with " + filename.substring(filename.length() - 4) + " not supported!");
		}
	}
	
	private static void tVehStarter(String tveh, String network) {
		String[] params = {tveh, network};		
		OTFVis.main(params);
	}
	
	

}
