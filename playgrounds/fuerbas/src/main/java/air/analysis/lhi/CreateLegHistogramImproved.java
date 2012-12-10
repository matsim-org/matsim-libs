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
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
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
		Tuple[] runs = {
				 new Tuple<String, Integer>("1836", 600),
				 new Tuple<String, Integer>("1837", 600),
				 new Tuple<String, Integer>("1838", 600),
				 new Tuple<String, Integer>("1839", 600),
				 new Tuple<String, Integer>("1840", 600),
				 new Tuple<String, Integer>("1841", 600)
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
			String runId = (String) runs[i].getFirst();
			Integer it = (Integer) runs[i].getSecond();
			String rundir = baseDirectory + "runs-svn/run" + runId + "/";
			OutputDirectoryHierarchy out = new OutputDirectoryHierarchy(rundir, runId, false, false);
			String eventsFilename =  out.getIterationFilename(it, "events.xml.gz");
			String pngOutputLegHisto = out.getIterationFilename(it, "leg_histogram_improved_de");
			String txtOutputLegHisto = pngOutputLegHisto + ".csv";
			String pngOutputSeatsHisto = out.getIterationFilename(it, "seats_histogram_improved_de");
			String txtOutputSeatsHisto = pngOutputSeatsHisto + ".csv";
			String pngOutputInVehHisto = out.getIterationFilename(it, "in_vehicle_histogram_improved_de");
			String txtOutputInVehHisto = pngOutputInVehHisto + ".csv";

//			eventsFilename = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.events.xml.gz";
//			txtOutput = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.leg_histogram_improved.csv";
//			pngOutput = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.leg_histogram_improved";

			
			EventsFilterManager eventsManager = new EventsFilterManagerImpl();
			EventFilter filter = new GeospatialLeavesEntersVehicleEventFilter(filterNetwork);
			eventsManager.addFilter(filter);
			
			InVehicleModeHistogramImproved inVehHisto = new InVehicleModeHistogramImproved();
			inVehHisto.reset(it);
			eventsManager.addHandler(inVehHisto);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
			CategoryHistogramWriter writer = new CategoryHistogramWriter();
			writer.setTitle("Passengers in Vehicle Histogram");
			writer.setyTitle("# Passengers");
			writer.writeCsv(inVehHisto.getCategoryHistogram(), txtOutputInVehHisto);
			writer.writeGraphics(inVehHisto.getCategoryHistogram(), pngOutputInVehHisto);

			
			
			eventsManager = new EventsFilterManagerImpl();
			filter = new GeospatialLinkVehicleEventFilter(filterNetwork);
			eventsManager.addFilter(filter);
			
			VehicleSeatsModeHistogramImproved vehHisto = new VehicleSeatsModeHistogramImproved(veh);
			vehHisto.reset(it);
			eventsManager.addHandler(vehHisto);
			reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
			writer = new CategoryHistogramWriter();
			writer.setTitle("Seats Histogram");
			writer.setyTitle("# seats");
			writer.writeCsv(vehHisto.getCategoryHistogram(), txtOutputSeatsHisto);
			writer.writeGraphics(vehHisto.getCategoryHistogram(), pngOutputSeatsHisto);


			eventsManager = new EventsFilterManagerImpl();
			GeospatialLinkDepartureArrivalEventFilter legHistoFilter = new GeospatialLinkDepartureArrivalEventFilter(filterNetwork);
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
	}

}
