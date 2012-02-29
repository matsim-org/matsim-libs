/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideFacilitiesXmlSchemaNames.java
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
package playground.thibautd.parknride;

/**
 * defines tags for the xml format for park and ride facilities.
 * @author thibautd
 */
public class ParkAndRideFacilitiesXmlSchemaNames {
	private ParkAndRideFacilitiesXmlSchemaNames() {}

	public static final String ROOT_TAG = "facilities";
	public static final String NAME_ATT = "name";

	public static final String FACILITY_TAG = "facility";
	public static final String X_COORD_ATT = "x";
	public static final String Y_COORD_ATT = "y";
	public static final String ID_ATT = "id";
	public static final String LINK_ID_ATT = "link";

	public static final String STOP_TAG = "attachedTransitStopFacility";
}

