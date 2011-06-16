/* *********************************************************************** *
 * project: org.matsim.*
 * PlotDataFile.java
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
package playground.thibautd.analysis.possiblesharedrides;

import java.io.IOException;

/**
 * @author thibautd
 */
public class PlotDataFile {
	public static final void main(String[] args) {
		String fileName = args[0];
		String outputFile = args[1];
		int width = 1024;
		int height = 800;

		CountPossibleSharedRides counter = new CountPossibleSharedRides();
		
		try {
			counter.loadTripData(fileName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		counter.getBoxAndWhiskersPerTimeBin(24).saveAsPng(outputFile, width, height);
	}
}

