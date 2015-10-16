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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * 
 * @author amit
 */

public class CarDepartureTimeDistribution extends AbstractAnalysisModule{

	public CarDepartureTimeDistribution (String eventsFile, double simulationEndTime, int noOfTimeBins){
		super(CarDepartureTimeDistribution.class.getSimpleName());
		this.handler = new CarDepartureTimeHandler(simulationEndTime, noOfTimeBins);
		this.eventsFile = eventsFile;
	}

	public static void main(String[] args) {

		String outDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/bau/";
		String eventsFile = outDir+"/ITERS/it.1500/1500.events.xml.gz";

		double simEndTime = LoadMyScenarios.getSimulationEndTime(outDir+"output_config.xml.gz");

		CarDepartureTimeDistribution lmtdd = new CarDepartureTimeDistribution(eventsFile, simEndTime, 30);
		lmtdd.preProcessData();
		lmtdd.postProcessData();
		lmtdd.writeResults(outDir+"/analysis/");
	}

	private CarDepartureTimeHandler handler;
	private String eventsFile ; 

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager events  = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(this.handler);
		reader.readFile(eventsFile);
	}

	@Override
	public void postProcessData() {

	}

	@Override
	public void writeResults(String outputFolder) {

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"carDepartureCounts.txt");
		try {
			writer.write("departureTime \t");
			for(UserGroup ug :UserGroup.values() ) {
				writer.write(ug+"\t");
			}
			writer.newLine();

			for (double d = 1; d<=30; d++) {
				writer.write( (int) d +"\t");

				for(UserGroup ug :UserGroup.values() ) {
					int count = 0;
					if ( this.handler.userGrp2TimeBin2Count.get(ug).containsKey( d*3600.0) ) count = this.handler.userGrp2TimeBin2Count.get(ug).get(d*3600.0);;
					
					writer.write(count + "\t"); 
				}
				writer.newLine();
			}
			writer.close();
		}  catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason "+e);
		}
	}

	private class CarDepartureTimeHandler implements PersonDepartureEventHandler {

		private final double timeBinSize;
		private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
		private final SortedMap<UserGroup, SortedMap<Double, Integer>> userGrp2TimeBin2Count = new TreeMap<UserGroup, SortedMap<Double,Integer>>();

		private CarDepartureTimeHandler(double simulationEndTime, int noOfTimeBins) {
			this.timeBinSize = simulationEndTime/noOfTimeBins;

			initializeMap();
		}

		@Override
		public void reset(int iteration) {
			userGrp2TimeBin2Count.clear();

			initializeMap();
		}

		private void initializeMap(){
			for (UserGroup ug :UserGroup.values()){
				this.userGrp2TimeBin2Count.put(ug, new TreeMap<Double, Integer>());
			}
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {

			if( ! event.getLegMode().equals(TransportMode.car)) return;

			UserGroup ug = this.pf.getUserGroupFromPersonId(event.getPersonId());

			Double time = event.getTime();
			if(time == 0.) time = this.timeBinSize;
			double endOfTimeInterval = Math.ceil(time/timeBinSize)*timeBinSize;
			if(endOfTimeInterval <= 0.) endOfTimeInterval = timeBinSize;

			SortedMap<Double, Integer> timeBin2Count = this.userGrp2TimeBin2Count.get(ug);

			if( timeBin2Count.containsKey(endOfTimeInterval) ) {
				timeBin2Count.put( endOfTimeInterval, 1 + timeBin2Count.get(endOfTimeInterval) );
			} else {
				timeBin2Count.put(endOfTimeInterval, 1);
			}
		}
	}
}