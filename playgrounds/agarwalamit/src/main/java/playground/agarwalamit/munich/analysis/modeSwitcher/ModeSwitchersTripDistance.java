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
package playground.agarwalamit.munich.analysis.modeSwitcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;

import playground.agarwalamit.analysis.legMode.distributions.LegModeRouteDistanceDistributionHandler;
import playground.agarwalamit.utils.LoadMyScenarios;


/**
 * This will first find mode switchers and then returns trip distances in groups. 
 *<p>
 * car2car, car2non-car, non-car2car and non-car2non-car
 * @author amit
 */

public class ModeSwitchersTripDistance {

	private Logger log = Logger.getLogger(ModeSwitchersTripDistance.class);

	private ModeSwitcherInfoCollector modeSwitchInfo;

	public static void main(String[] args) {

		String dir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String runCases[] ={"baseCaseCtd","ei","ci","eci","ei_10"};

		for(String runCase : runCases){
			ModeSwitchersTripDistance mstd = new ModeSwitchersTripDistance();
			mstd.run(dir+runCase);
			mstd.modeSwitchInfo.writeModeSwitcherTripDistances(dir+runCase);
		}
	}

	public ModeSwitchersTripDistance (){
		modeSwitchInfo = new ModeSwitcherInfoCollector();
	}

	private void run (String runCase){

		// data from event files
		String eventsFile_it_first = runCase+"/ITERS/it.1000/1000.events.xml.gz";
		String eventsFile_it_last = runCase+"/ITERS/it.1500/1500.events.xml.gz";
		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(runCase+"/output_network.xml.gz", runCase+"/output_config.xml");

		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelDists_it_first = getPerson2mode2TripDistances(eventsFile_it_first,sc);
		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelDists_it_last = getPerson2mode2TripDistances(eventsFile_it_last,sc);

		for(Id<Person> pId : person2ModeTravelDists_it_first.keySet()){

			if(person2ModeTravelDists_it_last.containsKey(pId) ) {

				int numberOfLegs = 0;
				if(person2ModeTravelDists_it_last.get(pId).size() != person2ModeTravelDists_it_first.get(pId).size()){
					//	if person does not have same number of trips as in first iteration

					this.log.warn("Person "+pId+" do not have the same number of trip legs in the two maps. This could be due to stuck and abort event. "
							+ "\n Thus including only minimum legs (removing the common trups) for that person.");
					numberOfLegs = Math.min(person2ModeTravelDists_it_last.get(pId).size(),person2ModeTravelDists_it_first.get(pId).size());
				} else numberOfLegs = person2ModeTravelDists_it_first.get(pId).size();

				for(int ii=0; ii<numberOfLegs;ii++){
					Tuple<String, Double> first_it_mode = person2ModeTravelDists_it_first.get(pId).get(ii);
					Tuple<String, Double> last_it_mode = person2ModeTravelDists_it_last.get(pId).get(ii);

					String firstMode = getTravelMode(first_it_mode.getFirst());
					String lastMode = getTravelMode(last_it_mode.getFirst());

					String switchTyp = firstMode.concat("2").concat(lastMode);
					ModeSwitcherType modeSwitchType = ModeSwitcherType.valueOf(switchTyp);
					this.modeSwitchInfo.storeTripDistanceInfo(pId, modeSwitchType, new Tuple<Double, Double>(first_it_mode.getSecond(), last_it_mode.getSecond()));
				} 

			} else if(!person2ModeTravelDists_it_last.containsKey(pId)) {

				log.warn("Person "+pId+ "is not present in the last iteration map. This person is thus not included in the results. Probably due to stuck and abort event.");

			} 
		}
	}


	private String getTravelMode(String mode){
		if(mode.equals(TransportMode.car)) return "car";
		else return "nonCar";
	}

	private Map<Id<Person>, List<Tuple<String, Double>>> getPerson2mode2TripDistances(String eventsFile, Scenario sc){

		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		LegModeRouteDistanceDistributionHandler distHandler = new LegModeRouteDistanceDistributionHandler(sc);
		events.addHandler(distHandler);

		reader.readFile(eventsFile);

		SortedMap<String,Map<Id<Person>,List<Double>>> mode2Person2TripDists = distHandler.getMode2PersonId2TravelDistances();

		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelDists = new HashMap<>();


		for(String mode : mode2Person2TripDists.keySet()){
			for (Id<Person> p : mode2Person2TripDists.get(mode).keySet()){
				for(Double d :mode2Person2TripDists.get(mode).get(p)){
					Tuple<String, Double> mode2TripDist = new Tuple<String, Double>(mode, d);

					if (person2ModeTravelDists.containsKey(p)){
						List<Tuple<String, Double>> mode2TripDistList  = person2ModeTravelDists.get(p);
						mode2TripDistList.add(mode2TripDist);
					} else {
						List<Tuple<String, Double>> mode2TripDistList = new ArrayList<Tuple<String,Double>>();
						mode2TripDistList.add(mode2TripDist);
						person2ModeTravelDists.put(p, mode2TripDistList);
					}

				}
			}
		}
		return person2ModeTravelDists;
	}
}
