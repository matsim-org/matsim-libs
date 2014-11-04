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
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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
import playground.agarwalamit.analysis.activity.ActivityDurationHandler;
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
	private ActivityDurationHandler actHandler;
	private final String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";/*"./output/run2/";*/
	private final String cases [] = {"baseCaseCtd", "ei","ci","eci"};
	private final UserGroup ug = UserGroup.URBAN;
	private SortedMap<String, Double> mode2Legs;
	private SortedMap<String, Map<Id<Person>, Double>> mode2PersonId2TotalTravelStat;

	private SortedMap<String,Map<Id<Person>, List<Double>>> getTravelTimes(String eventsFile){
		timeHandler = new LegModeTravelTimeHandler();
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(timeHandler);
		reader.readFile(eventsFile);
		return  timeHandler.getLegMode2PesonId2TripTimes();
	}

	private SortedMap<String,Map<Id<Person>, List<Double>>> getRouteDistances(String eventsFile, Scenario scenario){
		distHandler = new LegModeRouteDistanceDistributionHandler(scenario);
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(distHandler);
		reader.readFile(eventsFile);
		return distHandler.getMode2PersonId2TravelDistances();
	}

	private Map<Id<Person>, Double> getActivityDuration(String eventsFile){
		actHandler = new ActivityDurationHandler();
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(actHandler);
		reader.readFile(eventsFile);
		return actHandler.getPersonId2TotalActDuration();
	}

	private void run(){

		String outputFile = outputDir+"/analysis/"+ug+"/";
		new File(outputFile).mkdirs();
		
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile+ "/score_verification.txt");
		try {
			writer.write("Scenario \t mode \t numberOfLegs \t totalTravelTime \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
		Set<Id<Person>> stuckPersonsListBAU = new HashSet<Id<Person>>();
		Set<Id<Person>> stuckPersonsListEI = new HashSet<Id<Person>>();
		
		for(String str:cases){
			String eventsFile = outputDir+"/"+str+"/ITERS/it.1500/1500.events.xml.gz";
			String popFile = outputDir+"/"+str+"/output_plans.xml";
			Scenario sc = LoadMyScenarios.loadScenarioFromPlans(popFile);

			PersonFilter pf = new PersonFilter();
			Population revCommuterPop = pf.getPopulation(sc.getPopulation(), ug);
			Set<Id<Person>> revCommuterPersonsList = revCommuterPop.getPersons().keySet();
			
			getPersonId2Data(revCommuterPop, getTravelTimes(eventsFile));
			
			Map<String, Map<Id<Person>, Double>> mode2PersonId2TravelTime = mode2PersonId2TotalTravelStat ;
		
			for(String mode:mode2PersonId2TravelTime.keySet()){
				Map<Id<Person>, Double> personId2TravelTime = mode2PersonId2TravelTime.get(mode);
				double travelTime = getSumOfValuesInMap(personId2TravelTime);
				try {
					writer.write(str+"\t"+mode+"\t"+mode2Legs.get(mode)+"\t"+travelTime+"\n");
				} catch (Exception e) {
					throw new RuntimeException("Data is not written. Reason "+e);
				}
			}

			try {
				writer.write("Scenario \t mode \t numberOfLegs \t totalTravelDist \n");
			} catch (Exception e) {
				throw new RuntimeException("Data is not written. Reason "+e);
			}

			if(str.equals("baseCaseCtd")) stuckPersonsListBAU.addAll(timeHandler.getStuckPersonsList());
			else if(str.equals("ei")) stuckPersonsListEI.addAll(timeHandler.getStuckPersonsList());

			Config config = new Config();
			config.addCoreModules();
			List<String> mainModes = new ArrayList<String>(); mainModes.add("car");
			config.qsim().setMainModes(mainModes); config.network().setInputFile(outputDir+"/"+str+"/output_network.xml.gz");
			sc= ScenarioUtils.loadScenario(config);

			getPersonId2Data(revCommuterPop, getRouteDistances(eventsFile, sc));
			
			Map<String, Map<Id<Person>, Double>> mode2PersonId2TravelDist = mode2PersonId2TotalTravelStat;
			
			for(String mode:mode2PersonId2TravelDist.keySet()){
				Map<Id<Person>, Double> personId2TravelDist = mode2PersonId2TravelDist.get(mode);
				double travelDist = getSumOfValuesInMap(personId2TravelDist);
				try {
					writer.write(str+"\t"+mode+"\t"+mode2Legs.get(mode)+"\t"+travelDist+"\n");
				} catch (Exception e) {
					throw new RuntimeException("Data is not written. Reason "+e);
				}
			}

			Map<Id<Person>, Double> person2ActDurations = getActivityDuration(eventsFile);
			Map<Id<Person>, Double> revCommPersonActDuration = new HashMap<>();
			for(Id<Person> id : person2ActDurations.keySet()){
				if (revCommuterPersonsList.contains(id)) revCommPersonActDuration.put(id, person2ActDurations.get(id));
			}
			double actDuration = getSumOfValuesInMap(revCommPersonActDuration);
			try {
				writer.write("scenario \t totalActivityDurationInSec \n");
				writer.write(str+"\t"+actDuration+"\n");
			} catch (Exception e) {
				throw new RuntimeException("Data is not written. Reason "+e);
			}
		}
		System.out.println("Number of stuck persons in BAU are "+ stuckPersonsListBAU.size());
		System.out.println("Number of stuck persons in EI are "+ stuckPersonsListEI.size());

		for(Id<Person> id : stuckPersonsListBAU){
			if(!stuckPersonsListEI.contains(id)) System.out.println("Stuck person "+id+" is not same in both scenarios.");
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

	private void getPersonId2Data(Population pop, SortedMap<String, Map<Id<Person>, List<Double>>> inputMap){
		mode2PersonId2TotalTravelStat = new TreeMap<String, Map<Id<Person>,Double>>();
		mode2Legs = new TreeMap<String, Double>();
		for(String mode:inputMap.keySet()){
			double noOfLeg =0;
			Map<Id<Person>, Double> personId2TotalTravelStat = new HashMap<Id<Person>, Double>();
			for(Id<Person> id:inputMap.get(mode).keySet()){
				if(pop.getPersons().keySet().contains(id)){
					double travelStat=0;
					for(double d:inputMap.get(mode).get(id)){
						travelStat += d;
						noOfLeg++;
					}
					personId2TotalTravelStat.put(id, travelStat);
				}
			}
			mode2PersonId2TotalTravelStat.put(mode, personId2TotalTravelStat);
			mode2Legs.put(mode, noOfLeg);
		}
	}
}
