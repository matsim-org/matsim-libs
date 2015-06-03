/* *********************************************************************** *
 * project: org.matsim.*
 * DIGICORE_EVENT.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.southafrica.projects.digicore.scoring;

import org.apache.log4j.Logger;

import playground.southafrica.projects.digicore.scoring.DigiScorer.RISK_GROUP;


public enum DIGICORE_EVENT {
	SEVERE_G_FORCE(7), 
	HIGH_G_FORCE(8), 
	LOW_G_FORCE(9), 
	ROLL_OVER(11), 
	TRIP_START(12), 
	TRIP_END(13),
	HARSH_ACCELERATION(15), 
	HARSH_BRAKE(16), 
	HARSH_CORNER(17), 
	HARSH_BUMP(18), 
	SPEED_VIOLATION(20),
	UNKNOWN(0);

	DIGICORE_EVENT(int eventId) {
		this.eventId = eventId;
	}
	
	private static Logger log = Logger.getLogger(DIGICORE_EVENT.class);
	private int eventId;
	
	public static DIGICORE_EVENT getEvent(int eventId){
		switch (eventId) {
		case 7:
			return SEVERE_G_FORCE;
		case 8:
			return HIGH_G_FORCE;
		case 9:
			return LOW_G_FORCE;
		case 11:
			return ROLL_OVER;
		case 12:
			return TRIP_START;
		case 13:
			return TRIP_END;
		case 15:
			return HARSH_ACCELERATION;
		case 16:
			return HARSH_BRAKE;
		case 17:
			return HARSH_CORNER;
		case 18:
			return HARSH_BUMP;
		case 20:
			return SPEED_VIOLATION;
		default:
			log.warn("Cannot get a event type for eventId " + eventId);
			log.warn("Returning UNKNOWN, and all will have NONE as risk group.");
			return UNKNOWN;
		}
	}
	
	public RISK_GROUP getRiskGroup(){
		switch (eventId) {
		case 0:
			return RISK_GROUP.NONE;
		case 7:
		case 11:
			return RISK_GROUP.HIGH;
		case 8:
		case 15:
		case 16:
		case 17:
		case 20:
			return RISK_GROUP.MEDIUM;
		case 9:
		case 18:
			return RISK_GROUP.LOW;
		case 12:
		case 13:
			return RISK_GROUP.NONE;
		default:
			throw new RuntimeException("Don't know how to assign risk group to event id " + eventId);
		}
	}

}
