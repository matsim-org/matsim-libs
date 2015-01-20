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
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.singapore.utils.postgresql.CSVWriter;
import playground.pieter.singapore.utils.postgresql.PostgresType;
import playground.pieter.singapore.utils.postgresql.PostgresqlCSVWriter;
import playground.pieter.singapore.utils.postgresql.PostgresqlColumnDefinition;

public class RidershipTracking {

	class RidershipTracker {
		final FullDeparture fullDeparture;
		final Id driverId;
		final TransitRoute route;

		int ridership = 0;
		int stopsVisited = 0;
		final Id fullDepartureId;
		int lastRidership = 0;

		public RidershipTracker(FullDeparture fullDeparture, Id driverId) {
			super();
			this.fullDeparture = fullDeparture;
			this.fullDepartureId = fullDeparture.fullDepartureId;
			route = departureIdToRoute.get(fullDepartureId);
			this.driverId = driverId;
		}

		public void ridershipIncrement(PersonEntersVehicleEvent event) {
			if (!event.getPersonId().equals(driverId))
				ridership++;
		}

		public void ridershipDecrement(PersonLeavesVehicleEvent event) {
			if (!event.getPersonId().equals(driverId))
				ridership--;
		}

		public void incrementStopsVisited() {
			stopsVisited++;
		}
		
		public int getIncrement(){
			int temp = lastRidership;
			lastRidership=ridership;
			return(ridership-temp);
			
		}

	}

	class FullDeparture {
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
			fullDepartureId = Id.create(lineId.toString() + "_" + routeId.toString() + "_" + vehicleId.toString()
                    + "_" + departureId.toString(), Departure.class);
		}
	}

	class RidershipHandler implements  TransitDriverStartsEventHandler,VehicleArrivesAtFacilityEventHandler,VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {


		@Override
		public void reset(int iteration) {
			vehicletrackers = new HashMap<>();
		}


		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			RidershipTracker tracker = vehicletrackers.get(event.getAttributes().get("vehicle"));
			//skip car drivers
			if(tracker == null)	return;
			tracker.ridershipIncrement(event);
		}

		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			RidershipTracker tracker = vehicletrackers.get(event.getAttributes().get("vehicle"));
//			skip car drivers
			if(tracker == null)	return;
			tracker.ridershipDecrement( event);
		}

		@Override
		public void handleEvent(TransitDriverStartsEvent event) {
			RidershipTracker tracker = new RidershipTracker(new FullDeparture(event.getTransitLineId(), event.getTransitRouteId(),
					event.getVehicleId(), event.getDepartureId()), event.getDriverId());
			vehicletrackers.put(event.getAttributes().get("vehicleId"), tracker);
		}

		@Override
		public void handleEvent(VehicleArrivesAtFacilityEvent event) {
			RidershipTracker tracker = vehicletrackers.get(event.getAttributes().get("vehicle"));
			tracker.incrementStopsVisited();
			Object[] args = {
					tracker.fullDeparture.vehicleId,
					tracker.fullDeparture.routeId,
					tracker.fullDeparture.lineId,
					event.getFacilityId(),
					tracker.stopsVisited,
					event.getTime(),
					tracker.ridership ,
					tracker.getIncrement()};
			ridershipWriter.addLine(args);
		}

		@Override
		public void handleEvent(VehicleDepartsAtFacilityEvent event) {
			RidershipTracker tracker = vehicletrackers.get(event.getAttributes().get("vehicle"));
			Object[] args = {
					tracker.fullDeparture.vehicleId,
					tracker.fullDeparture.routeId,
					tracker.fullDeparture.lineId,
					event.getFacilityId(),
					tracker.stopsVisited,
					event.getTime(),
					tracker.ridership,
					tracker.getIncrement()};
			ridershipWriter.addLine(args);
		}
	}

	private final ScenarioImpl loRes;

    private Map<String, RidershipTracker> vehicletrackers;

	private final String eventsFile;
	private HashMap<Id, TransitRoute> departureIdToRoute;
	private final double maxSpeed = 80 / 3.6;

	private CSVWriter ridershipWriter;

	private RidershipTracking(String loResNetwork, String loResSchedule, String loResEvents) {
		loRes = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		loRes.getConfig().scenario().setUseTransit(true);
		new MatsimNetworkReader(loRes).readFile(loResNetwork);
		new TransitScheduleReader(loRes).readFile(loResSchedule);
        File outpath = new File(new File(loResEvents).getParent() + "/temp");
		outpath.mkdir();
		vehicletrackers = new HashMap<>();
		this.eventsFile = loResEvents;
	}

	void run() {

		identifyVehicleRoutes();
		readEvents();
		
	}

	private void readEvents() {
		RidershipHandler handler = new RidershipHandler();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(handler);
		EventsReaderXMLv1 eventReader = new EventsReaderXMLv1(eventsManager);
		eventReader.parse(eventsFile);

	}

	void initWriter( String suffix)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException,
			NoConnectionException {
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(new Date());
		// start with activities		String actTableName = "" + schemaName + ".matsim_ridership" + suffix;
		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("veh_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("route_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("line_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("stop_id", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("stop_no", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("time", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("ridership", PostgresType.INT));
		columns.add(new PostgresqlColumnDefinition("ridership_increment", PostgresType.INT));
		ridershipWriter = new CSVWriter("RIDERSHIP", "./",  1000, columns);
		ridershipWriter.addComment(String.format("MATSim ridership from events file %s, created on %s.", eventsFile,
				formattedDate));
	}

	private void identifyVehicleRoutes() {
		departureIdToRoute = new HashMap<>();
		Collection<TransitLine> lines = loRes.getTransitSchedule().getTransitLines().values();
		for (TransitLine line : lines) {
			Collection<TransitRoute> routes = line.getRoutes().values();
			for (TransitRoute route : routes) {
				Collection<Departure> departures = route.getDepartures().values();
				for (Departure departure : departures) {
					departureIdToRoute.put(new FullDeparture(line.getId(), route.getId(), departure.getVehicleId(),
							departure.getId()).fullDepartureId, route);
				}
			}
		}
	}

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		String loResNetwork = args[0];
		String loResSchedule = args[1];
		String loResEvents = args[2];
		RidershipTracking xfer = new RidershipTracking(loResNetwork, loResSchedule, loResEvents);

		xfer.initWriter( args[3]);
		xfer.run();
	}

}
