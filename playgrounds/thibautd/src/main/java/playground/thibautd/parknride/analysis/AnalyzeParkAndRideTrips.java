/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeParkAndRideTrips.java
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
package playground.thibautd.parknride.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.ChartUtil;
import playground.thibautd.utils.MoreIOUtils;

/**
 * @author thibautd
 */
public class AnalyzeParkAndRideTrips {
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	public static void main(final String[] args) {
		String configFile = args[ 0 ];
		String outputDir = args[ 1 ];

		MoreIOUtils.initOut( outputDir );

		Config config = ConfigUtils.loadConfig( configFile );
		Scenario scenario = ScenarioUtils.loadScenario( config );

		ParkAndRideTripsAnalyzer analyzer = new ParkAndRideTripsAnalyzer( scenario.getPopulation() );
		
		ChartUtil chart = analyzer.getPtTimeProportionHistogram();
		chart.saveAsPng( outputDir+"/ptTimeProportion.png" , WIDTH , HEIGHT );

		chart = analyzer.getNumberOfPtLegsHistogram();
		chart.saveAsPng( outputDir+"/transitLegsNumber.png" , WIDTH , HEIGHT );
	}
}

