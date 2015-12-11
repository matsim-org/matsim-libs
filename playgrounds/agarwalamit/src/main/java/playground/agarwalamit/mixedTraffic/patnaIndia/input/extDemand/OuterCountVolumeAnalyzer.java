/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.agarwalamit.analysis.LinkVolumeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand.OuterCordonUtils.OuterCordonLinks;

/**
 * @author amit
 */

public class OuterCountVolumeAnalyzer {

	private LinkVolumeHandler handler = new LinkVolumeHandler();
	private SortedMap<Id<Link>, Tuple<Integer,Integer>> link2totalCounts = new TreeMap<>();

	public static void main(String[] args) {
		String outputFolder ="../../../../repos/runs-svn/patnaIndia/run108/outerCordonOutput/";
		String eventsFile = outputFolder+"/output_events.xml.gz";
		OuterCountVolumeAnalyzer ocva =	new OuterCountVolumeAnalyzer();
		ocva.run(eventsFile);
		ocva.writeData(outputFolder);
	}

	public void run(String eventsFile){
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(handler);
		reader.readFile(eventsFile);
		getVolume();
	}

	public void writeData(String outputFolder){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/external2InternalSimCountData.txt");
		try {
			writer.write("OuterCountStationNumber \t linkId \t ext-intCount \t ext-extCount \n");
			for(Id<Link> linkId : OuterCordonUtils.getExternalToInternalCountStationLinkIds()){
				writer.write(OuterCordonLinks.getOuterCordonNumberFromLink(linkId.toString())+"\t"+linkId+"\t"+link2totalCounts.get(linkId).getSecond()*PatnaUtils.COUNT_SCALE_FACTOR+"\t"+link2totalCounts.get(linkId).getFirst()*PatnaUtils.COUNT_SCALE_FACTOR+"\n");
			}
			writer.newLine();
			for(Id<Link> linkId : OuterCordonUtils.getInternalToExternalCountStationLinkIds()){
				writer.write(OuterCordonLinks.getOuterCordonNumberFromLink(linkId.toString())+"\t"+linkId+"\t"+link2totalCounts.get(linkId).getSecond()*PatnaUtils.COUNT_SCALE_FACTOR+"\t"+link2totalCounts.get(linkId).getFirst()*PatnaUtils.COUNT_SCALE_FACTOR+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason - "+e);
		}
	}

	private void getVolume(){
		Map<Id<Link>, Map<Integer, List<Id<Vehicle>>>> link2time2vehicles = handler.getLinkId2TimeSlot2VehicleIds();
		List<Id<Link>> allCountStationLinks = new ArrayList<>();
		allCountStationLinks.addAll(OuterCordonUtils.getExternalToInternalCountStationLinkIds());
		allCountStationLinks.addAll(OuterCordonUtils.getInternalToExternalCountStationLinkIds());
		for (Id<Link> linkId : allCountStationLinks){
			Map<Integer, List<Id<Vehicle>>> time2vehicles = link2time2vehicles.get(linkId);
			int E2ECount = 0;
			int E2ICount = 0;
			if(time2vehicles != null) {
				for(int t : time2vehicles.keySet() ){
					for (Id<Vehicle> veh : time2vehicles.get(t)){
						if ( OuterCordonUtils.isVehicleFromThroughTraffic(veh) ) E2ECount++;
						else E2ICount++;
					}
				}
			}
			link2totalCounts.put(linkId, new Tuple<Integer, Integer>(E2ECount, E2ICount));
		}
	}
}
