/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mmoyo.analysis.comp;

import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;

/**
 * invokes a standard MATSim transit simulation, pt occupancy analysis is done with configurable time bin size, selected lines, per stop zone 
 */
public class ControlerLauncher {
	
	public static void main(String[] args) {
		String configFile; 
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../../";
		}

		Config config = null;
		config = ConfigUtils.loadConfig(configFile);

		final Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		//add analyzer for specific bus line and stop Zone conversion
		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		controler.getConfig().addModule(ccc) ;
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(false); 
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);  
		
		controler.run();
		
	} 

}
