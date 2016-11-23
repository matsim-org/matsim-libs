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
package playground.agarwalamit.analysis.modeSwitcherRetainer;

import java.io.BufferedWriter;
import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.utils.FileUtils;

/**
 *This will first find mode switchers and then returns trip times in groups. 
 *<p>
 * @author amit
 */

public class ModeSwitchersTripTime {

	private static final Logger LOG = Logger.getLogger(ModeSwitchersTripTime.class);

	private final Comparator<Tuple<String, String>> comparator = new Comparator<Tuple<String, String>>() {
		@Override
		public int compare(Tuple<String, String> o1, Tuple<String, String> o2) {
			return o1.toString().compareTo(o2.toString());
		}
	};

	private final SortedMap<Tuple<String, String>, List<Id<Person>>> modeSwitchType2PersonIds = new TreeMap<>(comparator);
	private final SortedMap<Tuple<String, String>, Integer> modeSwitchType2numberOfLegs = new TreeMap<>(comparator);
	private final SortedMap<Tuple<String, String>, Tuple<Double, Double>> modeSwitchType2TripTimes = new TreeMap<>(comparator);

	public static void main(String[] args) {

		String dir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run9/";
		String runCases[] ={"baseCaseCtd","ei","ci","eci"};

		for(String runNr : runCases){
			ModeSwitchersTripTime mstt = new ModeSwitchersTripTime();
			mstt.run(dir+runNr);
			mstt.writeModeSwitcherTripTimes(dir+runNr);
		}
	}

	private void writeModeSwitcherTripTimes(final String runCase){
		String outFile = runCase+"/analysis/modeSwitchersTripTimes.txt";
		BufferedWriter writer =  IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("firstMode \t lastMode \t numberOfLegs \t totalTripTimesForFirstIterationInHr \t totalTripTimesForLastIterationInHr \n");

			for(Tuple<String, String> str: modeSwitchType2numberOfLegs.keySet()){
				writer.write(str.getFirst()+"\t"+str.getSecond()+"\t"+ modeSwitchType2numberOfLegs.get(str)+"\t"
						+ modeSwitchType2TripTimes.get(str).getFirst()/3600.+"\t"
						+ modeSwitchType2TripTimes.get(str).getSecond()/3600.+"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Data is not written in file. Reason: " + e);
		}
		LOG.info("Data is written to "+outFile);
	}


	public void run (String runCase){

		// data from event files
		String eventsFileFirstIt = runCase+"/ITERS/it.1000/1000.events.xml.gz";
		String eventsFileLastIt = runCase+"/ITERS/it.1500/1500.events.xml.gz";

		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelTimesFirstIt = getPerson2mode2TripTimes(eventsFileFirstIt);
		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelTimesLastIt = getPerson2mode2TripTimes(eventsFileLastIt);

		// start processing
		for(Id<Person> pId : person2ModeTravelTimesFirstIt.keySet()){

			if(person2ModeTravelTimesLastIt.containsKey(pId) ) {

				int numberOfLegs = 0; 
				if(person2ModeTravelTimesLastIt.get(pId).size() != person2ModeTravelTimesFirstIt.get(pId).size()) {
					//	if person does not have same number of trips as in first iteration

					LOG.warn("Person "+pId+" do not have the same number of trip legs in the two maps. This could be due to stuck and abort event. "
							+ "\n Thus including only minimum legs (removing the common trups) for that person.");
					numberOfLegs  = Math.min(person2ModeTravelTimesLastIt.get(pId).size(),person2ModeTravelTimesFirstIt.get(pId).size());

				} else numberOfLegs = person2ModeTravelTimesLastIt.get(pId).size();

				for(int ii=0; ii<numberOfLegs;ii++){

					Tuple<String, Double> firstItMode = person2ModeTravelTimesFirstIt.get(pId).get(ii);
					Tuple<String, Double> lastItMode = person2ModeTravelTimesLastIt.get(pId).get(ii);

					String firstMode = firstItMode.getFirst();
					String lastMode = lastItMode.getFirst();

					Tuple<String, String> modeSwitchType = new Tuple<>(firstMode, lastMode);
					storeTripTimeInfo(pId, modeSwitchType, new Tuple<>(firstItMode.getSecond(), lastItMode.getSecond()));
				} 
			} else if(!person2ModeTravelTimesLastIt.containsKey(pId)) {
				LOG.warn("Person "+pId+ "is not present in the last iteration map. This person is thus not included in the results. Probably due to stuck and abort event.");
			} 
		}
	}

	public void storeTripTimeInfo(final Id<Person> personId, final Tuple<String, String> modeSwitchTyp, final Tuple<Double, Double> travelTimes){

		if(! this.modeSwitchType2numberOfLegs.containsKey(modeSwitchTyp)) { // initialize all
			this.modeSwitchType2numberOfLegs.put(modeSwitchTyp, 0);
			this.modeSwitchType2PersonIds.put(modeSwitchTyp, new ArrayList<>());
			this.modeSwitchType2TripTimes.put(modeSwitchTyp, new Tuple<>(0., 0.));
		}

		this.modeSwitchType2numberOfLegs.put(modeSwitchTyp, this.modeSwitchType2numberOfLegs.get(modeSwitchTyp) + 1);

		List<Id<Person>> swicherIds = this.modeSwitchType2PersonIds.get(modeSwitchTyp);
		swicherIds.add(personId);
		this.modeSwitchType2PersonIds.put(modeSwitchTyp, swicherIds);

		Tuple<Double, Double> nowFirstLastItsTripTimes = new Tuple<>(this.modeSwitchType2TripTimes.get(modeSwitchTyp).getFirst() + travelTimes.getFirst(),
				this.modeSwitchType2TripTimes.get(modeSwitchTyp).getSecond() + travelTimes.getSecond());
		this.modeSwitchType2TripTimes.put(modeSwitchTyp, nowFirstLastItsTripTimes);
	}

	private Map<Id<Person>, List<Tuple<String, Double>>> getPerson2mode2TripTimes(final String eventsFile){

		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(eventsFile);
		mtta.run();

		SortedMap<String,Map<Id<Person>,List<Double>>> mode2Person2TripTimes = mtta.getMode2PesonId2TripTimes();

		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelTimes = new HashMap<>();


		for(String mode : mode2Person2TripTimes.keySet()){
			for (Id<Person> p : mode2Person2TripTimes.get(mode).keySet()){
				for(Double d :mode2Person2TripTimes.get(mode).get(p)){
					Tuple<String, Double> mode2TripTime = new Tuple<>(mode, d);

					if (person2ModeTravelTimes.containsKey(p)){
						List<Tuple<String, Double>> mode2TripTimeList  = person2ModeTravelTimes.get(p);
						mode2TripTimeList.add(mode2TripTime);
					} else {
						List<Tuple<String, Double>> mode2TripTimeList = new ArrayList<>();
						mode2TripTimeList.add(mode2TripTime);
						person2ModeTravelTimes.put(p, mode2TripTimeList);
					}

				}
			}
		}
		return person2ModeTravelTimes;
	}
}
