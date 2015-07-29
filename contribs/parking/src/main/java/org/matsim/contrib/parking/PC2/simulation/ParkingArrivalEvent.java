/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.PC2.simulation;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.utils.geometry.CoordImpl;

public class ParkingArrivalEvent extends Event {

	private Id parkingId;
	private Id personId;
	public final static String ATTRIBUTE_PARKING_ID = "parkingId";
	public final static String ATTRIBUTE_PERSON_ID = "personId";
	public final static String EVENT_TYPE = "parkingArrivalEvent";
	public final static String DEST_COORD_X = "destinationCoordinateX";
	public final static String DEST_COORD_Y = "destinationCoordinateY";
	public final static String PARKING_SCORE = "parkingScore";
	private Coord destCoordinate;
	private double score;
	
	// yyyyyy the parking arrival event returns person id, not vehicle id.  Do we change to vehicle id, or add the vehicle
	// id to the event?  It is used for the walk distance; for that we need the person id. kai, jul'15

	public ParkingArrivalEvent(double time, Id parkingId, Id personId, Coord destCoordinate, double score) {
		super(time);
		this.destCoordinate = destCoordinate;
		this.score = score;
		
		if (time>110000){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		this.parkingId = parkingId;
		this.personId = personId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		final Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_PARKING_ID, parkingId.toString());
		attributes.put(ATTRIBUTE_PERSON_ID, personId!=null?personId.toString():null);
		if (destCoordinate==null){
			attributes.put(DEST_COORD_X, null);
			attributes.put(DEST_COORD_Y, null);
		} else {
			attributes.put(DEST_COORD_X, Double.toString(destCoordinate.getX()));
			attributes.put(DEST_COORD_Y, Double.toString(destCoordinate.getY()));
		}
		
		attributes.put(PARKING_SCORE, Double.toString(score));
		return attributes;
	}
	
	public static Coord getDestCoord(Map<String, String> attributes){
		String destCoordXString = attributes.get(ParkingArrivalEvent.DEST_COORD_X);
		String destCoordYString = attributes.get(ParkingArrivalEvent.DEST_COORD_Y);
		
		if (destCoordXString==null || destCoordYString==null){
			return null;
		} else {
			return new CoordImpl(Double.parseDouble(destCoordXString),Double.parseDouble(destCoordYString));
		}
	}
	
	public static double getScore(Map<String, String> attributes){
		String scoreString = attributes.get(ParkingArrivalEvent.PARKING_SCORE);
		return Double.parseDouble(scoreString);
	}
	
	public static Id<Person> getPersonId(Map<String, String> attributes){
		String personIdString = attributes.get(ParkingArrivalEvent.ATTRIBUTE_PERSON_ID);
		if (personIdString==null){
			return null;
		} else {
			return Id.create(personIdString, Person.class);
		}
	}
	
	public static Id<PC2Parking> getParkingId(Map<String, String> attributes){
		String parkingIdString = attributes.get(ParkingArrivalEvent.ATTRIBUTE_PARKING_ID);
		if (parkingIdString==null){
			return null;
		} else {
			return Id.create(parkingIdString, PC2Parking.class);
		}
	}

}
