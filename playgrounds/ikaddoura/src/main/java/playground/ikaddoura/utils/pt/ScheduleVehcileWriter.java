/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.utils.pt;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author ikaddoura
 *
 */
public class ScheduleVehcileWriter {
	
	private double headway_sec;
	private String networkFile;
	private String outputDirectory;
	// TODO: possibility of loading the schedule and only change departures depending on headway
	private String scheduleFile;
		
	public static void main(String[] args) {
		
		double headway = 600.;
		String netFile = "/Users/Ihab/Desktop/scheduleVehicleWriter/input/network.xml";
		String outputDir = "/Users/Ihab/Desktop/scheduleVehicleWriter/output/";
		String scheduleFile = "transitSchedule.xml";
		
		ScheduleVehcileWriter svw = new ScheduleVehcileWriter();
		svw.setHeadway_sec(headway);
		svw.setNetworkFile(netFile);
		svw.setOutputDirectory(outputDir);
		svw.setScheduleFile(scheduleFile);
		svw.run();
	}

	private void run() {
			
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(this.networkFile);
			
		ScheduleGenerator generator = new ScheduleGenerator();
		
		// set all constant parameters
		generator.setStartTime(4.0 * 3600);						// [sec]
		generator.setEndTime(24.0 * 3600);						// [sec]
		generator.setStopTime(15); 							// [sec]
		generator.setScheduleSpeed(8.3333333);					// [m/sec]
		// TODO: Warning if network freespeed < scheduled speed
		generator.setPausenzeit(10.0 * 60); 					// [sec]
		generator.setNetwork(scenario.getNetwork());
		generator.setTransitLineId(new IdImpl("busCorridorLine"));
		generator.setRouteId1(new IdImpl("west-east"));
		generator.setRouteId2(new IdImpl("east-west"));
		
		// create line, routes and transit stops
		generator.createLineRoutesStops();
		double umlaufzeit_sec = generator.getUmlaufzeit_sec();
		int numberOfBuses = (int) Math.ceil(umlaufzeit_sec / this.headway_sec);
		
		// create departures depending on headway
		generator.createVehicleIDs(numberOfBuses);
		generator.setDepartureIDs(this.headway_sec, numberOfBuses);
		
		// write scheduleFile
		generator.writeScheduleFile(this.outputDirectory, this.scheduleFile);
	}

	public void setHeadway_sec(double headway_sec) {
		this.headway_sec = headway_sec;
	}

	public void setNetworkFile(String networkFile) {
		this.networkFile = networkFile;
	}
	
	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}
	
	public void setScheduleFile(String scheduleFile) {
		this.scheduleFile = scheduleFile;
	}
	
}
