/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.lib.tools.events;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;


// TODO: negative part missing!  => print to console...
public class MeasureDifferenceInTrafficCounts {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventsFilePathA = "C:/data/parkingSearch/psim/zurich/output/run14/output/aaa/it.0.events_withSearch.xml";
		String eventsFilePathB = "C:/data/parkingSearch/psim/zurich/output/run14/output/aaa/it.0.events_noSearch.xml";

		TrafficCountsCollector trafficCountsCollectorA = new TrafficCountsCollector(eventsFilePathA);
		TrafficCountsCollector trafficCountsCollectorB = new TrafficCountsCollector(eventsFilePathB);

		HashSet<Id> usedLinks=new HashSet();
		
		usedLinks.addAll(trafficCountsCollectorA.linkToFrequencyCounts.getKeySet());
		usedLinks.addAll(trafficCountsCollectorB.linkToFrequencyCounts.getKeySet());

		double[] difference=new double[usedLinks.size()];
		int i=0;
		for (Id linkId:usedLinks){
			difference[i]=trafficCountsCollectorA.linkToFrequencyCounts.get(linkId)-trafficCountsCollectorB.linkToFrequencyCounts.get(linkId);
			i++;
		}
		
		GeneralLib.generateHistogram("C:/data/parkingSearch/psim/zurich/output/run14/output/aaa/histogram.png", difference, 10, "traffic counts difference", "traffic difference (numbers of cars)", "%");
	}

	private static class TrafficCountsCollector implements LinkLeaveEventHandler {

		public IntegerValueHashMap<Id> linkToFrequencyCounts;
		
		public TrafficCountsCollector(String eventsFilePathA){
			linkToFrequencyCounts=new IntegerValueHashMap<Id>();
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(this);

			EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
			reader.parse(eventsFilePathA);
		}
		
		@Override
		public void reset(int iteration) {

		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			linkToFrequencyCounts.increment(event.getLinkId());
		}

	}

}
