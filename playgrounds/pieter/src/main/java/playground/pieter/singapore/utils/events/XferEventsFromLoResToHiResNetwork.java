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

package playground.pieter.singapore.utils.events;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.pieter.events.EventsMergeSort;
import playground.pieter.singapore.utils.events.listeners.IncrementEventWriterXML;

public class XferEventsFromLoResToHiResNetwork{
	class FullDeparture{
		final Id fullDepartureId;
		final Id lineId;
		final Id routeId;
		final Id vehicleId;
		final Id departureId;
		public FullDeparture(Id lineId, Id routeId, Id vehicleId, Id departureId) {
			super();
			this.lineId = lineId;
			this.routeId = routeId;
			this.vehicleId = vehicleId;
			this.departureId = departureId;
			fullDepartureId = Id.create(lineId.toString() + "_" + routeId.toString() + "_" + vehicleId.toString() + "_" + departureId.toString(), Departure.class);
		}
	}
	class EventSplitter implements BasicEventHandler {
		private final IncrementEventWriterXML nonLinkEventWriter;
		private final EventWriterXML linkEventWriter;

		int eventCounter = 0;
		final EventsManager nonLinkEvents = EventsUtils.createEventsManager();
		private final Set<String> filteredEvents;
		private final EventsManager linkEvents = EventsUtils.createEventsManager();
		{
			filteredEvents = new HashSet<>();
			filteredEvents.add(Wait2LinkEvent.EVENT_TYPE);
			filteredEvents.add(LinkEnterEvent.EVENT_TYPE);
			filteredEvents.add(LinkLeaveEvent.EVENT_TYPE);
			filteredEvents.add(VehicleArrivesAtFacilityEvent.EVENT_TYPE);
			filteredEvents.add(VehicleDepartsAtFacilityEvent.EVENT_TYPE);
			filteredEvents.add(TransitDriverStartsEvent.EVENT_TYPE);
		}

		public EventSplitter() {
			nonLinkEventWriter = new IncrementEventWriterXML(outpath.getPath() + "/non_link_events.xml");
			File linktemp = new File(outpath.getPath()+"/linktemp");
			linktemp.mkdir();
			linkEventWriter = new EventWriterXML(linktemp.getPath() + "/link_events.xml");
			nonLinkEvents.addHandler(nonLinkEventWriter);
			linkEvents.addHandler(linkEventWriter);
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleEvent(Event event) {
			if (filteredEvents.contains(event.getEventType())) {
				LinkedList<Event> eventList = null;

				eventList = vehicleLinkEvents.get(event.getAttributes().get("vehicle"));
				if (eventList == null) {
					// it's a transit driver starts event, so has a different
					// attribute for vehid
					eventList = new LinkedList<>();
					vehicleLinkEvents.put(event.getAttributes().get("vehicleId"), eventList);
				}
				// arrive at facility events are merely there to punctuate the
				// list of link leave, link enter events
				if (event.getEventType().equals(VehicleArrivesAtFacilityEvent.EVENT_TYPE)
						|| event.getEventType().equals(TransitDriverStartsEvent.EVENT_TYPE)
						|| event.getEventType().equals(VehicleDepartsAtFacilityEvent.EVENT_TYPE)) {
					eventList.add(event);
					linkEvents.processEvent(event);
					nonLinkEvents.processEvent(event);
				}

			} else {
				nonLinkEvents.processEvent(event);
			}

		}

		public void close() {
			nonLinkEventWriter.closeFile();
			linkEventWriter.closeFile();
		}

	}
	
	private class VehicleLinkEventLoader implements BasicEventHandler {

		@Override
		public void reset(int iteration) {
		}

		@Override
		public void handleEvent(Event event) {
			LinkedList<Event> eventList = null;

			eventList = vehicleLinkEvents.get(event.getAttributes().get("vehicle"));
			if (eventList == null) {
				// it's a transit driver starts event, so has a different
				// attribute for vehid
				eventList = new LinkedList<>();
				vehicleLinkEvents.put(event.getAttributes().get("vehicleId"), eventList);
			}
			// arrive at facility events are merely there to punctuate the
			// list of link leave, link enter events
			if (event.getEventType().equals(VehicleArrivesAtFacilityEvent.EVENT_TYPE)
					|| event.getEventType().equals(TransitDriverStartsEvent.EVENT_TYPE)
					|| event.getEventType().equals(VehicleDepartsAtFacilityEvent.EVENT_TYPE)) {
				eventList.add(event);
			}
		}
	}

	private final MutableScenario loRes;
	private final MutableScenario hiRes;
	
	private final File outpath;
	private final Map<String, LinkedList<Event>> vehicleLinkEvents;
	
	private final String loResEvents;
    private HashMap<Id, TransitRoute> hiResDepartureIdToRouteIds;
    private File linkEventPath;

	private XferEventsFromLoResToHiResNetwork(String loResNetwork, String hiResNetwork, String loResSchedule,
                                              String hiResSchedule, String loResEvents) {
		loRes = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		hiRes = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		loRes.getConfig().transit().setUseTransit(true);
		hiRes.getConfig().transit().setUseTransit(true);
		new MatsimNetworkReader(loRes).readFile(loResNetwork);
		new MatsimNetworkReader(hiRes).readFile(hiResNetwork);
		new TransitScheduleReader(loRes).readFile(loResSchedule);
		new TransitScheduleReader(hiRes).readFile(hiResSchedule);
		outpath = new File(new File(loResEvents).getParent() + "/temp");
		outpath.mkdir();
		vehicleLinkEvents = new HashMap<>();
		this.loResEvents = loResEvents;
	}

	void run(boolean deserialize) {
		if(!deserialize){
			splitEvents();
				
		}else{
			vehicleLinkEventLoader();
		}
		identifyVehicleRoutes();
		writeHiResVehicleEvents();
		mergeSortVehicleEvents();
		mergeSortOriginalEventsAndVehicleEvents();
	}





	private void vehicleLinkEventLoader() {
		// TODO Auto-generated method stub
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new VehicleLinkEventLoader());
		
		EventsReaderXMLv1 eventReader = new EventsReaderXMLv1(eventsManager);
		eventReader.parse(outpath.getPath()+"/linktemp/link_events.xml");
		
	}



	private void splitEvents() {
		EventSplitter eventSplitter = new EventSplitter();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(eventSplitter);
		EventsReaderXMLv1 eventReader = new EventsReaderXMLv1(eventsManager);
		eventReader.parse(loResEvents);
		eventSplitter.close();	

	}

	private void identifyVehicleRoutes() {
        HashMap<Id, TransitRoute> loResDepartureIdToRouteIds = new HashMap<>();
		Collection<TransitLine> lines = loRes.getTransitSchedule().getTransitLines().values();
		for (TransitLine line : lines) {
			Collection<TransitRoute> routes = line.getRoutes().values();
			for (TransitRoute route : routes) {
				Collection<Departure> departures = route.getDepartures().values();
				for (Departure departure : departures) {
					loResDepartureIdToRouteIds.put(new FullDeparture(line.getId(), route.getId(), departure.getVehicleId(), departure.getId()).fullDepartureId, route);
				}
			}
		}
		hiResDepartureIdToRouteIds = new HashMap<>();
		lines = hiRes.getTransitSchedule().getTransitLines().values();
		for (TransitLine line : lines) {
			Collection<TransitRoute> routes = line.getRoutes().values();
			for (TransitRoute route : routes) {
				Collection<Departure> departures = route.getDepartures().values();
				for (Departure departure : departures) {
					hiResDepartureIdToRouteIds.put(new FullDeparture(line.getId(), route.getId(), departure.getVehicleId(), departure.getId()).fullDepartureId, route);
				}
			}
		}
	}

	private void writeHiResVehicleEvents() {
		linkEventPath = new File(outpath + "/links");
		linkEventPath.mkdir();
		for (String vehIdString : this.vehicleLinkEvents.keySet()) {
			System.out.println("handling vehicle: "+vehIdString);
			Id vehId = Id.create(vehIdString, TransitVehicle.class);
			LinkedList<Event> loResEvents = vehicleLinkEvents.get(vehId.toString());
			LinkedList<Event> hiResEvents = new LinkedList<>();
			Map<Id<Link>, ? extends Link> links = hiRes.getNetwork().getLinks();
			// the first event will always contain the departure info
			TransitDriverStartsEvent tDSE = (TransitDriverStartsEvent) loResEvents.getFirst();
			Id driverId = tDSE.getDriverId();

			loResEvents.removeFirst();
			Id departureId = new FullDeparture(tDSE.getTransitLineId(), tDSE.getTransitRouteId(), tDSE.getVehicleId(), tDSE.getDepartureId()).fullDepartureId;
			TransitRouteImpl hiResRoute = (TransitRouteImpl) this.hiResDepartureIdToRouteIds.get(departureId);
			LinkedList<TransitRouteStop> stops = new LinkedList<>();
			stops.addAll(hiResRoute.getStops());
			NetworkRoute route = hiResRoute.getRoute();
			Iterator<TransitRouteStop> stopIterator = stops.iterator();
			TransitRouteStop firstStop = stopIterator.next();
			Id departureLinkId = firstStop.getStopFacility().getLinkId();
			Event wait2Link = new Wait2LinkEvent(tDSE.getTime() + 0.004, driverId, departureLinkId, vehId, PtConstants.NETWORK_MODE, 1.0);
			hiResEvents.addLast(wait2Link);
			Id fromLinkId = departureLinkId;
			Iterator<Event> eventIterator = loResEvents.iterator();
			// not interested in the first arrival event
			eventIterator.next();
			VehicleDepartsAtFacilityEvent departure = (VehicleDepartsAtFacilityEvent) eventIterator.next();
			while (eventIterator.hasNext()) {
				VehicleArrivesAtFacilityEvent arrival = (VehicleArrivesAtFacilityEvent) eventIterator.next();

				Id toLinkId = stopIterator.next().getStopFacility().getLinkId();
				NetworkRoute subRoute = route.getSubRoute(fromLinkId, toLinkId);
				LinkedList<Double> linkTravelTimes = new LinkedList<>();
				double totalExpectedtravelTime = 0;
                double maxSpeed = 80 / 3.6;
                for (Id linkId : subRoute.getLinkIds()) {
					Link link = links.get(linkId);
					linkTravelTimes.add(link.getLength() / Math.min(link.getFreespeed(), maxSpeed));
					totalExpectedtravelTime += linkTravelTimes.getLast();
				}
				Link toLink = links.get(toLinkId);
				totalExpectedtravelTime += toLink.getLength() / Math.min(toLink.getFreespeed(), maxSpeed);

				double availableTime = arrival.getTime() - departure.getTime();
				double lastTime = departure.getTime() + 1;
				Event linkLeave = new LinkLeaveEvent(lastTime += 0.001, driverId, fromLinkId, vehId);
				Event linkEnter = null;

				hiResEvents.addLast(linkLeave);
				List<Id<Link>> linkIds = subRoute.getLinkIds();
				for (int i = 0; i < linkIds.size(); i++) {
					linkEnter = new LinkEnterEvent(lastTime += 0.001, driverId, linkIds.get(i), vehId);
					linkLeave = new LinkLeaveEvent(
							lastTime += (availableTime * linkTravelTimes.get(i) / totalExpectedtravelTime), driverId,
							linkIds.get(i), vehId);
					hiResEvents.addLast(linkEnter);
					hiResEvents.addLast(linkLeave);
				}
				linkEnter = new LinkEnterEvent(lastTime += 0.001, driverId, toLinkId, vehId);
				hiResEvents.addLast(linkEnter);
				if(eventIterator.hasNext()){
					departure = (VehicleDepartsAtFacilityEvent) eventIterator.next();					
				}
				fromLinkId = toLinkId;
			}

			EventWriterXML linkEventWriter = new EventWriterXML(linkEventPath + "/" + vehIdString + ".xml");
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(linkEventWriter);
			for (Event event : hiResEvents) {
				events.processEvent(event);
			}
			linkEventWriter.closeFile();
		}

	}

	private void mergeSortVehicleEvents() {
		EventsMergeSort ems = new EventsMergeSort(outpath.getPath(), linkEventPath.getPath());
		try {
			ems.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File outfile = new File(outpath + "/OUT_events.xml.gz");
		outfile.renameTo(new File(outpath + "/veh_events.xml.gz"));
	}

	private void mergeSortOriginalEventsAndVehicleEvents() {
		EventsMergeSort ems = new EventsMergeSort(outpath.getPath(), outpath.getPath());
		try {
			ems.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public File getOutpath() {
		return outpath;
	}

	public static void main(String[] args) {
		String loResNetwork = args[0];
		String hiResNetwork = args[1];
		String loResSchedule = args[2];
		String hiResSchedule = args[3];
		String loResEvents = args[4];
		XferEventsFromLoResToHiResNetwork xfer = new XferEventsFromLoResToHiResNetwork(loResNetwork, hiResNetwork,
				loResSchedule, hiResSchedule, loResEvents);
		
		xfer.run(Boolean.parseBoolean(args[5]));
	}

}
