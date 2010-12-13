/* *********************************************************************** *
 * project: org.matsim.*
 * ptRouteAdder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.ptRouterAdapted.precalculation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;

import playground.mmoyo.utils.DataLoader;

public class ptRouteAdder {

	/**
	 * Reads a population an adds new routes from another pop file
	 */
	public static void main(String[] args) {
		String plansDir = "../playgrounds/mmoyo/output/precalculation/routed3150/";
		
		String basePopfile = "../playgrounds/mmoyo/output/precalculation/FragPersArea_812020_3.xml";
		File plansDirfile = new File(plansDir);
		String [] plansArray = plansDirfile.list();
		Map <Id, List<Id>> clonsMap = new TreeMap <Id, List<Id>>();  //saves the original agent and his/her clons
		
		DataLoader loader= new DataLoader ();
		Population basePop = loader.readPopulation(basePopfile); 
		Population newPop= new ScenarioImpl().getPopulation();
		
		PlanPTRoutesComparator planPTRoutesComparator = new PlanPTRoutesComparator();
		
		for (int i=0; i< plansArray.length; i++){
			String fileName= plansArray[i];
			String strId = fileName.substring(11, fileName.length()-9);
			String filePath = plansDir + fileName;
			Population popX = loader.readPopulation( filePath);
		
			for (Person person : basePop.getPersons().values()){
				Person personX = popX.getPersons().get(person.getId());
				Plan planX = personX.getSelectedPlan();
				
				if(!clonsMap.containsKey(person.getId())){
					clonsMap.put(person.getId(), new ArrayList<Id>());
				}
				List<Id> clonsList = clonsMap.get(person.getId());

				boolean exists= false;
				for (Id idClon: clonsList){ //validates that this pt  connection does not exist yet for this person
					Plan clonPlan = newPop.getPersons().get(idClon).getSelectedPlan();
					exists = exists || planPTRoutesComparator.haveSamePtRoutes(clonPlan, planX);
					if(exists) break;
				}
				
				if (!exists){
					String strNewId = personX.getId().toString() +  strId;
					Id newId = new IdImpl(strNewId);
					personX.setId(newId);
					newPop.addPerson(personX);
					clonsMap.get(person.getId()).add(newId);
				}
				
			}
			
			fileName= null; strId = null;  filePath = null; popX= null;
		}

		String outputFile = "../playgrounds/mmoyo/output/precalculation/routed3150/allRoutes.xml";
		String net = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz" ;
		System.out.println("writing output plan file..." + outputFile);
		PopulationWriter popwriter = new PopulationWriter(newPop , loader.readNetwork(net));
		popwriter.write(outputFile) ;
		System.out.println("done");

		File file = new File( basePopfile);
		file.getParent();
		
		String ctrln = "\n";
		try { 
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file.getParent() + "/agents.txt"));
		
			for (Person person : newPop.getPersons().values()){
				bufferedWriter.write(person.getId().toString() + ctrln);
			}

			bufferedWriter.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		} 

	}

}
