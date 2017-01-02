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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis;

import java.io.BufferedWriter;
import java.util.Map;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.linkVolume.FilteredLinkVolumeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.MapUtils;

/**
 * @author amit
 */

public class PatnaPolicyLinkVolumeWriter {

	private static final String dir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/";
	private static final String eventsFileBAU = dir+"/bau/output_events.xml.gz";
    private static final String eventsFilePolicy = dir+"/BT-b/output_events.xml.gz";

    private static final int timeSlot = 7; // 7 to 8 am

	public static void main(String[] args) {
		PatnaPolicyLinkVolumeWriter plvw = new PatnaPolicyLinkVolumeWriter();
		Map<Id<Link>, Map<Integer, Double>> linkid2Volume_bau = plvw.processEventsFile(eventsFileBAU, dir+"/bau/output_vehicles.xml.gz");
		Map<Id<Link>, Double> linkVol_bau = linkid2Volume_bau.entrySet().stream().collect(
				Collectors.toMap(
//						entry -> entry.getKey(), entry -> MapUtils.doubleValueSum(entry.getValue())
						entry -> entry.getKey(), entry -> entry.getValue().containsKey(timeSlot) ? entry.getValue().get(timeSlot) : 0.
				)
		);

//		plvw.writeData(dir+"/analysis/link2Vol_bau.txt", linkid2Volume_bau);
//		plvw.writeData(dir+"/analysis/link2Vol_7to8_bau.txt", linkVol_bau);

		// diff
		Map<Id<Link>, Map<Integer, Double>> linkid2Volume_policy = plvw.processEventsFile(eventsFilePolicy, dir+"/bau/output_vehicles.xml.gz");
		Map<Id<Link>, Double> linkVol_policy = linkid2Volume_policy.entrySet().stream().collect(
				Collectors.toMap(
//						entry -> entry.getKey(), entry -> MapUtils.doubleValueSum(entry.getValue())
						entry -> entry.getKey(), entry -> entry.getValue().containsKey(timeSlot) ? entry.getValue().get(timeSlot) : 0.
				)
		);

		Map<Id<Link>, Double> linkVol_policy_diff = MapUtils.subtractMaps(linkVol_bau, linkVol_policy);
		plvw.writeData(dir+"/analysis/link2Vol_7to8_BSH-b_WRT_bau.txt", linkVol_policy_diff);
	}

	private Map<Id<Link>, Map<Integer, Double>> processEventsFile(final String eventsFile, final String vehiclesFile) {

		FilteredLinkVolumeHandler handler = new FilteredLinkVolumeHandler(vehiclesFile, PatnaPersonFilter.PatnaUserGroup.urban.toString(), new PatnaPersonFilter());
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		return handler.getLinkId2TimeSlot2LinkVolumePCU();
	}

//	private void writeData (final String outFile, final Map<Id<Link>, Map<Integer, Double>> linkid2Volume){
//		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
//			writer.write("linkId\tvolumePCU\n");
//			for(Id<Link> l : linkid2Volume.keySet()){
//				writer.write(l+"\t"+ MapUtils.doubleValueSum( linkid2Volume.get(l) )+"\n");
//			}
//			writer.close();
//		} catch (Exception e) {
//			throw new RuntimeException("Data is not written. Reason "+e );
//		}
//	}

	private void writeData (final String outFile, final Map<Id<Link>, Double> linkid2Volume){
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("linkId\tvolumePCU\n");
			for(Id<Link> l : linkid2Volume.keySet()){
				writer.write(l+"\t"+ linkid2Volume.get(l) +"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}
}
