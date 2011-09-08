/* *********************************************************************** *
 * project: org.matsim.*
 * RunXml.java
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
package playground.thibautd.analysis.joinabletripsidentifier;

/**
 * @author thibautd
 */
public class RunXml {
	public static void main(final String[] args) {
		// TODO: logging
		JoinableTripsXmlReader reader = new JoinableTripsXmlReader();
		reader.parse(args[0]+"trips.xml");

		JoinableTrips joinableTripData = reader.getJoinableTrips();

		(new JoinableTripsXmlWriter(joinableTripData)).write(args[0]+"trips-reWrite.xml");
	}
}

