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

import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PersonImpl;

import playground.mmoyo.utils.DataLoader;

public class SetUtl2pop {

	private static void setUtCorrections(String popFile, Map <Id, double[]> correcMap, String netFile){
		//load population
		DataLoader loader = new DataLoader();
		Scenario sn = loader.readNetwork_Population(netFile, popFile);
		Population pop = sn.getPopulation();
		for (Person person : pop.getPersons().values()){
			for (int i=0;i<=3;i++){
				double[] correctArray = correcMap.get(person.getId());
				Plan plan = person.getPlans().get(i);
				plan.setScore(correctArray[i+1]); // correcMap [selectedIndx, utl0, utl1, utl2, utl3] 
				if(i== (int)correctArray[0]){
					((PersonImpl)person).setSelectedPlan(plan);
				}
			}
		}
		
		Network net = new DataLoader().readNetwork(netFile);
		String corrPopFile = popFile + "WithUtilCorrections.xml.gz";
		System.out.println("writing output plan file..." + corrPopFile);
		new PopulationWriter(pop, net).write(corrPopFile);
	}
	
	public static void main(String[] args) {
		String uiCorrectionsFile;
		String popFile;
		String netFile;
		if(args.length>0){
			popFile =args[0];
			uiCorrectionsFile= args[1];
			netFile= args[2];
		}else{
			popFile ="../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/poponlyM44.xml.gz"; //"../../input/newDemand/ptLinecountsScenario/mergedPlans.xml.gztracked.xml.gzoverDemandPlanSample.xml";
			uiCorrectionsFile= "../../input/choiceM44/m44utls.txt"; 	////"../../input/newDemand/ptLinecountsScenario/utlCorrections24hrs.txt";
			netFile= "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		}

		//read utilities corrections
		UtilCorrectonReader reader= new UtilCorrectonReader();
		try {
			reader.readFile(uiCorrectionsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		setUtCorrections(popFile, reader.getCorrecMap(), netFile);
	}
	
}
