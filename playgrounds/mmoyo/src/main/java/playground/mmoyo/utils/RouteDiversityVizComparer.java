/* *********************************************************************** *
 * project: org.matsim.*
 * RouteVizComparer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;

import playground.mmoyo.algorithms.PopulationSecFilter;
import playground.mmoyo.io.PopSecReader;

public class RouteDiversityVizComparer {

	public RouteDiversityVizComparer (String configFile){

	}

	public static void main(String[] args) {
		String configFile;
		String strIdPerson;
		int trip;
		
		if (args.length>0){ 
			configFile =  args[0];
			strIdPerson = args[1];
			trip=Integer.valueOf (args[2]);
		}else{
			configFile =  "../../ptManuel/calibration/my_config.xml";
			strIdPerson = "b1000185";
			trip= 1;   //there is not trip 0, trip 1 is the first 
		}
		
		// look for the desired person in the array of populations
		DataLoader dataLoader = new DataLoader();
		 
		Id<Person> idPerson = Id.create(strIdPerson, Person.class);
		PopulationSecFilter populationSecFilter = new PopulationSecFilter();
		populationSecFilter.getStrIdList().add(idPerson);
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		PopSecReader popSecReader = new PopSecReader (scn, populationSecFilter);
		Config config= dataLoader.readConfig(configFile);
		popSecReader.readFile(config.plans().getInputFile());
		Population pop = populationSecFilter.getNewPop();
		
		scn= null;
		popSecReader = null;
		populationSecFilter= null;
		idPerson = null;
		
		PlanFragmenter planFragmenter = new PlanFragmenter();
		pop=planFragmenter.plans2Persons(pop); //each plan is converted into a person
		
		scn= (ScenarioImpl) dataLoader.createScenario();
        Population newPop = PopulationUtils.createPopulation(scn.getConfig(), scn.getNetwork());
		for (Person person: pop.getPersons().values()){
			List<Person> perList= planFragmenter.run(person); //each plan is splitted in trips that are converted to "persons"
			//System.out.println(person + " " + perList.size());
			if(perList.size()>0 && perList.size()>=trip){   // this is to discard stay home plan
				newPop.addPerson(perList.get(trip-1));   
			}
		}
		planFragmenter= null;
		scn= null;

		//////////////////////////////////
		//convert each plan into new persons
		Network net = dataLoader.readNetwork(config.network().getInputFile());
		PopulationWriter popwriter = new PopulationWriter(newPop, net);
		String fragmentedPopPath = config.controler().getOutputDirectory() + "planFragmented.xml";
		popwriter.write(fragmentedPopPath);
		
		//prepare new config for otfviz
		config.setParam("plans", "inputPlansFile", fragmentedPopPath);
		String outConfig = config.controler().getOutputDirectory() + "/config_fragmentedAgent.xml";
		ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write(outConfig);
		
		//set all objects to null, the OTFVis may need that memory space
		Set<Id<Person>> personsIds = newPop.getPersons().keySet();
		pop= null;
		dataLoader = null;
		net= null;
		popwriter= null;
		config = null;
		configWriter= null;
		newPop = null;
		
		OTFVis.playConfig(outConfig);
		
		for (Id id : personsIds){
			System.out.println(id);
		}
	}

}
