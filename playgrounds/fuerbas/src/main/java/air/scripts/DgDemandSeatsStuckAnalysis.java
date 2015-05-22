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
package air.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import air.analysis.DgSeatsODTableEventHandler;
import air.analysis.stuck.BoardingDeniedStuckEvaluator;
import air.analysis.stuck.BoardingDeniedStuckEvaluator.BoardingDeniedStuck;
import air.analysis.stuck.CollectBoardingDeniedStuckEventHandler;
import air.demand.DgDemandReader;
import air.demand.DgDemandUtils;
import air.demand.FlightODRelation;


/**
 * Writes a table containing number of available seats in simulation and demand for each o-d pair
 * @author dgrether
 *
 */
public class DgDemandSeatsStuckAnalysis {

	public static void main(String[] args) throws IOException {
		String baseDirectory = "/media/data/work/repos/";
		String odDemand22 = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_tabelle_2.2.2.csv";
		Tuple[] runs = { new Tuple<String, Integer>("1854", 600)
//				,
//				new Tuple<String, Integer>("1837", 600), new Tuple<String, Integer>("1838", 600),
//				new Tuple<String, Integer>("1839", 600), new Tuple<String, Integer>("1840", 600),
//				new Tuple<String, Integer>("1841", 600) };
		};
		String vehiclesFile = "/home/dgrether/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_tuesday_flight_model_2_runways_3600vph_storage_restriction/flight_transit_vehicles.xml";
		Vehicles veh = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vreader = new VehicleReaderV1(veh);
		vreader.readFile(vehiclesFile);

		
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
			String populationFilename = out.getOutputFilename("output_plans.xml.gz");
			Config config = ConfigUtils.createConfig();
			config.plans().setInputFile(populationFilename);
			Scenario scenario = ScenarioUtils.createScenario(config);
			MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
			popReader.readFile(populationFilename);
			String demandVsSeatsFile = out.getOutputFilename("demand_seats_stuck_by_od_pair.csv");
				
			
			EventsManager eventsManager = EventsUtils.createEventsManager();
			DgSeatsODTableEventHandler seatsODTable = new DgSeatsODTableEventHandler(veh);
			eventsManager.addHandler(seatsODTable);
			CollectBoardingDeniedStuckEventHandler stuckHandler = new CollectBoardingDeniedStuckEventHandler();
			eventsManager.addHandler(stuckHandler);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
			BoardingDeniedStuckEvaluator stuckEvaluator = new BoardingDeniedStuckEvaluator(stuckHandler.getBoardingDeniedStuckEventsByPersonId(), scenario.getPopulation());
			
			List<FlightODRelation> demandList = new DgDemandReader().readFile(odDemand22);
			DgDemandUtils.convertToDailyDemand(demandList);
			BufferedWriter bw = IOUtils.getBufferedWriter(demandVsSeatsFile);
			bw.write("From\tTo\tDemand\tSeats\tSeats-Demand\tStuck");
			bw.newLine();
			for (FlightODRelation rel : demandList) {
				bw.write(rel.getFromAirportCode());
				bw.write("\t");
				bw.write(rel.getToAirportCode());
				bw.write("\t");
				Double trips = rel.getNumberOfTrips();
				if (trips != null){
					bw.write(Double.toString(Math.floor(trips)));
				}
				else {
					bw.write("-");
				}
				bw.write("\t");
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
				bw.write("\t");
				if (trips != null && seatsOd != null) {
					long diff = Math.round(seatsOd.getNumberOfTrips()) - Math.round(trips);
					bw.write(Long.toString(diff));
				}
				else {
					bw.write("-");
				}
				bw.write("\t");
				BoardingDeniedStuck odStuck = stuckEvaluator.getOdStuckCountMap().get(new Tuple<Id<Link>, Id<Link>>(Id.create(rel.getFromAirportCode(), Link.class), Id.create(rel.getToAirportCode(), Link.class)));
				if (odStuck != null) {
					bw.write(Integer.toString(odStuck.getStuckAndBoardingDenied() + odStuck.getStuckNoBoardingDenied()));
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
