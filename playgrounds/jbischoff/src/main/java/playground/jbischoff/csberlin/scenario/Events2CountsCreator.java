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

/**
 * 
 */
package playground.jbischoff.csberlin.scenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.vehicles.Vehicle;

/**
 * @author  jbischoff
 *
 */
/**
 * 	
 */
public class Events2CountsCreator {
	
	
	
public static void main(String[] args) {
	new Events2CountsCreator().run();
}

void run(){
	final Map<Id<Link>,double[]> monitoredLinks = new HashMap<>();
	
	monitoredLinks.put(Id.createLinkId(92773),new double[24]);
	monitoredLinks.put(Id.createLinkId(92772),new double[24]);
	monitoredLinks.put(Id.createLinkId(36109),new double[24]);
	monitoredLinks.put(Id.createLinkId(36108),new double[24]);
	monitoredLinks.put(Id.createLinkId(6813),new double[24]);
	monitoredLinks.put(Id.createLinkId(6812),new double[24]);
	monitoredLinks.put(Id.createLinkId(35653),new double[24]);
	monitoredLinks.put(Id.createLinkId(35652),new double[24]);
	
	EventsManager events = EventsUtils.createEventsManager();
	events.addHandler(new LinkEnterEventHandler() {
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (monitoredLinks.containsKey(event.getLinkId())){
				int hour = (int) (event.getTime() / 3600);
				if (hour<24){
					monitoredLinks.get(event.getLinkId())[hour]++;
				}
			}
		}
	});
	new MatsimEventsReader(events).readFile("D:/runs-svn/bmw_carsharing/basecase/bc09-nopark/output_events.xml.gz");
	
	Counts<Link> counts = new Counts<>();
	for (Entry<Id<Link>, double[]> e : monitoredLinks.entrySet()){
	counts.createAndAddCount(e.getKey(), e.getKey().toString());
	Count count = counts.getCount(e.getKey());
	for (int i = 0; i<e.getValue().length;i++){
		count.createVolume(i+1, e.getValue()[i]);
	}
	new CountsWriter(counts).write("D:/runs-svn/bmw_carsharing/basecase/bc09-nopark/experiencedCounts.xml");
	}
}
}

