/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.legModeHandler.LegModeRouteDistanceDistributionHandler;
import playground.agarwalamit.analysis.legModeHandler.LegModeTravelTimeHandler;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class WriteTotalTimeDistanceStats {

	private LegModeTravelTimeHandler timeHandler;
	private LegModeRouteDistanceDistributionHandler distHandler;
	private final String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";/*"./output/run2/";*/
	private final String cases [] = {"baseCaseCtd", "ei"};
	private final UserGroup ug = UserGroup.REV_COMMUTER;

	private Map<String,Map<Id<Person>, Double>> getTravelTimes(String eventsFile){
		timeHandler = new LegModeTravelTimeHandler();
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(timeHandler);
		reader.readFile(eventsFile);
		return  timeHandler.getLegMode2PersonId2TotalTravelTime();
	}

	private  Map<String,Map<Id<Person>, Double>> getRouteDistances(String eventsFile, Scenario scenario){
		distHandler = new LegModeRouteDistanceDistributionHandler(scenario);
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(distHandler);
		reader.readFile(eventsFile);
		return distHandler.getLegMode2PersonId2TotalTravelDistance();
	}

	private void run(){

		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/verifyScoreCalculation.txt");
		try {
			writer.write("Scenario \t mode \t numberOfLegs \t totalTravelTime \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}

		for(String str:cases){
			String eventsFile = outputDir+"/"+str+"/ITERS/it.1500/1500.events.xml.gz";
			String popFile = outputDir+"/"+str+"/output_plans.xml";
			Scenario sc = LoadMyScenarios.loadScenarioFromPlans(popFile);

			PersonFilter pf = new PersonFilter();
			Population revCommuterPop = pf.getPopulation(sc.getPopulation(), ug);
			Set<Id<Person>> revCommuterPersonsList = revCommuterPop.getPersons().keySet();

			Map<String, Map<Id<Person>, Double>> mode2PersonId2TravelTime = getTravelTimes(eventsFile);
			for(String mode:mode2PersonId2TravelTime.keySet()){
				Map<Id<Person>, Double> personId2TravelTime = mode2PersonId2TravelTime.get(mode);
				Map<Id<Person>, Double> revCommPersonTravelTime = new HashMap<>();
				for(Id<Person> id : personId2TravelTime.keySet()){
					if (revCommuterPersonsList.contains(id)) revCommPersonTravelTime.put(id, personId2TravelTime.get(id));
				}
				double travelTime = getSumOfValuesInMap(revCommPersonTravelTime);
				try {
					writer.write(str+"\t"+mode+"\t"+timeHandler.getTravelMode2NumberOfLegs().get(mode)+"\t"+travelTime+"\n");
				} catch (Exception e) {
					throw new RuntimeException("Data is not written. Reason "+e);
				}
			}

			try {
				writer.write("Scenario \t mode \t totalTravelDist \n");
			} catch (Exception e) {
				throw new RuntimeException("Data is not written. Reason "+e);
			}

			Config config = new Config();
			config.addCoreModules();
			List<String> mainModes = new ArrayList<String>(); mainModes.add("car");
			config.qsim().setMainModes(mainModes); config.network().setInputFile(outputDir+"/"+str+"/output_network.xml.gz");
			sc= ScenarioUtils.loadScenario(config);

			Map<String, Map<Id<Person>, Double>> mode2PersonId2TravelDist = getRouteDistances(eventsFile, sc);
			for(String mode:mode2PersonId2TravelDist.keySet()){
				Map<Id<Person>, Double> personId2TravelDist = mode2PersonId2TravelDist.get(mode);
				Map<Id<Person>, Double> revCommPersonTravelDist = new HashMap<>();
				for(Id<Person> id : personId2TravelDist.keySet()){
					if (revCommuterPersonsList.contains(id)) revCommPersonTravelDist.put(id, personId2TravelDist.get(id));
				}
				double travelDist = getSumOfValuesInMap(revCommPersonTravelDist);
				try {
					writer.write(str+"\t"+mode+"\t"+distHandler.getTravelMode2NumberOfLegs().get(mode)+"\t"+travelDist+"\n");
				} catch (Exception e) {
					throw new RuntimeException("Data is not written. Reason "+e);
				}
			}
		}
		try{
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
	}
	public static void main(String[] args) {
		new WriteTotalTimeDistanceStats().run();
	}

	private double getSumOfValuesInMap(Map<Id<Person>, Double> inputMap) {
		double sum =0;
		for(Id<Person> personId : inputMap.keySet()){
			sum += inputMap.get(personId);
		}
		return sum;
	}
}
