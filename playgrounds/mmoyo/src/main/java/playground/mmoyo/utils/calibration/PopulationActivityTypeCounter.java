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

package playground.mmoyo.utils.calibration;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.io.TextFileWriter;
import playground.mmoyo.utils.DataLoader;

public class PopulationActivityTypeCounter  implements PersonAlgorithm {
	private String TAB = "\t";
	private String CRL = "\n";
	private StringBuffer sBuff = new StringBuffer();
	
	@Override
	public void run(Person person) {
		Map <String, Integer> typeMap = new TreeMap <String, Integer>();
		for (Plan plan: person.getPlans()){
			String type = ((PlanImpl)plan).getType();
			if (!typeMap.keySet().contains(type)){
				typeMap.put(type, 0);
			}
			
			int oldValue= typeMap.get(type);  
			typeMap.put(type, oldValue + 1);
		}
		sBuff.append(CRL +  person.getId());
		for(Map.Entry <String,Integer> entry: typeMap.entrySet() ){
			sBuff.append(TAB + entry.getKey() + TAB + entry.getValue());
		}
	}

	public static void main(String[] args) {
		String popFile;
		String netFilePath;
		
		if (args.length>0){
			popFile = args[0];
			netFilePath = args[1];
		}else{
			popFile = "../../input/temp/sameNullScore90.plans.xml.gz";
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		}

		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario(); 
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFilePath);
		
		PopulationActivityTypeCounter typeCounter = new PopulationActivityTypeCounter();
		new PopSecReader(scn, typeCounter).readFile(popFile);
	
		//write the analysis in the same path as population file
		File file = new File (popFile);
		new TextFileWriter().write(typeCounter.sBuff.toString(), file.getPath() + "ActTypeCount.txt" , false);
	}

}
