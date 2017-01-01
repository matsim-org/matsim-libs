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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.StuckAgentsFilter;
import playground.agarwalamit.analysis.tripDistance.TripDistanceHandler;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class WriteTotalTimeDistanceStats {

	private TripDistanceHandler distHandler;
	private final String outputDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
	private final String cases [] = {"baseCaseCtd", "ei","ci","eci"};
	private final UserGroup ug = UserGroup.REV_COMMUTER;

	private void run(){

		String outputFile = outputDir+"/analysis/"+ug+"/";
		new File(outputFile).mkdirs();

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile+ "/score_verification.txt");
		try {
			writer.write("Scenario \t mode \t numberOfLegs \t totalTravelTime \t totalTravelDist \n");

			Set<Id<Person>> stuckPersonsListBAU = new HashSet<>();
			Set<Id<Person>> stuckPersonsListEI = new HashSet<>();

			for(String str:cases){
				String eventsFile = outputDir+"/"+str+"/ITERS/it.1500/1500.events.xml.gz";
				String popFile = outputDir+"/"+str+"/output_plans.xml";
				String networkFile = outputDir+"/"+str+"/output_network.xml.gz";
				List<String> mainModes = new ArrayList<>(); mainModes.add("car");

				Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(popFile, networkFile);
				sc.getConfig().qsim().setMainModes(mainModes); 

				SortedMap<String,Map<Id<Person>, Double>> mode2Person2time =  getTravelTimes(eventsFile);
				SortedMap<String,Map<Id<Person>, Double>> mode2Person2dist =  getRouteDistances(eventsFile, sc);

				for(String mode:mode2Person2time.keySet()){
					Map<Id<Person>, Double> personId2TravelTime = mode2Person2time.get(mode);
					Map<Id<Person>, Double> personId2TravelDist = mode2Person2dist.get(mode);
					double travelTime = getSumOfValuesInMap(personId2TravelTime);
					double travelDist = getSumOfValuesInMap(personId2TravelDist);
//					timeHandler.getMode2NumberOfLegs().get(mode)
					writer.write(str+"\t"+mode+"\t"+"NA"+"\t"+travelTime+"\t"+travelDist+"\n");
				}
				if(str.equals("baseCaseCtd")) stuckPersonsListBAU = getStuckPersonsList(eventsFile);
				else if(str.equals("ei")) stuckPersonsListEI = getStuckPersonsList(eventsFile);

				for(Id<Person> id : stuckPersonsListBAU){
					if(!stuckPersonsListEI.contains(id)) System.out.println("Stuck person "+id+" is not same in both scenarios.");
				}
			}
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
		PersonFilter pf = new PersonFilter();
		for(Id<Person> personId : inputMap.keySet()){
			if(pf.isPersonIdFromUserGroup(personId, ug)){
				sum += inputMap.get(personId);
			}
		}
		return sum;
	}
	
	private Set<Id<Person>> getStuckPersonsList(String eventsFile) {
		StuckAgentsFilter saf = new StuckAgentsFilter(Arrays.asList(eventsFile));
		saf.preProcessData();
		saf.postProcessData();
		return saf.getStuckPersonsFromEventsFiles();
	}
	
	private SortedMap<String,Map<Id<Person>, Double>> getTravelTimes(String eventsFile){
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(eventsFile);
		mtta.run();
		return  mtta.getMode2PersonId2TotalTravelTime();
	}

	private SortedMap<String,Map<Id<Person>, Double>> getRouteDistances(String eventsFile, Scenario scenario){
		distHandler = new TripDistanceHandler(scenario);
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(distHandler);
		reader.readFile(eventsFile);
		return distHandler.getLegMode2PersonId2TotalTravelDistance();
	}
}
