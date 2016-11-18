/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.analysis.socialchoicesetconstraints;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.utils.MonitoringUtils;
import playground.ivt.utils.MoreIOUtils;

/**
 * @author thibautd
 */
public class AnalyzeSocialChoiceSet {
	public static void main( final String... args ) throws Exception {
		final Config config = ConfigUtils.loadConfig( args[ 0 ] , new SocialChoiceSetConstraintsConfigGroup() );

		try ( AutoCloseable monitor = MonitoringUtils.monitorAndLogOnClose();
				AutoCloseable logCloseable = MoreIOUtils.initOut( config );
				AutoCloseable gcTracker = MonitoringUtils.writeGCFigure( config.controler().getOutputDirectory()+"/gc.dat" ) ) {
			final Scenario scenario = ScenarioUtils.loadScenario( config );
			final SocialChoiceSetConstraintsAnalyser analyser = new SocialChoiceSetConstraintsAnalyser( scenario );

			analyser.analyzeToFile( config.controler().getOutputDirectory() +"/constrainedChoiceSetSizes.dat" );
		}
	}
}

