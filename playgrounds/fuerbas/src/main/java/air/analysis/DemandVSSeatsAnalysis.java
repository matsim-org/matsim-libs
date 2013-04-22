/* *********************************************************************** *
 * project: org.matsim.*
 * DemandVSSeatsAnalysis
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
package air.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import air.demand.DgDemandReader;
import air.demand.DgDemandUtils;
import air.demand.FlightODRelation;


/**
 * @author dgrether
 *
 */
public class DemandVSSeatsAnalysis {

	public static void main(String[] args) throws IOException {
		String baseDirectory = "/media/data/work/repos/";
		String odDemand22 = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_tabelle_2.2.2.csv";
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

		for (int i = 0; i < runs.length; i++) {
			String runId = (String) runs[i].getFirst();
			Integer it = (Integer) runs[i].getSecond();
			String rundir = baseDirectory + "runs-svn/run" + runId + "/";
			OutputDirectoryHierarchy out = new OutputDirectoryHierarchy(rundir, runId, false, false);
			String eventsFilename = out.getIterationFilename(it, "events.xml.gz");
			String demandVsSeatsFile = out.getOutputFilename("demand_vs_seats_by_od_pair.csv");
			
			EventsFilterManager eventsManager = new EventsFilterManagerImpl();
			CreateSeatsODTable seatsODTable = new CreateSeatsODTable(veh);
			eventsManager.addHandler(seatsODTable);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
			
			List<FlightODRelation> demandList = new DgDemandReader().readFile(odDemand22);
			DgDemandUtils.convertToDailyDemand(demandList);
			BufferedWriter bw = IOUtils.getBufferedWriter(demandVsSeatsFile);
			bw.write("From;To;Demand;Seats");
			bw.newLine();
			for (FlightODRelation rel : demandList) {
				bw.write(rel.getFromAirportCode());
				bw.write(";");
				bw.write(rel.getToAirportCode());
				bw.write(";");
				Double trips = rel.getNumberOfTrips();
				if (trips != null){
					bw.write(Double.toString(Math.floor(trips)));
				}
				else {
					bw.write("-");
				}
				bw.write(";");
				SortedMap<String, FlightODRelation> toMap = seatsODTable.getODSeats().get(rel.getFromAirportCode());
				FlightODRelation seatsOd = null;
				if (toMap != null){
					seatsOd = toMap.get(rel.getToAirportCode());
				}
				if (seatsOd != null) {
					bw.write(Double.toString( seatsOd.getNumberOfTrips()));
				}
				else {
					bw.write("-");
				}
				bw.newLine();
			}
			bw.close();
		}

	}

}
