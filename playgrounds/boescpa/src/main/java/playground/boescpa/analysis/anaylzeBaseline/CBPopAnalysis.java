/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.analysis.anaylzeBaseline;

import playground.boescpa.analysis.populationAnalysis.ActDurationAnalyzer;
import playground.boescpa.analysis.populationAnalysis.DepartureTimeAnalyzer;

/**
 * WHAT IS IT FOR?
 *
 * @author boescpa
 */
public class CBPopAnalysis {

	public static void main(final String[] args) {
		final String population = args[0];
		final String output = args[1];
		// departures
		DepartureTimeAnalyzer.main(new String[]{population, output + "depTimeAnalysis.txt"});
		DepartureTimeAnalyzer.main(new String[]{population, output + "depTimeShopAnalysis.txt", "cbShop"});
		DepartureTimeAnalyzer.main(new String[]{population, output + "depTimeLeisureAnalysis.txt", "cbLeisure"});
		DepartureTimeAnalyzer.main(new String[]{population, output + "depTimeWorkAnalysis.txt", "cbWork"});
		// durations
		ActDurationAnalyzer.main(new String[]{population, output + "durationAnalysis.txt"});
		ActDurationAnalyzer.main(new String[]{population, output + "durationShopAnalysis.txt", "cbShop"});
		ActDurationAnalyzer.main(new String[]{population, output + "durationLeisureAnalysis.txt", "cbLeisure"});
		ActDurationAnalyzer.main(new String[]{population, output + "durationWorkAnalysis.txt", "cbWork"});
	}

}
