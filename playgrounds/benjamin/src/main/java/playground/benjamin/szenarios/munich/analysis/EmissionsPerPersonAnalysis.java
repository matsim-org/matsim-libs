/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisEmissionsPerArea.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.szenarios.munich.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.benjamin.events.EmissionEventsReader;

/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonAnalysis {
	private static final Logger logger = Logger.getLogger(EmissionsPerPersonAnalysis.class);

	private final String runNumber = "972";
	private final String runDirectory = "../../runs-svn/run" + runNumber + "/";
//	private final String netFile = runDirectory + runNumber + ".output_network.xml.gz";
	private final String netFile = runDirectory + "output_network.xml.gz";
//	private final String plansFile = runDirectory + runNumber + ".output_plans.xml.gz";
	private final String plansFile = runDirectory + "output_plans.xml.gz";
	private final String emissionFile = runDirectory + "emission.events.xml.gz";
	
	private Scenario scenario;
	private EmissionsPerPersonHotEventHandler hotHandler;
	private EmissionsPerPersonColdEventHandler coldHandler;
	private Map<Id, Map<String, Double>> hotEmissions;
	private Map<Id, Map<String, Double>> coldEmissions;
	private Map<Id, Map<String, Double>> totalEmissions;
	private SortedSet<String> listOfPollutants;


	private void run() {
		loadScenario();
		processEmissions();
		hotEmissions = hotHandler.getHotEmissionsPerPerson();
		coldEmissions = coldHandler.getColdEmissionsPerPerson();
		fillListOfPollutants(hotEmissions, coldEmissions);
		setNonCalculatedEmissions(scenario.getPopulation(), hotEmissions);
		setNonCalculatedEmissions(scenario.getPopulation(), coldEmissions);
		totalEmissions = sumUpEmissions(hotEmissions, coldEmissions);
		printEmissions(scenario.getPopulation(), hotEmissions, "EmissionsPerHomeLocationWarm.txt");
		printEmissions(scenario.getPopulation(), coldEmissions, "EmissionsPerHomeLocationCold.txt");
		printEmissions(scenario.getPopulation(), totalEmissions, "EmissionsPerHomeLocationTotal.txt");
	}

	private void printEmissions(Population population, Map<Id, Map<String, Double>> emissions, String filename) {
		String outFile = runDirectory + filename;
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.append("personId \t xHome \t yHome \t");
			for (String pollutant : listOfPollutants){
				out.append(pollutant + "[g] \t");
			}
			out.append("\n");

			for(Person person: population.getPersons().values()){
				Id personId = person.getId();
				Plan plan = person.getSelectedPlan();
				Activity homeAct = (Activity) plan.getPlanElements().get(0);
				Coord homeCoord = homeAct.getCoord();
				Double xHome = homeCoord.getX();
				Double yHome = homeCoord.getY();

				out.append(personId + "\t" + xHome + "\t" + yHome + "\t");

				// TODO: make this in line with listOfPollutants, ordering right?
				Map<String, Double> emissionType2Value = emissions.get(personId);
				for(String pollutant : listOfPollutants){
					Double value = emissionType2Value.get(pollutant);
					out.append(value + "\t");
				}
				out.append("\n");
			}
			//Close the output stream
			out.close();
			logger.info("Finished writing output to " + outFile);
		}
		catch (Exception e){
			logger.warn("Error: " + e.getMessage());
		}
	}

	private Map<Id, Map<String, Double>> sumUpEmissions(Map<Id, Map<String, Double>> hotEmissions, Map<Id, Map<String, Double>> coldEmissions) {
		Map<Id, Map<String, Double>> totalEmissions = new HashMap<Id, Map<String, Double>>();
		for(Entry<Id, Map<String, Double>> entry : hotEmissions.entrySet()){
			Id personId = entry.getKey();
			Map<String, Double> individualHotEmissions = entry.getValue();

			if(coldEmissions.containsKey(personId)){
				Map<String, Double> individualSumOfEmissions = new HashMap<String, Double>();
				Map<String, Double> individualColdEmissions = coldEmissions.get(personId);
				for(String pollutant : individualHotEmissions.keySet()){
					Double individualValue = individualHotEmissions.get(pollutant) + individualColdEmissions.get(pollutant);
					individualSumOfEmissions.put(pollutant, individualValue);
				}
				totalEmissions.put(personId, individualSumOfEmissions);
			}
			else{
				totalEmissions.put(personId, individualHotEmissions);
			}
		}
		return totalEmissions;
	}

	private void setNonCalculatedEmissions(Population population, Map<Id, Map<String, Double>> emissionsPerPerson) {
		for(Person person : population.getPersons().values()){
			Id personId = person.getId();
			if(!emissionsPerPerson.containsKey(personId)){
				Map<String, Double> emissionType2Value = new HashMap<String, Double>();
				for(String pollutant : listOfPollutants){
					// setting emissions that are were not calculated to 0.0 
					emissionType2Value.put(pollutant, 0.0);
				}
				emissionsPerPerson.put(personId, emissionType2Value);
			}
			else{
				// do nothing
			}
		}
	}

	private void fillListOfPollutants(Map<Id, Map<String, Double>> hotEmissions, Map<Id, Map<String, Double>> coldEmissions) {
		listOfPollutants = new TreeSet<String>();
		for(Map<String, Double> emissionType2Value : hotEmissions.values()){
			for(String pollutant : emissionType2Value.keySet()){
				if(!listOfPollutants.contains(pollutant)){
					listOfPollutants.add(pollutant);
				}
			}
		}
		for(Map<String, Double> emissionType2Value : coldEmissions.values()){
			for(String pollutant : emissionType2Value.keySet()){
				if(!listOfPollutants.contains(pollutant)){
					listOfPollutants.add(pollutant);
				}
			}
		}
		logger.info("The following pollutants are considered: " + listOfPollutants);
	}

	private void processEmissions() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		hotHandler = new EmissionsPerPersonHotEventHandler();
		coldHandler = new EmissionsPerPersonColdEventHandler();
		eventsManager.addHandler(hotHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(emissionFile);
	}

	private void loadScenario() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
	}

	public static void main(String[] args) {
		EmissionsPerPersonAnalysis emissionsPerPerson = new EmissionsPerPersonAnalysis();
		emissionsPerPerson.run();
	}
}
