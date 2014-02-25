/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractTripModeSharesLocatedInArea.java
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
package playground.thibautd.socnetsim.analysis.scripts;

import java.io.IOException;

import playground.thibautd.socnetsim.analysis.LocatedTripsWriter;

/**
 * @author thibautd
 */
public class ExtractTripModeSharesLocatedInArea {

	public static void main(final String[] args) throws IOException {
		final String inPopFile = args[ 0 ];
		final String outRawData = args[ 1 ];

		LocatedTripsWriter.write( inPopFile , outRawData );
	}
}

