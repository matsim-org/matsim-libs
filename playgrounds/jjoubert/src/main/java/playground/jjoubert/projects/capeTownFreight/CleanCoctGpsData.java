/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.jjoubert.projects.capeTownFreight;

import playground.southafrica.utilities.Header;

/**
 * Class to read in City of Cape Town GPS records and removing all points that
 * are not within the boundaries of the city.
 * 
 * @author jwjoubert
 */
public class CleanCoctGpsData {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CleanCoctGpsData.class.toString(), args);
		String gpsFile = args[0];
		String shapefile = args[1];
		
		/* TODO Read in shapefile. */
		
		/* TODO Parse GPS file. */
		
		Header.printFooter();
	}

}
