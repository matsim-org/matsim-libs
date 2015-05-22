/* *********************************************************************** *
 * project: org.matsim.*
 * DgSeatsODTable
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
package air.scripts;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import air.analysis.DgSeatsODTableEventHandler;
import air.demand.DgDemandWriter;


public class DgSeatsODTable {

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
			OutputDirectoryHierarchy out = new OutputDirectoryHierarchy(
					rundir,
					runId,
							false ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists,
					false);
			String eventsFilename = out.getIterationFilename(it, "events.xml.gz");
			String seatsOdOutputFile = out.getOutputFilename("seats_by_od_pair.csv");
			
			EventsFilterManager eventsManager = new EventsFilterManagerImpl();
			DgSeatsODTableEventHandler seatsODTable = new DgSeatsODTableEventHandler(veh);
			eventsManager.addHandler(seatsODTable);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
			writer.writeFlightODRelations(seatsOdOutputFile, seatsODTable.getODSeats());
		}
		
		
	}

}
