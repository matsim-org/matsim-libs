/* *********************************************************************** *
 * project: org.matsim.*
 * CreateLegHistoImprovedNoPassengers
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.dgrether.analysis.categoryhistogram.CategoryHistogramWriter;
import playground.dgrether.analysis.flightlhi.GeospatialLinkDepartureArrivalStuckEventFilter;
import playground.dgrether.analysis.flightlhi.GeospatialLinkVehicleEventFilter;
import playground.dgrether.analysis.flightlhi.LegModeHistogramImproved;
import playground.dgrether.analysis.flightlhi.VehicleSeatsModeHistogramImproved;
import playground.dgrether.events.EventsFilterManagerImpl;


/**
 * @author dgrether
 *
 */
public class CreateLegHistoImprovedNoPassengers {

	private static final Logger log = Logger.getLogger(CreateLegHistoImprovedNoPassengers.class);
	
	private static String dataBaseDirectory = "/media/data/work/repos/";

	public static void main(String[] args) {
				String baseDirectory = "/media/data/work/repos/";
				Tuple[] runs = { 

										new Tuple<String, Integer>("1811", 0),
				};
				
				String vehiclesFile = "/home/dgrether/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_tuesday_flight_model_2_runways_3600vph/flight_transit_vehicles.xml";
				Vehicles veh = VehicleUtils.createVehiclesContainer();
				VehicleReaderV1 vreader = new VehicleReaderV1(veh);
				vreader.readFile(vehiclesFile);
				
				String filterNetworkFile = dataBaseDirectory
						+ "shared-svn/studies/countries/de/flight/"
						+ "dg_oag_tuesday_flight_model_germany_only_2_runways_airport_capacities_www_storage_restriction/air_network.xml";
				Config c = ConfigUtils.createConfig();
				c.network().setInputFile(filterNetworkFile);
				Network filterNetwork = ScenarioUtils.loadScenario(c).getNetwork();

				
				Map<String, List<Integer>> occupancyByRunId = new HashMap<String, List<Integer>>();

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
					String pngOutputLegHisto = out.getIterationFilename(it, "leg_histogram_improved_de");
					String txtOutputLegHisto = pngOutputLegHisto;
					String pngOutputSeatsHisto = out.getIterationFilename(it, "seats_histogram_improved_de");
					String txtOutputSeatsHisto = pngOutputSeatsHisto;

					// seats over time data
					EventsFilterManagerImpl eventsManager = new EventsFilterManagerImpl();
					GeospatialLinkVehicleEventFilter filter = new GeospatialLinkVehicleEventFilter(filterNetwork);
					eventsManager.addFilter(filter);

					VehicleSeatsModeHistogramImproved vehHisto = new VehicleSeatsModeHistogramImproved(veh);
					vehHisto.reset(it);
					eventsManager.addHandler(vehHisto);
					MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
					reader.readFile(eventsFilename);
					CategoryHistogramWriter writer = new CategoryHistogramWriter();
					writer.setTitle("Seats Histogram");
					writer.setyTitle("# seats");
					writer.writeCsv(vehHisto.getCategoryHistogram(), txtOutputSeatsHisto);
					writer.writeGraphics(vehHisto.getCategoryHistogram(), pngOutputSeatsHisto);

					// conventional leg histogram improved
					eventsManager = new EventsFilterManagerImpl();
					GeospatialLinkDepartureArrivalStuckEventFilter legHistoFilter = new GeospatialLinkDepartureArrivalStuckEventFilter(
							filterNetwork);
					eventsManager.addFilter(legHistoFilter);
					LegModeHistogramImproved handler = new LegModeHistogramImproved();
					handler.reset(it);
					eventsManager.addHandler(handler);
					reader = new MatsimEventsReader(eventsManager);
					reader.readFile(eventsFilename);
					CategoryHistogramWriter writer2 = new CategoryHistogramWriter();
					writer2.writeCsv(handler.getCategoryHistogram(), txtOutputLegHisto);
					writer2.writeGraphics(handler.getCategoryHistogram(), pngOutputLegHisto);

				}
				for (Entry<String, List<Integer>> e : occupancyByRunId.entrySet()) {
					double trips = e.getValue().get(0);
					double seats = e.getValue().get(1);
					log.info("Run " + e.getKey()+ " seats total: " + seats + " trips: " + trips + " occupancy: " + trips/seats*100.0);
				}

			}

}
