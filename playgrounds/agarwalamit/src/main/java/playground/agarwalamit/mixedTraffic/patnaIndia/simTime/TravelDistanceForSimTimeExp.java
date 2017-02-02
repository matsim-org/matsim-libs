/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.simTime;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.tripDistance.TripDistanceHandler;
import playground.agarwalamit.analysis.tripDistance.ModeFilterTripDistanceHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.ListUtils;

/**
 * @author amit
 */

public class TravelDistanceForSimTimeExp {
	
	private final String respectiveFileDirectory ;
	private final String netFile ;
	private BufferedWriter writer;

	TravelDistanceForSimTimeExp(final String dir, final String networkFile) {
		this.respectiveFileDirectory = dir;
		this.netFile = networkFile;
	}

	public static void main(String[] args) {
		TravelDistanceForSimTimeExp tdc = new TravelDistanceForSimTimeExp(FileUtils.RUNS_SVN+"/patnaIndia/run110/randomNrFix/fastCapacityUpdate/1pct/",
				FileUtils.RUNS_SVN+"/patnaIndia/inputs/network.xml");
		tdc.run();
	}

	void run (){
		openFile();
		startProcessing();
		closeFile();
	}

	private void openFile(){
		writer = IOUtils.getBufferedWriter(respectiveFileDirectory+"/travelDistance.txt");
		try {
			writeString("scenario\tmode\ttravelDistKm\n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	private void writeString(String str){
		try{
			writer.write(str);
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	private void closeFile(){
		try{
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	private void startProcessing(){
		for (LinkDynamics ld : LinkDynamics.values() ) {
			for ( TrafficDynamics td : TrafficDynamics.values()){
				String queueModel = ld+"_"+td;
				for(int i=1;i<12;i++){
					String eventsFile = respectiveFileDirectory + "/output_"+queueModel+"_"+i+"/output_events.xml.gz";
					if (! new File(eventsFile).exists() ) continue;
					SortedMap<String,Double> mode2avgDist = getMode2Dist(eventsFile);
					
					for (String mode : mode2avgDist.keySet()) {
						writeString(queueModel+"\t"+mode+"\t"+mode2avgDist.get(mode)+"\n");
					}
				}
			}
		}
	}
	
	private SortedMap<String,Double> getMode2Dist(String eventsFile) {
		Config config = ConfigUtils.createConfig();
		config.qsim().setMainModes(PatnaUtils.URBAN_MAIN_MODES);
		config.network().setInputFile(netFile);
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		EventsManager manager = EventsUtils.createEventsManager();
		ModeFilterTripDistanceHandler handler_bike = new ModeFilterTripDistanceHandler(sc.getNetwork(),sc.getConfig().qsim().getEndTime(),1,"bike");
		ModeFilterTripDistanceHandler handler_car = new ModeFilterTripDistanceHandler(sc.getNetwork(),sc.getConfig().qsim().getEndTime(),1,"car");
		TripDistanceHandler handler = new TripDistanceHandler(sc);
		manager.addHandler(handler);
		manager.addHandler(handler_bike);
		manager.addHandler(handler_car);
		
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		reader.readFile(eventsFile);
		
		SortedMap<String,Double> mode2dists = new TreeMap<>();
		SortedMap<String,Map<Id<Person>,List<Double>>> dists = handler.getMode2PersonId2TravelDistances();

		{
			double [] sumCount = getAvgDist(handler_bike.getTimeBin2Person2TripsDistance().entrySet().iterator().next().getValue());
			System.out.println("bike "+ sumCount[0]/sumCount[1]);
		}

		{
			double [] sumCount = getAvgDist(handler_car.getTimeBin2Person2TripsDistance().entrySet().iterator().next().getValue());
			System.out.println("car "+ sumCount[0]/sumCount[1]);
		}


		for (String mode :dists.keySet()) {
			double sumAndCount [] = getAvgDist(dists.get(mode));
			mode2dists.put(mode, sumAndCount[0]/(sumAndCount[1]*1000.));
		}
		return mode2dists;
	}

	private double [] getAvgDist (final Map<Id<Person>,List<Double>> dists) {
		double modedistSum = 0;
		double count = 0;
		for(Id<Person> p : dists.keySet()){
			count += dists.get(p).size();
			modedistSum += ListUtils.doubleSum(dists.get(p));
		}
		return new double [] {modedistSum, count};
	}
}
