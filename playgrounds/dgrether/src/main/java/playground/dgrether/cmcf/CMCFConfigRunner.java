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

import org.matsim.core.controler.Controler;
import org.matsim.run.OTFVis;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class CMCFConfigRunner {

	
	private static final String configSO = DgPaths.SCMWORKSPACE + "studies/dgrether/cmcf/daganzoConfig_cmcf_so.xml";
	
	private static final String configUE = DgPaths.SCMWORKSPACE + "studies/dgrether/cmcf/daganzoConfig_cmcf_ue.xml";
	
	private static final String config	= configUE;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new Controler(config);
		controler.run();
		
		String[] args2 = {controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/" + controler.getConfig().controler().getLastIteration() + ".otfvis.mvi"};
		
		OTFVis.main(args2);

	}

}
