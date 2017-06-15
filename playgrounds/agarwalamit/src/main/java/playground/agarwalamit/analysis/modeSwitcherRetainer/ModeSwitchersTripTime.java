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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.PersonFilter;

/**
 *This will first find mode switchers and then returns trip times in groups. 
 *<p>
 * @author amit
 */

public class ModeSwitchersTripTime {

	private static final Logger LOG = Logger.getLogger(ModeSwitchersTripTime.class);

	public ModeSwitchersTripTime(){
		this(null, null);
	}

	private final String userGroup;
	private final PersonFilter pf;

	public ModeSwitchersTripTime (final String userGroup, final PersonFilter personFilter) {
		this.pf = personFilter;
		this.userGroup = userGroup;

		if( (userGroup==null && personFilter!=null) || (userGroup!=null && personFilter==null) ) {
			throw new RuntimeException("Either of user group or person filter is null.");
		} else if(userGroup!=null && personFilter!=null) {
			LOG.info("Usergroup filtering is used, result will include persons from given user group only.");
		}
	}

	private final SortedMap<Tuple<String, String>, ModeSwitcherInfoCollector> modeSwitchType2InfoCollector = new TreeMap<>((o1, o2) -> o1.toString().compareTo(o2.toString()));
	private final Map<Id<Person>,List<Tuple<String, String>>> personId2ModeInfos = new HashMap<>();

	public static void main(String[] args) {

		String dir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run9/";
		String runCases[] ={"baseCaseCtd","ei","ci","eci"};

		for(String runNr : runCases){
			ModeSwitchersTripTime mstt = new ModeSwitchersTripTime();
			Scenario scenario = LoadMyScenarios.loadScenarioFromNetworkAndConfig(dir+"/output_network.xml.gz", dir+"output_config.xml.gz");
			scenario.getConfig().controler().setOutputDirectory(dir);
			mstt.processEventsFiles(scenario);
			mstt.writeResults(dir+runNr+"/analysis/");
		}
	}

	public void processEventsFile (final String firstIterationFile, final String lastIterationFile){
		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelTimesFirstIt = getPerson2mode2TripTimes(firstIterationFile);
		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelTimesLastIt = getPerson2mode2TripTimes(lastIterationFile);

		// start processing
		for(Id<Person> pId : person2ModeTravelTimesFirstIt.keySet()){

			if(this.userGroup !=null  && ! this.pf.getUserGroupAsStringFromPersonId(pId).equals(this.userGroup)) {
				continue; // if using person filtering and person does not belong to desired user group, dont include it in the analysis
			}

			if(person2ModeTravelTimesLastIt.containsKey(pId) ) {
				int numberOfLegs = 0;
				if(person2ModeTravelTimesLastIt.get(pId).size() != person2ModeTravelTimesFirstIt.get(pId).size()) {
					//	if person does not have same number of trips as in first iteration
					LOG.warn("Person "+pId+" do not have the same number of trip legs in the two maps. This could be due to stuck and abort event. "
							+ "\n Thus including only minimum number of legs (using the common trips) for that person.");
					numberOfLegs  = Math.min(person2ModeTravelTimesLastIt.get(pId).size(),person2ModeTravelTimesFirstIt.get(pId).size());

				} else numberOfLegs = person2ModeTravelTimesLastIt.get(pId).size();

				for(int ii=0; ii<numberOfLegs;ii++){

					Tuple<String, Double> firstItMode = person2ModeTravelTimesFirstIt.get(pId).get(ii);
					Tuple<String, Double> lastItMode = person2ModeTravelTimesLastIt.get(pId).get(ii);

					Tuple<String, String> modeSwitchType = new Tuple<>(firstItMode.getFirst(), lastItMode.getFirst());
					storeTripTimeInfo(pId, modeSwitchType, new Tuple<>(firstItMode.getSecond(), lastItMode.getSecond()));

				}
			} else if(!person2ModeTravelTimesLastIt.containsKey(pId)) {
				LOG.warn("Person "+pId+ "is not present in the last iteration map. This person is thus not included in the results. Probably due to stuck and abort event.");
			}
		}
	}

	public void processEventsFiles (final Scenario scenario){
		String eventsDir = scenario.getConfig().controler().getOutputDirectory();
		int firstIteration = scenario.getConfig().controler().getFirstIteration();
		int lastIteration = scenario.getConfig().controler().getLastIteration();
		// data from event files
		String eventsFileFirstIt = eventsDir+"/ITERS/it."+firstIteration+"/"+firstIteration+".events.xml.gz";
		String eventsFileLastIt = eventsDir+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";

		processEventsFile(eventsFileFirstIt, eventsFileLastIt);

	}

	private void storePerson2ModeSwitchinfo(final Id<Person> personId, final Tuple<String, String> modeSwitchTyp) {
		List<Tuple<String, String>> modeInfo = this.personId2ModeInfos.get(personId);
		if (modeInfo == null) {
			modeInfo = new ArrayList<>();
			modeInfo.add(modeSwitchTyp);
		} else {
			modeInfo.add(modeSwitchTyp);
		}
		this.personId2ModeInfos.put(personId, modeInfo);
	}

	public Map<Id<Person>, List<Tuple<String, String>>> getPersonId2ModeSwitcherRetainerTripInfo(){
		return this.personId2ModeInfos;
	}

	private void storeTripTimeInfo(final Id<Person> personId, final Tuple<String, String> modeSwitchTyp, final Tuple<Double, Double> travelTimes){
		storePerson2ModeSwitchinfo (personId, modeSwitchTyp);

		ModeSwitcherInfoCollector infoCollector = this.modeSwitchType2InfoCollector.get(modeSwitchTyp);
		if (infoCollector == null ) {
			infoCollector = new ModeSwitcherInfoCollector();
		}

		infoCollector.addPersonToList(personId);
		infoCollector.addToFirstIterationStats(travelTimes.getFirst());
		infoCollector.addToLastIterationStats(travelTimes.getSecond());

		this.modeSwitchType2InfoCollector.put(modeSwitchTyp, infoCollector);
	}

	private Map<Id<Person>, List<Tuple<String, Double>>> getPerson2mode2TripTimes(final String eventsFile) {

		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(eventsFile);
		mtta.run();

		SortedMap<String, Map<Id<Person>, List<Double>>> mode2Person2TripTimes = mtta.getMode2PesonId2TripTimes();
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

	public void writeResults(final String outputFolder){
		String outFile = outputFolder+"modeSwitchersTripTimes.txt";
		BufferedWriter writer =  IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("firstMode \t lastMode \t numberOfLegs \t totalTripTimesForFirstIterationInHr \t totalTripTimesForLastIterationInHr \n");

			for(Tuple<String, String> str: this.modeSwitchType2InfoCollector.keySet()){
				ModeSwitcherInfoCollector infoCollector = this.modeSwitchType2InfoCollector.get(str);
				writer.write(str.getFirst()+"\t" +
								str.getSecond()+"\t"+
								infoCollector.getNumberOfLegs()+"\t" +
								infoCollector.getFirstIterationStats()/3600.+"\t" +
								infoCollector.getLastIterationStats()/3600.+
								"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
		LOG.info("Data is written to "+outFile);
	}
}
