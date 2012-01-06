/* *********************************************************************** *
 * project: org.matsim.*
 * ScheduleChanger.java
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.finalDyn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author Ihab
 *
 */
public class ScheduleChanger {

	TransitScheduleFactory sf = new TransitScheduleFactoryImpl();

	private int numberOfBuses = 2;
	private String directoryExtIt;
	private String scheduleFile;
	private TransitSchedule schedule;
	TransitSchedule scheduleNew = sf.createTransitSchedule();

	Scenario scen = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
	Config config = scen.getConfig();
	
	public ScheduleChanger(String directoryExtIt, TransitSchedule transitSchedule, List<Id> list) {
		this.directoryExtIt = directoryExtIt;
		this.scheduleFile = this.directoryExtIt+"/scheduleFileModified.xml";
		this.schedule = transitSchedule;
	}
	
	public void changeTransit() {
		
		for (TransitLine line : schedule.getTransitLines().values()){
			TransitLine lineNew = sf.createTransitLine(line.getId());
			for (TransitRoute route : line.getRoutes().values()){
				TransitRoute routeNew = sf.createTransitRoute(route.getId(), route.getRoute(), route.getStops(), route.getTransportMode());
				for (Departure dep : route.getDepartures().values()){
					
					if (dep.getDepartureTime()>= 7*3600){
						if (dep.getVehicleId().equals(dep.getVehicleId()))
						routeNew.addDeparture(dep);
					}
					
//					if (dep.getDepartureTime()>= 7*3600){
//						if (dep.getVehicleId().equals(dep.getVehicleId()))
//						routeNew.addDeparture(dep);
//					}
				}
				lineNew.addRoute(routeNew);
			}
			scheduleNew.addTransitLine(lineNew);
		}
		
		for (TransitStopFacility stopFac : schedule.getFacilities().values()){
			scheduleNew.addStopFacility(stopFac);
		}
		
		writeScheduleFile();
		// schedule laden
		// ab bestimmten dep.times bestimmte busse entfernen...
		
	}
	
	public void writeScheduleFile() {
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(scheduleNew);
		scheduleWriter.write(this.scheduleFile);
	}

}
