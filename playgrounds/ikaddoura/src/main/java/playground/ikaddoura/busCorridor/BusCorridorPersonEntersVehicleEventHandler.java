/* *********************************************************************** *
 * project: org.matsim.*
 * BusCorridorEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.busCorridor;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

/**
 * @author Ihab
 *
 */

public class BusCorridorPersonEntersVehicleEventHandler implements PersonEntersVehicleEventHandler{
	
	Scenario scenario;
	
	public BusCorridorPersonEntersVehicleEventHandler(Scenario scenario) {
		this.scenario = scenario;
	}
	
	public void reset(int iteration) {
	}

	public void handleEvent(PersonEntersVehicleEvent event) {
		System.out.println(" *** "+getDayTime(event.getTime())+" Uhr: Person "+event.getPersonId()+ " steigt in den "+event.getVehicleId()+" ein.");
	}

	private String getDayTime(double time) {
		double hours = Math.floor(time/3600);
		double minutes = Math.floor((time-(hours*3600))/60);
		double seconds = Math.floor(time-(hours*3600)-minutes*60);
		String hoursString = getString(hours);
		String minutesString = getString(minutes);
		String secondsString = getString(seconds);
		String dayTime = hoursString+":"+minutesString+":"+secondsString;
		return dayTime;
	}

	private String getString(double value) {
		String valueString = null;
		if (value < 10) {
			valueString = "0"+String.valueOf((int)value);
		}
		else {
			valueString = String.valueOf((int)value);
		}
		return valueString;
	}	

}
