/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package analysis.cartrips;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTrip;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTripsExtractor;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunCarTrips2CSV {
public static void main(String[] args) {
	
	String folder = "D:/runs-svn/vw_rufbus/";
	String run = "run121.10";	
	
	String eventsFile = folder+run+"/"+run+".output_events.xml.gz";
	String plansFile = folder+run+"/"+run+".output_plans.xml.gz";
	String networkFile = folder+run+"/"+run+".output_network.xml.gz";
	String outFileName = folder+run+"/"+run+".carTrips.csv";
	
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile(plansFile);
	new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
	
	
	EventsManager events = EventsUtils.createEventsManager();
		
	CarTripsExtractor carTripsExtractor = new CarTripsExtractor(scenario.getPopulation().getPersons().keySet(), scenario.getNetwork());
	events.addHandler(carTripsExtractor);
	new MatsimEventsReader(events).readFile(eventsFile);
	List<CarTrip> carTrips = carTripsExtractor.getTrips();
	writeTravelTimes(outFileName, carTrips);
	
	
}

static void writeTravelTimes(String filename, List<CarTrip> trips){
	BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
		bw.append("agent;departureTime;fromX;fromY;toX;toY;traveltimeActual");
		for (CarTrip trip : trips){
				bw.newLine();
				bw.append(trip.toString());
			
		}	
	
			bw.flush();
		bw.close();
	} catch (IOException e) {
		e.printStackTrace();
	}

}
}
