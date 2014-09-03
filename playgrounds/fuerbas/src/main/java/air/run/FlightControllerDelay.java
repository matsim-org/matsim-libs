/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightDelayController
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
package air.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

/**
 * @author dgrether
 * 
 */
public class FlightControllerDelay {
	
	private static final Logger log = Logger.getLogger(FlightControllerDelay.class);
	
	
	
	private static void modifySchedule(TransitSchedule schedule, double delayProbability) {
		Random random = MatsimRandom.getLocalInstance();
		Random timeRandom = MatsimRandom.getLocalInstance();

		TransitScheduleFactory factory = schedule.getFactory();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				List<Departure> departures = new ArrayList<Departure>();
				departures.addAll(route.getDepartures().values());
				for (Departure dep : departures) {
					double r = random.nextDouble();
					if (r <= delayProbability) {
						double time = dep.getDepartureTime();
						route.removeDeparture(dep);
						log.info("Original departure time: " + Time.writeTime(time) + " for departure: " + dep.getId());
						double r2 = timeRandom.nextGaussian();
						time = time + ((r2 * 27.6/2 * 60.0) + (27.6 * 60.0));
						Departure dep2 = factory.createDeparture(Id.create(dep.getVehicleId(), Departure.class), time);
						dep2.setVehicleId(dep.getVehicleId());
						route.addDeparture(dep2);
						log.info("Shifted deprature time: " + Time.writeTime(time) + " using r2: " + r2);
						log.info("");
					}
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String configFilename = args[0];
//		String configFilename = "/media/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_flight_model_2_runways_3600vph_one_line/air_config.xml";
		Config config = ConfigUtils.loadConfig(configFilename);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);

		TransitSchedule schedule = scenario.getTransitSchedule();
		modifySchedule(schedule, 0.371); 
//		modifySchedule(schedule, 1.0); 
		FlightController controler = new FlightController();
		controler.run(scenario);

	}

}
