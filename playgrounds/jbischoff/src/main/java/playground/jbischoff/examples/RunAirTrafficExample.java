/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.examples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *	Example code to run the MATSim Air traffic scenario. 
 *	Note that there is no demand attached to this.
 */
public class RunAirTrafficExample {

	public static void main(String[] args) {
		String INPUTDIR = "../../../shared-svn/studies/countries/eu/flight/sf_oag_flight_model/public/";
		Config config = ConfigUtils.createConfig();
		
		config.network().setInputFile(INPUTDIR+"air_network.xml");
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(INPUTDIR+"flight_transit_schedule.xml");
		config.transit().setVehiclesFile(INPUTDIR+"flight_transit_vehicles.xml");
		
		config.controler().setOutputDirectory(INPUTDIR+"output");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.vspExperimental().setWritingOutputEvents(true);
		
		config.qsim().setEndTime(40*3600);
		
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario); 
		controler.run();
		
	}

}
