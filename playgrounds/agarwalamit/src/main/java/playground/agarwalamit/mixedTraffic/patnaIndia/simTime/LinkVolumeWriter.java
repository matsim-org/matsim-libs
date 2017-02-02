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
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.linkVolume.LinkVolumeHandler;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.MapUtils;

/**
 * @author amit
 */

public class LinkVolumeWriter {

	private static final String dir = FileUtils.RUNS_SVN+"/patnaIndia/run110/randomNrFix/slowCapacityUpdate/100pct/output_PassingQ_withHoles_2/";
	private static final String eventsFile = dir+"/output_events.xml.gz";
	private Map<Id<Link>, Map<Integer, Double>> linkid2Volume = new HashMap<>();

	public static void main(String[] args) {
		LinkVolumeWriter plvw = new LinkVolumeWriter();
		plvw.processEventsFile(eventsFile);
		plvw.writeData(dir+"/analysis/link2Vol.txt");
	}

	private void processEventsFile(final String eventsFile){
		String vehicleFile = dir+"/output_vehicles.xml.gz";
		LinkVolumeHandler handler = new LinkVolumeHandler(vehicleFile);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		this.linkid2Volume = handler.getLinkId2TimeSlot2LinkVolumePCU();
	}

	private void writeData (final String outFile){
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("linkId\tvolumePCU\n");
			for(Id<Link> l : this.linkid2Volume.keySet()){
				writer.write(l+"\t"+ MapUtils.doubleValueSum( this.linkid2Volume.get(l) )+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}

}
