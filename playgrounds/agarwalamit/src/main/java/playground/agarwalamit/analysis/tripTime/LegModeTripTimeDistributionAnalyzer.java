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
package playground.agarwalamit.analysis.tripTime;

import java.io.BufferedWriter;
import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.PersonFilter;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */
public class LegModeTripTimeDistributionAnalyzer extends AbstractAnalysisModule {

	private final List<Integer> timeClasses = new ArrayList<>();
	private final SortedSet<String> usedModes = new TreeSet<>();
	private static final Logger LOG = Logger.getLogger(LegModeTripTimeDistributionAnalyzer.class);

	private final SortedMap<String, SortedMap<Integer, Integer>> mode2timeClass2LegCount = new TreeMap<>();
	private final SortedMap<String, Map<Id<Person>, List<Double>>> mode2PersonId2tripTimes = new TreeMap<>();
	private final ModalTripTravelTimeHandler lmtth;
	private final String eventsFile;
	private final String userGroup;
	private final PersonFilter personFilter;

	public LegModeTripTimeDistributionAnalyzer(final String eventsFile, final String userGroup, final PersonFilter personFilter) {
		super(LegModeTripTimeDistributionAnalyzer.class.getSimpleName());
		this.lmtth = new ModalTripTravelTimeHandler();
		this.eventsFile = eventsFile;
		LOG.info("enabled");

		this.userGroup = userGroup;
		this.personFilter = personFilter;
		if(this.userGroup != null && this.personFilter != null) LOG.info("Results will include persons from "+this.userGroup+" only.");
	}

	public LegModeTripTimeDistributionAnalyzer(final String eventsFile) {
		this(eventsFile, null, null);
	}

	public static void main(String[] args) {
		String runDir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run10/policies/";
		String [] runs = {"bau","ei","ci","eci","10ei"};

		for(String run:runs){
			String configFile = runDir+run+"/output_config.xml.gz";
			int lastItr = LoadMyScenarios.getLastIteration(configFile);
			String eventsFile = runDir+run+"/ITERS/it."+lastItr+"/"+lastItr+".events.xml.gz";

			LegModeTripTimeDistributionAnalyzer lmttd = new LegModeTripTimeDistributionAnalyzer(eventsFile);
			lmttd.preProcessData();
			lmttd.postProcessData();
			lmttd.writeResults(runDir+"/analysis/legModeDistributions/"+run+"_it."+lastItr+"_");
		}
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(lmtth);
		reader.readFile(eventsFile);
		
		this.mode2PersonId2tripTimes.putAll(lmtth.getLegMode2PesonId2TripTimes());
		initializeTimeClasses();
		this.usedModes.addAll(this.mode2PersonId2tripTimes.keySet());

		LOG.info("The following transport modes are considered: " + this.usedModes);

		for(String mode:this.usedModes){
			SortedMap<Integer, Integer> distClass2Legs = new TreeMap<>();
			for(int i: this.timeClasses){
				distClass2Legs.put(i, 0);
			}
			this.mode2timeClass2LegCount.put(mode, distClass2Legs);
		}
	}

	@Override
	public void postProcessData() {
		calculateMode2TimeClass2LegCount();
	}

	private void calculateMode2TimeClass2LegCount() {
		for(String mode : this.usedModes){
			SortedMap<Integer, Integer> timeClass2NoOfLegs = this.mode2timeClass2LegCount.get(mode);
			for(int i=0;i<this.timeClasses.size()-1;i++){
				int legCount = 0;

				for(Id<Person> personId :mode2PersonId2tripTimes.get(mode).keySet()){
					if (this.userGroup!=null && this.personFilter!= null){
						for(double tripTime : mode2PersonId2tripTimes.get(mode).get(personId)){
							if(tripTime > this.timeClasses.get(i) && tripTime <= this.timeClasses.get(i + 1)){
								legCount++;
							}
						}
					} else {
						for(double tripTime : mode2PersonId2tripTimes.get(mode).get(personId)){
							if(tripTime > this.timeClasses.get(i) && tripTime <= this.timeClasses.get(i + 1)){
								legCount++;
							}
						}
					}

				}
				timeClass2NoOfLegs.put(this.timeClasses.get(i+1), legCount);
			}
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		String outFile = outputFolder + ".legModeTripTimeDistribution.txt";
		try{
			BufferedWriter writer1 = IOUtils.getBufferedWriter(outFile);
			writer1.write("class");
			for(String mode : this.usedModes){
				writer1.write("\t" + mode);
			}
			writer1.write("\t" + "sum");
			writer1.write("\n");
			for(int i = 0; i < this.timeClasses.size() - 1 ; i++){
				writer1.write(this.timeClasses.get(i+1) + "\t");
				Integer totalLegsInTimeClass = 0;
				for(String mode : this.usedModes){
					Integer modeLegs = null;
					modeLegs = this.mode2timeClass2LegCount.get(mode).get(this.timeClasses.get(i + 1));
					totalLegsInTimeClass = totalLegsInTimeClass + modeLegs;
					writer1.write(modeLegs.toString() + "\t");
				}
				writer1.write(totalLegsInTimeClass.toString());
				writer1.write("\n");
			}
			writer1.close();
			LOG.info("Finished writing output to " + outFile);
		}catch (Exception e){
			LOG.error("Data is not written. Reason " + e.getMessage());
		}
	}

	private void initializeTimeClasses() {
		double longestTripTime = getHighestTravelTime();
		LOG.info("The longest trip time is found to be: " + longestTripTime);
		int endOfTimeClass = 0;
		int classCounter = 0;
		this.timeClasses.add(endOfTimeClass);

		while(endOfTimeClass <= longestTripTime){
			endOfTimeClass = 100 * (int) Math.pow(2, classCounter);
			classCounter++;
			this.timeClasses.add(endOfTimeClass);
		}
		LOG.info("The following trip time classes were defined: " + this.timeClasses);
	}

	private double getHighestTravelTime(){
		double highestTravelTime = Double.NEGATIVE_INFINITY;
		for(String mode : this.mode2PersonId2tripTimes.keySet()){
			for(Id<Person> id : this.mode2PersonId2tripTimes.get(mode).keySet()){
				for(double time : this.mode2PersonId2tripTimes.get(mode).get(id)){
					if(highestTravelTime<time){
						highestTravelTime = time;
					}
				}
			}
		}
		return highestTravelTime;
	}

	public SortedMap<String, SortedMap<Integer, Integer>> getMode2TripTimeClass2LegCount() {
		return this.mode2timeClass2LegCount;
	}

	public SortedMap<String, Map<Id<Person>, List<Double>>> getMode2PersonId2TripTimes(){
		return this.mode2PersonId2tripTimes;
	}
}