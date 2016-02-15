/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideFacilities2xy.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.parknride.ParkAndRideFacilities;
import playground.thibautd.parknride.ParkAndRideFacilitiesXmlReader;
import playground.thibautd.parknride.ParkAndRideFacility;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * a simple utility to extract coordinates of park and ride facilities.
 * To use to visualize the location of such facilities in via.
 * @author thibautd
 */
public class ParkAndRideFacilities2xy {

	public static void main(final String[] args) {
		String inputFileName = args[ 0 ];
		String outputFileName = args[ 1 ];

		ParkAndRideFacilitiesXmlReader reader = new ParkAndRideFacilitiesXmlReader();
		reader.parse( inputFileName );

		ParkAndRideFacilities facilities = reader.getFacilities();

		BufferedWriter writer = IOUtils.getBufferedWriter( outputFileName );
		Counter count = new Counter( "writing coordinate # " );
		try {
			for (ParkAndRideFacility facility : facilities.getFacilities().values()) {
				count.incCounter();
				Coord coord = facility.getCoord();
				writer.write( coord.getX()+"\t"+coord.getY() );
				writer.newLine();
			}
			count.printCounter();
			writer.close();
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
