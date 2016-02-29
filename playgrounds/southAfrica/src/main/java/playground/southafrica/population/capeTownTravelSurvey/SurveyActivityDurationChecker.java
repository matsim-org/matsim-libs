/* *********************************************************************** *
 * project: org.matsim.*
 * SurveyActivityDurationChecker.java
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
package playground.southafrica.population.capeTownTravelSurvey;

import playground.southafrica.population.utilities.PopulationUtils;
import playground.southafrica.utilities.Header;

/**
 * Checks the activity durations of the parsed survey plans. This should be
 * done before allocating the survey plans to a full population. The objective
 * of this class is mainly fault finding.
 * 
 * @author jwjoubert
 */
public class SurveyActivityDurationChecker {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SurveyActivityDurationChecker.class.toString(), args);
		
		String populationFile = args[0];
		String outputFile = args[1];
		
		PopulationUtils.extractActivityDurations(populationFile, outputFile);

		Header.printFooter();
	}

}
