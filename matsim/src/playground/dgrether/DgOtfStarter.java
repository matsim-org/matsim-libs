/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
		String base = "/Volumes/data/work/cvsRep/vsp-cvs/runs/run";
		 
		String network = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/network/ivtch-osm.xml";
		
//		String runNumber = "486";
//	String mvi = "1000.T.mvi";
		
		//base case without beta_earlydeparture 
//		String file = "465/it.500/500.T.mvi";

		//base case with beta_earlydeparture beta_pt= -3
//		String file = "560/500.T.mvi";
		
		//base case ersa 
//		String file = "583/run583.it800.T.mvi";
		
		//toll case ersa
		String file = "585/run585.it800.T.mvi";
		
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
		
		
		

		
		String filename = base + file;
		
		if (filename.endsWith(".veh.gz")) {
			tVehStarter(filename, network);
		}
		else if (filename.endsWith(".mvi")) {
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
