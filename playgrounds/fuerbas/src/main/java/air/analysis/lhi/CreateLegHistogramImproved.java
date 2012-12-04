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

import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;



/**
 * @author dgrether
 *
 */
public class CreateLegHistogramImproved {

	public static void main(String[] args) {
		String baseDirectory = "/media/data/work/repos/";
		String[] runs = {
				 "1811"
				};
		
		String vehiclesFile = "/home/dgrether/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_tuesday_flight_model_2_runways_airport_capacities_www_storage_restriction/flight_transit_vehicles.xml";
		Vehicles veh = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vreader = new VehicleReaderV1(veh);
		vreader.readFile(vehiclesFile);
		
//		String europeOnlyNetworkFile = "";
//		Config c = ConfigUtils.createConfig();
//		c.network().setInputFile(europeOnlyNetworkFile);
//		Network europeOnlyNetwork = ScenarioUtils.loadScenario(c).getNetwork();
		
		for (int i = 0; i < runs.length; i++){
			String rundir = baseDirectory + "runs-svn/run" + runs[i] + "/";
			String eventsFilename = rundir + "ITERS/it.0/" + runs[i] + ".0.events.xml.gz";
			String txtOutput = rundir + "ITERS/it.0/" + runs[i] + ".0.leg_histogram_improved.csv";
			String pngOutput = rundir + "ITERS/it.0/" + runs[i] + ".0.leg_histogram_improved_all.png";
			String pngOutputPt = rundir + "ITERS/it.0/" + runs[i] + ".0.leg_histogram_improved_pt.png";
			String txtOutputVeh = rundir + "ITERS/it.0/" + runs[i] + ".0.vehicle_histogram_improved.csv";
			String pngOutputVeh = rundir + "ITERS/it.0/" + runs[i] + ".0.vehicle_histogram_improved_all.png";

//			eventsFilename = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.events.xml.gz";
//			txtOutput = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.leg_histogram_improved.csv";
//			pngOutput = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.leg_histogram_improved.png";

			
			EventsFilterManager eventsManager = new EventsFilterManagerImpl();
//			GeospatialEventFilter filter = new GeospatialEventFilter(europeOnlyNetwork);
//			eventsManager.addFilter(filter);
			LegModeHistogramImproved handler = new LegModeHistogramImproved();
			VehicleSeatsModeHistogramImproved vehHisto = new VehicleSeatsModeHistogramImproved(veh);
			eventsManager.addHandler(handler);
			eventsManager.addHandler(vehHisto);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
			handler.write(txtOutput);
			handler.writeGraphic(pngOutput);
			vehHisto.write(txtOutputVeh);
			vehHisto.writeGraphic(pngOutputVeh);
//			handler.writeGraphic(pngOutputPt, "pt");
			
		}
	}

}
