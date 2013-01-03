/* *********************************************************************** *
 * project: org.matsim.*
 * CreateSeatsODTable
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import air.demand.DgDemandWriter;
import air.demand.FlightODRelation;


/**
 * @author dgrether
 *
 */
public class CreateSeatsODTable implements
VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler{

	private SortedMap<String, SortedMap<String, FlightODRelation>> fromAirport2FlightOdRelMap;
	private Vehicles vehicles;
	private Map<Id, VehicleDepartsAtFacilityEvent> vehDepartsEventsByVehicleId = new HashMap<Id, VehicleDepartsAtFacilityEvent>();
	
	public CreateSeatsODTable(Vehicles vehicles){
		this.vehicles = vehicles;
		this.reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		this.fromAirport2FlightOdRelMap = new TreeMap<String, SortedMap<String, FlightODRelation>>();
		this.vehDepartsEventsByVehicleId.clear();
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehDepartsEventsByVehicleId.put(event.getVehicleId(), event);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		VehicleDepartsAtFacilityEvent departureEvent = this.vehDepartsEventsByVehicleId.get(event.getVehicleId());
		if (departureEvent != null){
			Vehicle vehicle = this.vehicles.getVehicles().get(event.getVehicleId());
			int seats = vehicle.getType().getCapacity().getSeats() - 1;
			this.addSeats(departureEvent.getFacilityId().toString(), event.getFacilityId().toString(), seats);
		}
	}
	
	private void addSeats(String from, String to, int seats){
		SortedMap<String, FlightODRelation> m = this.fromAirport2FlightOdRelMap.get(from);
		if (m == null){
			m = new TreeMap<String, FlightODRelation>();
			this.fromAirport2FlightOdRelMap.put(from, m);
		}
		FlightODRelation odRel = m.get(to);
		if (odRel == null){
			odRel = new FlightODRelation(from, to, (double) seats);
			m.put(to, odRel);
		}
		else {
			odRel.setNumberOfTrips(odRel.getNumberOfTrips() + seats);
		}
	}

	private SortedMap<String, SortedMap<String, FlightODRelation>> getODSeats() {
		return this.fromAirport2FlightOdRelMap;
	}

	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String baseDirectory = "/media/data/work/repos/";
		Tuple[] runs = { new Tuple<String, Integer>("1836", 600)
//				,
//				new Tuple<String, Integer>("1837", 600), new Tuple<String, Integer>("1838", 600),
//				new Tuple<String, Integer>("1839", 600), new Tuple<String, Integer>("1840", 600),
//				new Tuple<String, Integer>("1841", 600) };
		};
		String vehiclesFile = "/home/dgrether/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_tuesday_flight_model_2_runways_airport_capacities_www_storage_restriction/flight_transit_vehicles.xml";
		Vehicles veh = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vreader = new VehicleReaderV1(veh);
		vreader.readFile(vehiclesFile);
		DgDemandWriter writer = new DgDemandWriter();

		for (int i = 0; i < runs.length; i++) {
			String runId = (String) runs[i].getFirst();
			Integer it = (Integer) runs[i].getSecond();
			String rundir = baseDirectory + "runs-svn/run" + runId + "/";
			OutputDirectoryHierarchy out = new OutputDirectoryHierarchy(rundir, runId, false, false);
			String eventsFilename = out.getIterationFilename(it, "events.xml.gz");
			String seatsOdOutputFile = out.getOutputFilename("seats_by_od_pair.csv");
			
			EventsFilterManager eventsManager = new EventsFilterManagerImpl();
			CreateSeatsODTable seatsODTable = new CreateSeatsODTable(veh);
			eventsManager.addHandler(seatsODTable);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
			writer.writeFlightODRelations(seatsOdOutputFile, seatsODTable.getODSeats());
		}
		
		
	}




}
