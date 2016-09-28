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

package playground.agarwalamit.mixedTraffic.patnaIndia.analysis;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.linkVolume.LinkVolumeHandler;

/**
 * @author amit
 */

public class PatnaLinkVolumeWriter {
	
	private static final String dir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/shpNetwork/incomeDependent/c13/";
	private static final String eventsFile = dir+"/output_events.xml.gz";
	private Map<Id<Link>, Map<Integer, Double>> linkid2Time2Count = new HashMap<>();
	
	public static void main(String[] args) {
		PatnaLinkVolumeWriter plvw = new PatnaLinkVolumeWriter();
		plvw.processEventsFile(eventsFile);
		plvw.writeData(dir+"/analysis/link2time2Vol.txt");
	}
	
	public void processEventsFile(final String eventsFile){
		LinkVolumeHandler handler = new LinkVolumeHandler();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		this.linkid2Time2Count = handler.getLinkId2TimeSlot2LinkCount();
	}
	
	public void writeData (final String outFile){
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("linkId\ttimeBin\tcount\n");
			for(Id<Link> l : this.linkid2Time2Count.keySet()){
				for (int i : this.linkid2Time2Count.get(l).keySet() ){
					writer.write(l+"\t"+i+"\t"+this.linkid2Time2Count.get(l).get(i)+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}

}
