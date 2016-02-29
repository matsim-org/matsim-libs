/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTypeExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.southafrica.population.census2011.capeTown;

import playground.southafrica.population.utilities.PopulationUtils;
import playground.southafrica.utilities.Header;

/**
 * A class to extract the activity times for the City of Cape Town population.
 * 
 * @author jwjoubert
 */
public class CapeTownActivityTypeExtractor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CapeTownActivityTypeExtractor.class.toString(), args);
		
		/* For persons. */
		String populationFile = args[0];
		String outputFile = args[1];
		PopulationUtils.extractActivityDurations(populationFile, outputFile);

		/* For freight. */
		String freightFile = args[2];
		String freightOutputFile = args[3];
		PopulationUtils.extractActivityDurations(freightFile, freightOutputFile);

		Header.printFooter();
	}

}
