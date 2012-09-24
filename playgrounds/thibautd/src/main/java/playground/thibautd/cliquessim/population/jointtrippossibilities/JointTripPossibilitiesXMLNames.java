/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilitiesXMLNames.java
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
package playground.thibautd.cliquessim.population.jointtrippossibilities;

/**
 * @author thibautd
 */
public class JointTripPossibilitiesXMLNames {
	private JointTripPossibilitiesXMLNames() {}

	public static final String ROOT_TAG = "possibilities";
	public static final String DESC_ATT = "name";

	public static final String POSS_TAG = "possibility";

	public static final String DRIVER_ID_ATT = "driverId";
	public static final String PASSENGER_ID_ATT = "passengerId";
	public static final String DRIVER_OR_ATT = "driverOrigin";
	public static final String PASSENGER_OR_ATT = "passengerOrigin";
	public static final String DRIVER_DEST_ATT = "driverDestination";
	public static final String PASSENGER_DEST_ATT = "passengerDestination";
}

