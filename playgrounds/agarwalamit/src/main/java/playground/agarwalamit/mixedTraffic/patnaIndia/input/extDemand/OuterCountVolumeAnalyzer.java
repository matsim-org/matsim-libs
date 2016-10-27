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

import playground.agarwalamit.analysis.linkVolume.LinkVolumeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class OuterCountVolumeAnalyzer {

	private final LinkVolumeHandler handler = new LinkVolumeHandler();
	private final SortedMap<Id<Link>, Tuple<Integer,Integer>> link2totalCounts = new TreeMap<>();
	private static final int COUNT_SCALE_FACTOR = 10;
	
	public static void main(String[] args) {
		String outputFolder ="../../../../repos/runs-svn/patnaIndia/run108/outerCordonOutput_10pct_OC1Excluded/";
		String eventsFile = outputFolder+"/output_events.xml.gz";
		OuterCountVolumeAnalyzer ocva =	new OuterCountVolumeAnalyzer();
		ocva.run(eventsFile);
		ocva.writeData(outputFolder);
		ocva.writeHourlyLinkCounts(outputFolder);
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
			for( Id<Link> linkId : OuterCordonUtils.getExternalToInternalCountStationLinkIds( PatnaUtils.PATNA_NETWORK_TYPE  ) ){
				writer.write(
						new OuterCordonLinks (PatnaUtils.PATNA_NETWORK_TYPE).getCountingStation(linkId.toString())+
						"\t"+linkId+
						"\t"+link2totalCounts.get(linkId).getSecond()*COUNT_SCALE_FACTOR+
						"\t"+link2totalCounts.get(linkId).getFirst()*COUNT_SCALE_FACTOR+
						"\n");
			}
			writer.newLine();
			for(Id<Link> linkId : OuterCordonUtils.getInternalToExternalCountStationLinkIds( PatnaUtils.PATNA_NETWORK_TYPE  )){
				writer.write(
						new OuterCordonLinks (PatnaUtils.PATNA_NETWORK_TYPE).getCountingStation(linkId.toString())+
						"\t"+linkId+
						"\t"+link2totalCounts.get(linkId).getSecond()*COUNT_SCALE_FACTOR+
						"\t"+link2totalCounts.get(linkId).getFirst()*COUNT_SCALE_FACTOR+
						"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason - "+e);
		}
	}
	
	public void writeHourlyLinkCounts(String outputFolder){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/hourlyLinkCounts.txt");
		List<Id<Link>> allCountStationLinks = new ArrayList<>();
		allCountStationLinks.addAll(OuterCordonUtils.getExternalToInternalCountStationLinkIds( PatnaUtils.PATNA_NETWORK_TYPE  ));
		allCountStationLinks.addAll(OuterCordonUtils.getInternalToExternalCountStationLinkIds( PatnaUtils.PATNA_NETWORK_TYPE  ));
		
		Map<Id<Link>, Map<Integer, Double>> link2time2volume = handler.getLinkId2TimeSlot2LinkCount();
		try {
			writer.write("timebin \t");
			for(Id<Link> linkId : allCountStationLinks){
				writer.write(new OuterCordonLinks (PatnaUtils.PATNA_NETWORK_TYPE).getCountingStation(linkId.toString())+"\t");
			}
			writer.newLine();
			for (int ii = 1; ii<=30;ii++){
				writer.write(ii+"\t");
				for(Id<Link> linkId : allCountStationLinks){
					double count = link2time2volume.get(linkId).containsKey(ii) ? link2time2volume.get(linkId).get(ii) : 0.;
					writer.write(  count * COUNT_SCALE_FACTOR + "\t" );
				}	
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason - "+e);
		}
	}

	private void getVolume(){
		Map<Id<Link>, Map<Integer, List<Id<Vehicle>>>> link2time2vehicles = handler.getLinkId2TimeSlot2VehicleIds();
		List<Id<Link>> allCountStationLinks = new ArrayList<>();
		allCountStationLinks.addAll(OuterCordonUtils.getExternalToInternalCountStationLinkIds(PatnaUtils.PATNA_NETWORK_TYPE));
		allCountStationLinks.addAll(OuterCordonUtils.getInternalToExternalCountStationLinkIds(PatnaUtils.PATNA_NETWORK_TYPE));
		for (Id<Link> linkId : allCountStationLinks){
			Map<Integer, List<Id<Vehicle>>> time2vehicles = link2time2vehicles.get(linkId);
			int e2eCount = 0;
			int e2iCount = 0;
			if(time2vehicles != null) {
				for(int t : time2vehicles.keySet() ){
					for (Id<Vehicle> veh : time2vehicles.get(t)){
						if ( OuterCordonUtils.isVehicleFromThroughTraffic(veh) ) e2eCount++;
						else e2iCount++;
					}
				}
			}
			link2totalCounts.put(linkId, new Tuple<>(e2eCount, e2iCount));
		}
	}
}
