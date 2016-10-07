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
package playground.agarwalamit.mixedTraffic.seepage.TestSetUp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;


/**
 * @author amit
 */
public class AverageLinkFlowHandler implements LinkEnterEventHandler, LinkLeaveEventHandler{

	private final Map<Id<Link>,List<Double>> enterTimes = new HashMap<>();
	private final Map<Id<Link>,List<Double>> leaveTimes = new HashMap<>();
	public static final Logger LOG = Logger.getLogger(AverageLinkFlowHandler.class);
	
	public static void main(String[] args) {
		String outputDir =  "/Users/amit/Documents/repos/shared-svn/projects/mixedTraffic/seepage/xt_1Link/seepage/";
		String eventsFile = outputDir+"ITERS/it.0/0.events.xml.gz";
		
		AverageLinkFlowHandler ana = new AverageLinkFlowHandler();
		ana.startProcessingEventsFile(eventsFile);
	}
	
	private void startProcessingEventsFile(String eventsFile){
		EventsManager events = EventsUtils.createEventsManager();
		AverageLinkFlowHandler linkFlow = new AverageLinkFlowHandler();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(linkFlow);
		reader.readFile(eventsFile);
		
		LOG.info("Inflow : - "+linkFlow.getInflow().toString());
		LOG.info("Outflow : - "+linkFlow.getOutflow().toString());
	}
	
	@Override
	public void reset(int iteration) {
		enterTimes.clear();
		leaveTimes.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(leaveTimes.containsKey(event.getLinkId())){
			List<Double> times = leaveTimes.get(event.getLinkId());
			times.add(event.getTime());
		} else {
			List<Double> times = new ArrayList<>();
			times.add(event.getTime());
			leaveTimes.put(event.getLinkId(), times);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(enterTimes.containsKey(event.getLinkId())){
			List<Double> times = enterTimes.get(event.getLinkId());
			times.add(event.getTime());
		} else {
			List<Double> times = new ArrayList<>();
			times.add(event.getTime());
			enterTimes.put(event.getLinkId(), times);
		}
	}

	public Map<Id<Link>, Double> getInflow(){
		Map<Id<Link>, Double> linkId2Inflow = new HashMap<>();
		for(Id<Link> id : enterTimes.keySet()){
			double inflow;
			double firstVehEnterTime =Collections.min(enterTimes.get(id)); 
			double lastVehEnterTime = Collections.max(enterTimes.get(id));
			double totalEnteredVeh = enterTimes.get(id).size();
			inflow = totalEnteredVeh * 3600 / (lastVehEnterTime-firstVehEnterTime);
			linkId2Inflow.put(id, inflow);
		}
		return linkId2Inflow;
	}

	public Map<Id<Link>, Double> getOutflow(){
		Map<Id<Link>, Double> linkId2Outflow = new HashMap<>();
		for(Id<Link> id : leaveTimes.keySet()){
			double outflow;
			double firstVehLeaveTime =Collections.min(leaveTimes.get(id)); 
			double lastVehLeaveTime = Collections.max(leaveTimes.get(id));
			double totalLeftVeh = leaveTimes.get(id).size();
			outflow = totalLeftVeh * 3600 / (lastVehLeaveTime-firstVehLeaveTime);
			linkId2Outflow.put(id, outflow);
		}
		return linkId2Outflow;
	}
}
