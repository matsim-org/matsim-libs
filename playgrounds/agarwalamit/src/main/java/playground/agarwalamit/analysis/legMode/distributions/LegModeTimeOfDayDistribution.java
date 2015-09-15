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

package playground.agarwalamit.analysis.legMode.distributions;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * 
 * @author amit
 */

public class LegModeTimeOfDayDistribution extends AbstractAnalysisModule{

	public LegModeTimeOfDayDistribution (UserGroup ug, String eventsFile, double simulationEndTime, int noOfTimeBins){
		super(LegModeTimeOfDayDistribution.class.getSimpleName());
		this.lmtdh = new LegModeTimeOfDayHandler(simulationEndTime, noOfTimeBins, ug);
		this.eventsFile = eventsFile;
		this.outFilePrefix = ug.toString()+"_";
	}

	public LegModeTimeOfDayDistribution (String eventsFile, double simulationEndTime, int noOfTimeBins){
		super(LegModeTimeOfDayDistribution.class.getSimpleName());
		this.lmtdh = new LegModeTimeOfDayHandler(simulationEndTime, noOfTimeBins);
		this.eventsFile = eventsFile;
	}

	public static void main(String[] args) {
		String outDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/eci/";
		String eventsFile = outDir+"/ITERS/it.1500/1500.events.xml.gz";
		LegModeTimeOfDayDistribution lmtdd = new LegModeTimeOfDayDistribution(eventsFile, 30*3600, 30);
		lmtdd.preProcessData();
		lmtdd.postProcessData();
		lmtdd.writeResults(outDir+"/analysis/");
	}

	private LegModeTimeOfDayHandler lmtdh;
	private String eventsFile ; 
	private String outFilePrefix= "";

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager events  = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(this.lmtdh);
		reader.readFile(eventsFile);
	}

	@Override
	public void postProcessData() {

	}

	@Override
	public void writeResults(String outputFolder) {

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/"+this.outFilePrefix+"timeOfDayToLegMode2Count.txt");
		try {
			writer.write("timeOfDay \t mode \t count \n");
			SortedMap<Double, SortedMap<String, Integer>> time2mode2count = this.getLegModeToTimeOfDayCount();

			for(Double d : time2mode2count.keySet()){
				for(String mode : time2mode2count.get(d).keySet()){
					writer.write(d+"\t"+mode+"\t"+time2mode2count.get(d).get(mode)+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason "+e);
		}

	}

	public SortedMap<Double, SortedMap<String, Integer>> getLegModeToTimeOfDayCount (){
		SortedMap<Double, SortedMap<String, Integer>> outMap = new TreeMap<Double, SortedMap<String,Integer>>();

		SortedMap<Double, SortedMap<String, Integer>> timeBin2Mode2Count = this.lmtdh.timeBin2Mode2Count;

		for (double d : timeBin2Mode2Count.keySet()){
			SortedMap<String, Integer> mode2Count = new TreeMap<String, Integer>();

			for(String mode : this.lmtdh.modes){
				mode2Count.put(mode, timeBin2Mode2Count.get(d).containsKey(mode) ? timeBin2Mode2Count.get(d).get(mode) : 0 );
			}
			outMap.put(d, mode2Count);
		}
		return outMap;
	}

	private class LegModeTimeOfDayHandler implements PersonDepartureEventHandler {

		private final Logger log = Logger.getLogger(LegModeTimeOfDayDistribution.class);

		private final double timeBinSize;
		private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
		private final SortedMap<Double, SortedMap<String, Integer>> timeBin2Mode2Count = new TreeMap<Double, SortedMap<String,Integer>>();
		private final UserGroup ug;
		private final List<String> modes = new ArrayList<String>();

		private LegModeTimeOfDayHandler(double simulationEndTime, int noOfTimeBins) {
			log.info("The output will contain data from whole population.");
			this.timeBinSize = simulationEndTime/noOfTimeBins;
			this.ug  = null;
		}

		private LegModeTimeOfDayHandler(double simulationEndTime, int noOfTimeBins, UserGroup ug) {
			log.info("The output will contain data only from the user group "+ug);
			this.timeBinSize = simulationEndTime/noOfTimeBins;
			this.ug = ug;
		}

		@Override
		public void reset(int iteration) {
			timeBin2Mode2Count.clear();
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			UserGroup thisUserGroup = this.pf.getUserGroupFromPersonId(event.getPersonId());
			if(this.ug != null && !this.ug.equals(thisUserGroup)) return;

			Double time = event.getTime();
			if(time == 0.) time = this.timeBinSize;
			double endOfTimeInterval = 0.0;

			endOfTimeInterval = Math.ceil(time/timeBinSize)*timeBinSize;
			if(endOfTimeInterval <= 0.) endOfTimeInterval = timeBinSize;

			if(this.timeBin2Mode2Count.containsKey(endOfTimeInterval)){
				SortedMap<String, Integer> mode2count = this.timeBin2Mode2Count.get(endOfTimeInterval);
				if(mode2count.containsKey(event.getLegMode())){
					int previousCount = mode2count.get(event.getLegMode());
					mode2count.put(event.getLegMode(), previousCount+1);
				} else {
					mode2count.put(event.getLegMode(), 1);
				}
			} else {
				SortedMap<String, Integer> mode2count = new TreeMap<String, Integer>();
				mode2count.put(event.getLegMode(), 1);
				timeBin2Mode2Count.put(endOfTimeInterval, mode2count);
			}
			if (! modes.contains(event.getLegMode()) ) modes.add(event.getLegMode());
		}
	}
}


