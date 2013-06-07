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
import playground.mmoyo.utils.DataLoader;

/**
 * Writes a plans file where each plan has the given utility correction as score 
  */

public class SetUtl2pop {
	
	protected String setUtCorrections(String popFile, Map <Id, double[]> correcMap, String netFile){
		//load population
		DataLoader loader = new DataLoader();
		Scenario sn = loader.readNetwork_Population(netFile, popFile);
		Population pop = sn.getPopulation();
	
		for (Person person : pop.getPersons().values()){
			//((PersonImpl)person).setSelectedPlan(person.getPlans().get(0));
			
			double[] correctArray = correcMap.get(person.getId());
			if(correctArray!=null){
				int utlsNum = correctArray.length;
				
				//validation
				if(person.getPlans().size() != utlsNum){
					throw new RuntimeException("the number of available utilities is different to the number of plans: " + person.getId() ) ;
				}
				for (int i=0;i<utlsNum;i++){
					person.getPlans().get(i).setScore(correctArray[i]);  // correcMap [utl0, utl1, utl2,... utln] 
				}
	 			
			}else{
				for(Plan plan: person.getPlans()){
					plan.setScore(0.0);
				}
			}
		}
		
		Network net = new DataLoader().readNetwork(netFile);
		String corrPopFile = popFile + "WithUtilCorrections.xml.gz";
		System.out.println("writing output plan file..." + corrPopFile);
		new PopulationWriter(pop, net).write(corrPopFile);
		return corrPopFile;
	}
	
	public static void main(String[] args) {
		String inputUtlCorrectionsFile;
		String popFile;
		String netFile;
		if(args.length>0){
			popFile =args[0];
			inputUtlCorrectionsFile= args[1];
			netFile= args[2];
		}else{
			popFile ="../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/poponlyM44.xml.gz"; //"../../input/newDemand/ptLinecountsScenario/mergedPlans.xml.gztracked.xml.gzoverDemandPlanSample.xml";
			inputUtlCorrectionsFile= "../../input/choiceM44/m44utls.txt"; 	////"../../input/newDemand/ptLinecountsScenario/utlCorrections24hrs.txt";
			netFile= "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		}

		//read utilities corrections
		UtilCorrectonReader reader= new UtilCorrectonReader();
		try {
			reader.readFile(inputUtlCorrectionsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		new SetUtl2pop().setUtCorrections(popFile, reader.getCorrecMap(), netFile);
	}
	
}
