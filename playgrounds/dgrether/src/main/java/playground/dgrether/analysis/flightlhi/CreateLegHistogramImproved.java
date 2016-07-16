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
package playground.dgrether.analysis.flightlhi;

import java.util.ArrayList;
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
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.events.filters.EventFilter;

/**
 * @author dgrether
 * 
 */
public class CreateLegHistogramImproved {

	private static final Logger log = Logger.getLogger(CreateLegHistogramImproved.class);

	private static String dataBaseDirectory = "/media/data/work/repos/";

	public static void main(String[] args) {
		String baseDirectory = "/media/data/work/repos/";
		Tuple[] runs = { 

//								new Tuple<String, Integer>("1811", 0),
//				new Tuple<String, Integer>("1836", 600),
//				new Tuple<String, Integer>("1837", 600), 
//				new Tuple<String, Integer>("1838", 600),
//				new Tuple<String, Integer>("1839", 600), 
//				new Tuple<String, Integer>("1840", 600),
//				new Tuple<String, Integer>("1841", 600) 
//				new Tuple<String, Integer>("1848", 600),
//				new Tuple<String, Integer>("1849", 600), 
//				new Tuple<String, Integer>("1850", 600),
//				new Tuple<String, Integer>("1851", 600), 
//				new Tuple<String, Integer>("1852", 600),
//				new Tuple<String, Integer>("1853", 600) 

//			new Tuple<String, Integer>("1854", 1),
//			new Tuple<String, Integer>("1855", 1),
//			new Tuple<String, Integer>("1856", 1),
//			new Tuple<String, Integer>("1857", 1),
//			new Tuple<String, Integer>("1858", 1),
//			new Tuple<String, Integer>("1859", 1),
//			
//			new Tuple<String, Integer>("1860", 1),
//			new Tuple<String, Integer>("1861", 1),
//			new Tuple<String, Integer>("1862", 1),
//			new Tuple<String, Integer>("1863", 1),
//			new Tuple<String, Integer>("1864", 1)
				
//			new Tuple<String, Integer>("1865", 600),
//			new Tuple<String, Integer>("1866", 600),
//			new Tuple<String, Integer>("1867", 600),
//			new Tuple<String, Integer>("1868", 600),
//			new Tuple<String, Integer>("1869", 600),
//			new Tuple<String, Integer>("1870", 600),

//			new Tuple<String, Integer>("1876", 600), //2009 data worst plan selector
//			new Tuple<String, Integer>("1877", 600),
//			new Tuple<String, Integer>("1878", 600),
//			new Tuple<String, Integer>("1879", 600),
//			new Tuple<String, Integer>("1880", 600),
//			new Tuple<String, Integer>("1881", 600),


				
//			new Tuple<String, Integer>("1882", 600), //path size mode choice 2011 daten
//			new Tuple<String, Integer>("1883", 600),
//			new Tuple<String, Integer>("1884", 600),
//			new Tuple<String, Integer>("1885", 600),
//			new Tuple<String, Integer>("1886", 600),

//				new Tuple<String, Integer>("1887", 600), //2009 data worst plan selector
//				new Tuple<String, Integer>("1888", 600),
//				new Tuple<String, Integer>("1889", 600),
//				new Tuple<String, Integer>("1890", 600),
//				new Tuple<String, Integer>("1891", 600),
//				new Tuple<String, Integer>("1892", 600)

				
//			new Tuple<String, Integer>("1893", 600), //path size mode choice 2009 daten
//			new Tuple<String, Integer>("1894", 600),
//			new Tuple<String, Integer>("1895", 600),
//			new Tuple<String, Integer>("1896", 600),
//			new Tuple<String, Integer>("1897", 600),

			new Tuple<String, Integer>("1903", 600), //mode choice 2009 daten, no psl
			new Tuple<String, Integer>("1904", 600),
			new Tuple<String, Integer>("1905", 600),
			new Tuple<String, Integer>("1906", 600),
			new Tuple<String, Integer>("1907", 600),

				
		};

		String vehiclesFile = "/home/dgrether/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_tuesday_flight_model_2_runways_airport_capacities_www_storage_restriction/flight_transit_vehicles.xml";
		Vehicles veh = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vreader = new VehicleReaderV1(veh);
		vreader.readFile(vehiclesFile);

		String filterNetworkFile = dataBaseDirectory
				+ "shared-svn/studies/countries/de/flight/"
				+ "dg_oag_tuesday_flight_model_germany_only_2_runways_airport_capacities_www_storage_restriction/air_network.xml";
		// String filterNetworkFile = dataBaseDirectory + "shared-svn/studies/countries/eu/flight/" +
		// "dg_oag_tuesday_flight_model_europe_only_2_runways_airport_capacities_www_storage_restriction/air_network.xml";

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
			String txtOutputSeatsHisto = pngOutputSeatsHisto ;
			String pngOutputInVehHisto = out.getIterationFilename(it, "in_vehicle_histogram_improved_de");
			String txtOutputInVehHisto = pngOutputInVehHisto;

			// Persons in vehicle over time data
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
			List<Integer> results = new ArrayList<Integer>();
			results.add(inVehHisto.getTotalTrips());
			occupancyByRunId.put(runId, results);

			// seats over time data
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
			results.add(vehHisto.getTotalSeats());

			// conventional leg histogram improved
			eventsManager = new EventsFilterManagerImpl();
//			GeospatialLinkDepartureArrivalStuckEventFilter legHistoFilter = new GeospatialLinkDepartureArrivalStuckEventFilter(
//					filterNetwork);
//			eventsManager.addFilter(legHistoFilter);
//			eventsManager.addFilter(new NotCarModeLinkEventFilter());
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
