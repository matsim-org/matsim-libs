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
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.legModeHandler.LegModeTravelTimeHandler;

/**
 * @author amit
 */

public class ModeSwitchersTravelTime {
	private Logger log = Logger.getLogger(ModeSwitchersTravelTime.class);

	public static void main(String[] args) {

		String dir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String runCases[] ={"baseCaseCtd","ei","ci","eci"};

		for(String runCase : runCases){
			new ModeSwitchersTravelTime().run(dir+runCase);
		}
		

	}
	

	private void run (String runCase){

		Tuple<Integer, Double> car2NonCar_legs_travelTime_first = new Tuple<Integer, Double>(0, 0.0);
		Tuple<Integer, Double> car2NonCar_legs_travelTime_last = new Tuple<Integer, Double>(0, 0.0);


		Tuple<Integer, Double> nonCar2Car_legs_travelTime_first = new Tuple<Integer, Double>(0, 0.0);
		Tuple<Integer, Double> nonCar2Car_legs_travelTime_last = new Tuple<Integer, Double>(0, 0.0);


		String eventsFile_it_first = runCase+"/ITERS/it.1000/1000.events.xml.gz";
		String eventsFile_it_last = runCase+"/ITERS/it.1500/1500.events.xml.gz";

		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelTimes_it_first = getPerson2mode2TripTimes(eventsFile_it_first);
		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelTimes_it_last = getPerson2mode2TripTimes(eventsFile_it_last);


		for(Id<Person> pId : person2ModeTravelTimes_it_first.keySet()){

			if(person2ModeTravelTimes_it_last.containsKey(pId) && person2ModeTravelTimes_it_last.get(pId).size()==person2ModeTravelTimes_it_first.get(pId).size()) {

				for(int ii=0; ii<person2ModeTravelTimes_it_first.get(pId).size();ii++){
					Tuple<String, Double> first_mode = person2ModeTravelTimes_it_first.get(pId).get(ii);
					Tuple<String, Double> last_mode = person2ModeTravelTimes_it_last.get(pId).get(ii);

					if(first_mode.getFirst().equals("car") && !last_mode.getFirst().equals("car")){ //car 2 non-car
						car2NonCar_legs_travelTime_first = new Tuple<Integer, Double>(car2NonCar_legs_travelTime_first.getFirst()+1, car2NonCar_legs_travelTime_first.getSecond()+first_mode.getSecond());
						car2NonCar_legs_travelTime_last = new Tuple<Integer, Double>(car2NonCar_legs_travelTime_last.getFirst()+1, car2NonCar_legs_travelTime_last.getSecond()+last_mode.getSecond());
					} else if (!first_mode.getFirst().equals("car") && last_mode.getFirst().equals("car")) { //non-car 2 car
						nonCar2Car_legs_travelTime_first = new Tuple<Integer, Double>(nonCar2Car_legs_travelTime_first.getFirst()+1, nonCar2Car_legs_travelTime_first.getSecond()+first_mode.getSecond());
						nonCar2Car_legs_travelTime_last = new Tuple<Integer, Double>(nonCar2Car_legs_travelTime_last.getFirst()+1, nonCar2Car_legs_travelTime_last.getSecond()+last_mode.getSecond());
					}
				} 

			} else if(!person2ModeTravelTimes_it_last.containsKey(pId)) {

				log.warn("Person "+pId+ "is not present in the last iteration map. This person is thus not included in the results. Probably due to stuck and abort event.");

			} else { // number of trips are different for particular person

				log.warn("Person "+pId+" do not have the same number of trip legs in the two maps. This could be due to stuck and abort event. Thus including only minimum legs for that person.");
				int minLegs = Math.min(person2ModeTravelTimes_it_last.get(pId).size(),person2ModeTravelTimes_it_first.get(pId).size());


				for (int jj=0; jj<minLegs; jj++){

					Tuple<String, Double> first_mode = person2ModeTravelTimes_it_first.get(pId).get(jj);
					Tuple<String, Double> last_mode = person2ModeTravelTimes_it_last.get(pId).get(jj);

					if(first_mode.getFirst().equals("car") && !last_mode.getFirst().equals("car")){ //car 2 non-car
						car2NonCar_legs_travelTime_first = new Tuple<Integer, Double>(car2NonCar_legs_travelTime_first.getFirst()+1, car2NonCar_legs_travelTime_first.getSecond()+first_mode.getSecond());
						car2NonCar_legs_travelTime_last = new Tuple<Integer, Double>(car2NonCar_legs_travelTime_last.getFirst()+1, car2NonCar_legs_travelTime_last.getSecond()+last_mode.getSecond());
					} else if (!first_mode.getFirst().equals("car") && last_mode.getFirst().equals("car")) { //non-car 2 car
						nonCar2Car_legs_travelTime_first = new Tuple<Integer, Double>(nonCar2Car_legs_travelTime_first.getFirst()+1, nonCar2Car_legs_travelTime_first.getSecond()+first_mode.getSecond());
						nonCar2Car_legs_travelTime_last = new Tuple<Integer, Double>(nonCar2Car_legs_travelTime_last.getFirst()+1, nonCar2Car_legs_travelTime_last.getSecond()+last_mode.getSecond());
					}
				}
			}
			
			BufferedWriter writer =  IOUtils.getBufferedWriter(runCase+"/analysis/modeSwitchersTravelTime.txt");
			try {
				writer.write("initialOrFinal \t numberOfCar2NonCarSwitchLegs \t totalTravelTimeForSuchTripsInHr \n");
				writer.write("initial \t "+car2NonCar_legs_travelTime_first.getFirst()+"\t"+car2NonCar_legs_travelTime_first.getSecond()/3600+"\n");
				writer.write("final \t "+car2NonCar_legs_travelTime_last.getFirst()+"\t"+car2NonCar_legs_travelTime_last.getSecond()/3600+"\n");
				
				writer.newLine();
				
				writer.write("initialOrFinal \t numberOfnonCar2CarSwitchLegs \t totalTravelTimeForSuchTripsInHr \n");
				writer.write("initial \t "+nonCar2Car_legs_travelTime_first.getFirst()+"\t"+nonCar2Car_legs_travelTime_first.getSecond()/3600+"\n");
				writer.write("final \t "+nonCar2Car_legs_travelTime_last.getFirst()+"\t"+nonCar2Car_legs_travelTime_last.getSecond()/3600+"\n");
				
				writer.close();
			} catch (Exception e) {
				throw new RuntimeException(
						"Data is not written in file. Reason: " + e);
			}
		}
	}


	private Map<Id<Person>, List<Tuple<String, Double>>> getPerson2mode2TripTimes(String eventsFile){

		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		LegModeTravelTimeHandler timeHandler = new LegModeTravelTimeHandler();
		events.addHandler(timeHandler);

		reader.readFile(eventsFile);

		SortedMap<String,Map<Id<Person>,List<Double>>> mode2Person2TripTimes = timeHandler.getLegMode2PesonId2TripTimes();

		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelTimes = new HashMap<>();


		for(String mode : mode2Person2TripTimes.keySet()){
			for (Id<Person> p : mode2Person2TripTimes.get(mode).keySet()){
				for(Double d :mode2Person2TripTimes.get(mode).get(p)){
					Tuple<String, Double> mode2TripTime = new Tuple<String, Double>(mode, d);

					if (person2ModeTravelTimes.containsKey(p)){
						List<Tuple<String, Double>> mode2TripTimeList  = person2ModeTravelTimes.get(p);
						mode2TripTimeList.add(mode2TripTime);
					} else {
						List<Tuple<String, Double>> mode2TripTimeList = new ArrayList<Tuple<String,Double>>();
						mode2TripTimeList.add(mode2TripTime);
						person2ModeTravelTimes.put(p, mode2TripTimeList);
					}

				}
			}
		}
		return person2ModeTravelTimes;
	}


}
