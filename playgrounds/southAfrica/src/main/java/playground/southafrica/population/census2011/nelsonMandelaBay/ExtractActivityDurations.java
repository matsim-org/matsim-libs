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

package playground.southafrica.population.census2011.nelsonMandelaBay;

import playground.southafrica.population.utilities.PopulationUtils;
import playground.southafrica.utilities.Header;

/**
 * Basic implementation of {@link PopulationUtils#extractActivityDurations(String, String)}
 * once plans (and GTI buildings) have been assigned to the Nelson Mandela Bay
 * 10% sample population.
 * 
 * @author jwjoubert
 */
public class ExtractActivityDurations {

	public static void main(String[] args) {
		Header.printHeader(ExtractActivityDurations.class.toString(), args);
		
		String populationFile = args[0];
		String outputFile = args[1];
		PopulationUtils.extractActivityDurations(populationFile, outputFile);
		
		Header.printFooter();
	}

}
