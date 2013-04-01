/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mmoyo.taste_variations;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.pt.CadytsPtConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.mmoyo.utils.DataLoader;

public class SVD_CtrlLauncher {

	public static void main(String[] args) {
		String configFile;
		String svdSolutionFile;
		if (args.length>0){
			configFile = args[0];
			svdSolutionFile = args[1];
		}else{
			configFile = "../../";
			svdSolutionFile = "../../";
		}
		
		//load data
		DataLoader dataLoader = new DataLoader();
		Scenario scn =dataLoader.loadScenario(configFile);
		Population pop = scn.getPopulation();
		Network net = scn.getNetwork();
		TransitSchedule schedule = scn.getTransitSchedule();
		
		final Controler controler = new Controler(scn);
		controler.setOverwriteFiles(true);
		
		CadytsPtConfigGroup ccc = new CadytsPtConfigGroup() ;
		controler.getConfig().addModule(CadytsPtConfigGroup.GROUP_NAME, ccc) ;
		
		Map <Id, SVDvalues> svdMap = new SVDValuesAsObjAttrReader(pop.getPersons().keySet()).readFile(svdSolutionFile); 
		controler.setScoringFunctionFactory(new SVDScoringfunctionFactory(svdMap, net, schedule));
		
		//add analyzer for specific bus line
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(false);
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);  
		
		controler.run();
	}
	
}