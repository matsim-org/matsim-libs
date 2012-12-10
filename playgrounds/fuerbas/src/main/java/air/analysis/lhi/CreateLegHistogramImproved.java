/* *********************************************************************** *
 * project: org.matsim.*
 * CreateLegHistogramImproved
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
package air.analysis.lhi;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.filters.EventFilter;
import air.analysis.categoryhistogram.CategoryHistogramWriter;



/**
 * @author dgrether
 *
 */
public class CreateLegHistogramImproved {
	private static String dataBaseDirectory = "/media/data/work/repos/";
	
	public static void main(String[] args) {
		String baseDirectory = "/media/data/work/repos/";
		String[] runs = {
				 "1811"
				};
		
		String vehiclesFile = "/home/dgrether/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_tuesday_flight_model_2_runways_airport_capacities_www_storage_restriction/flight_transit_vehicles.xml";
		Vehicles veh = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vreader = new VehicleReaderV1(veh);
		vreader.readFile(vehiclesFile);
		
		String filterNetworkFile = dataBaseDirectory + "shared-svn/studies/countries/de/flight/" + "dg_oag_tuesday_flight_model_germany_only_2_runways_airport_capacities_www_storage_restriction/air_network.xml";
//		String filterNetworkFile = dataBaseDirectory + "shared-svn/studies/countries/eu/flight/" + "dg_oag_tuesday_flight_model_europe_only_2_runways_airport_capacities_www_storage_restriction/air_network.xml";

		Config c = ConfigUtils.createConfig();
		c.network().setInputFile(filterNetworkFile);
		Network filterNetwork = ScenarioUtils.loadScenario(c).getNetwork();
		
		for (int i = 0; i < runs.length; i++){
			String rundir = baseDirectory + "runs-svn/run" + runs[i] + "/";
			String eventsFilename = rundir + "ITERS/it.0/" + runs[i] + ".0.events.xml.gz";
			String txtOutput = rundir + "ITERS/it.0/" + runs[i] + ".0.leg_histogram_improved.csv";
			String pngOutput = rundir + "ITERS/it.0/" + runs[i] + ".0.leg_histogram_improved_all.png";
			String pngOutputPt = rundir + "ITERS/it.0/" + runs[i] + ".0.leg_histogram_improved_pt";
			String txtOutputVeh = rundir + "ITERS/it.0/" + runs[i] + ".0.vehicle_histogram_improved_all_de.csv";
			String pngOutputVeh = rundir + "ITERS/it.0/" + runs[i] + ".0.vehicle_histogram_improved_all_de";

//			eventsFilename = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.events.xml.gz";
//			txtOutput = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.leg_histogram_improved.csv";
//			pngOutput = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.leg_histogram_improved";

			
			EventsFilterManager eventsManager = new EventsFilterManagerImpl();
			EventFilter filter = new GeospatialLinkVehicleEventFilter(filterNetwork);
//			eventsManager.addFilter(filter);
			
			VehicleSeatsModeHistogramImproved vehHisto = new VehicleSeatsModeHistogramImproved(veh);
			eventsManager.addHandler(vehHisto);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
			CategoryHistogramWriter writer = new CategoryHistogramWriter();
			writer.setTitle("Seats Histogram");
			writer.setyTitle("# seats");
			writer.writeCsv(vehHisto.getCategoryHistogram(), txtOutput);
			writer.writeGraphics(vehHisto.getCategoryHistogram(), pngOutput);


//			eventsManager = new EventsFilterManagerImpl();
//			LegModeHistogramImproved handler = new LegModeHistogramImproved();
//			eventsManager.addHandler(handler);
//			reader = new MatsimEventsReader(eventsManager);
//			reader.readFile(eventsFilename);
//			HistogramWriter writer2 = new HistogramWriter();
//			writer2.writeCsv(handler.getCategoryHistogram(), txtOutput);
//			writer2.writeGraphics(handler.getCategoryHistogram(), pngOutput);

		}
	}

}
