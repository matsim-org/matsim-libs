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
package playground.agarwalamit.analysis.tripTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.EventHandler;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */

public class LinkTravelTimeCalculator extends AbstractAnalysisModule {
	private final String eventsFile;
	private LinkTravelTimeHandler ltth ;

	public LinkTravelTimeCalculator(final String eventsFile){
		super(LinkTravelTimeCalculator.class.getSimpleName());
		this.eventsFile = eventsFile;
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		this.ltth = new LinkTravelTimeHandler();
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(ltth);
		reader.readFile(eventsFile);
	}

	@Override
	public void postProcessData() {
		// nothing to do
	}

	@Override
	public void writeResults(String outputFolder) {

	}

	public Map<Id<Link>, Map<Id<Person>, List<Double>>> getLink2Person2TravelTime() {
		return this.ltth.getLink2PersonTravelTime();
	}

	public class LinkTravelTimeHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
		private final  Map<Id<Link>,Map<Id<Person>,Double>> link2PersonEnterTime = new HashMap<>();
		private final Map<Id<Link>,Map<Id<Person>,List<Double>>> link2PersonTravelTime = new HashMap<>();
		private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

		@Override
		public void reset(int iteration) {
			link2PersonEnterTime.clear();
			link2PersonTravelTime.clear();
			this.delegate.reset(iteration);
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			this.delegate.handleEvent(event);
		}

		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			this.delegate.handleEvent(event);
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {

			Id<Link> linkId = event.getLinkId();
			Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
			double enterTime = event.getTime();

			if(link2PersonEnterTime.containsKey(linkId)){
				Map<Id<Person>,Double> p2et = link2PersonEnterTime.get(linkId);
				p2et.put(personId, enterTime);
			} else {
				Map<Id<Person>,Double> p2et = new HashMap<>();
				p2et.put(personId, enterTime);
				link2PersonEnterTime.put(linkId, p2et);
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Id<Link> linkId = event.getLinkId();
			Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
			double leaveTime = event.getTime();

			if(! link2PersonEnterTime.containsKey(linkId) || ! link2PersonEnterTime.get(linkId).containsKey(personId) ) return;

			if(link2PersonTravelTime.containsKey(linkId)){
				Map<Id<Person>,List<Double>> p2tt = link2PersonTravelTime.get(linkId);

				List<Double> tts ;
				if(p2tt.containsKey(personId)){
					tts = link2PersonTravelTime.get(linkId).get(personId);
					tts.add( leaveTime - link2PersonEnterTime.get(linkId).get(personId));
				}else {
					tts = new ArrayList<>();
					tts.add( leaveTime - link2PersonEnterTime.get(linkId).get(personId));
				}
				p2tt.put(personId, tts);
				link2PersonEnterTime.get(linkId).remove(personId);

			} else {

				Map<Id<Person>,List<Double>> p2tt = new HashMap<>();
				List<Double> tts = new ArrayList<>();
				tts.add(leaveTime - link2PersonEnterTime.get(linkId).get(personId));
				p2tt.put(personId, tts);
				link2PersonTravelTime.put(linkId, p2tt);
				link2PersonEnterTime.get(linkId).remove(personId);
			}
		}

		public Map<Id<Link>, Map<Id<Person>, List<Double>>> getLink2PersonTravelTime() {
			return link2PersonTravelTime;
		}
	}
}