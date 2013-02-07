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
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.StopZoneAnalysisCtrlListener;
import playground.mmoyo.utils.DataLoader;

public class PersonalizedTasteControler {

	public static void main(String[] args) {
		String configFile;
		String svdSolutionFile;
		if (args.length>0){
			configFile = args[0];
			svdSolutionFile = args[1];
		}else{
			configFile = "../../ptManuel/calibration/my_config.xml";
			svdSolutionFile = "../../input/choiceM44/500.cadytsCorrectionsNsvdValues.xml.gz";
		}
		
		//load data
		DataLoader dataLoader = new DataLoader();
		Scenario scn =dataLoader.loadScenario(configFile);
		Population pop = scn.getPopulation();
		Network net = scn.getNetwork();
		TransitSchedule schedule = scn.getTransitSchedule();
		
		final Controler controler = new Controler(scn);
		controler.setOverwriteFiles(true);
		
		Map <Id, SVDvalues> svdMap = getSvdValuesMap(pop, svdSolutionFile); 
		controler.setScoringFunctionFactory(new PersonalizedTasteScoring(svdMap, net, schedule));
		
		//add analyzer for specific bus line
		StopZoneAnalysisCtrlListener stopZoneAnalysisCtrlListener = new StopZoneAnalysisCtrlListener(controler);
		controler.addControlerListener(stopZoneAnalysisCtrlListener);  
		
		controler.run();
	}
	
	private static Map <Id, SVDvalues> getSvdValuesMap(final Population pop, final String svdSolutionFile){
		Map <Id, SVDvalues> svdMap = new TreeMap <Id, SVDvalues>();
		
		ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(attributes);
		reader.parse(svdSolutionFile);
		final String STR_wWalk="wWalk";
		final String STR_wTime="wTime";
		final String STR_wDista="wDista";
		final String STR_wChng="wChng";
		for(Id id : pop.getPersons().keySet()){
			String strId = id.toString();
			double wWalk = ((Double) attributes.getAttribute(strId, STR_wWalk)).doubleValue();
			double wTime = ((Double) attributes.getAttribute(strId, STR_wTime)).doubleValue();
			double wDista = ((Double) attributes.getAttribute(strId, STR_wDista)).doubleValue();
			double wChng = ((Double) attributes.getAttribute(strId, STR_wChng)).doubleValue();
			SVDvalues svdValues = new SVDvalues(id, wWalk, wTime, wDista, wChng); 
			svdMap.put(id, svdValues);
		}
		return svdMap;
	}
	
	
	

}