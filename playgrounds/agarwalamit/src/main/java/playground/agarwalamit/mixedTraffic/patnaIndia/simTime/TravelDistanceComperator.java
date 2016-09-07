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

import playground.agarwalamit.analysis.legMode.distributions.LegModeRouteDistanceDistributionHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.ListUtils;

/**
 * @author amit
 */

public class TravelDistanceComperator {
	
	private String respectiveFileDirectory = "../../../../repos/runs-svn/patnaIndia/run106/10pct/";
	private String netFile = "../../../../repos/runs-svn/patnaIndia/inputs/network.xml";
	private BufferedWriter writer;
	private static final String LAST_IT = "200";

	public static void main(String[] args) {
		TravelDistanceComperator tdc = new TravelDistanceComperator();
		tdc.openFile();
		tdc.startProcessing();
		tdc.closeFile();
	}

	public void openFile(){
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

	public void closeFile(){
		try{
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file. Reason :"+ e);
		}
	}

	public void startProcessing(){
		for (LinkDynamics ld : LinkDynamics.values() ) {
			for ( TrafficDynamics td : TrafficDynamics.values()){
				String queueModel = ld+"_"+td;
				for(int i=2;i<12;i++){
					String eventsFile = respectiveFileDirectory + "/output_"+queueModel+"_"+i+"/ITERS/it."+LAST_IT+"/"+LAST_IT+".events.xml.gz";
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
		LegModeRouteDistanceDistributionHandler handler = new LegModeRouteDistanceDistributionHandler(sc);
		manager.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		reader.readFile(eventsFile);
		
		SortedMap<String,Double> mode2dists = new TreeMap<>();
		
		SortedMap<String,Map<Id<Person>,List<Double>>> dists = handler.getMode2PersonId2TravelDistances();
		
		for (String mode :dists.keySet()) {
			double modedistSum = 0;
			double count = 0;
			for(Id<Person> p : dists.get(mode).keySet()){
				count += dists.get(mode).get(p).size();
				modedistSum += ListUtils.doubleSum(dists.get(mode).get(p));
			}
			mode2dists.put(mode, modedistSum/(count*1000));
		}
		return mode2dists;
	}
}
