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
package playground.dgrether.cmcf;

import org.matsim.run.OTFVis;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class CmcfOtfStarter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String base = DgPaths.VSPCVSBASE + "runs/run";
		 
		String networkFileOld = DgPaths.SCMWORKSPACE + "studies/dgrether/cmcf/daganzoNetworkOldRenamed.xml";

		String networkFile 	= networkFileOld;
		
		String file = "650/it.550/550.otfvis.mvi";

//		String file = "651/it.550/550.otfvis.mvi";

//		String file = "652/it.550/550.otfvis.mvi";

//		String file = "653/it.550/550.otfvis.mvi";

		
		String filename = base + file;
		
		OTFVis.main(new String[] {filename});
	}

}
