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
package eu.eunoiaproject.bikesharing.framework.examples;

import eu.eunoiaproject.bikesharing.examples.example03configurablesimulation.RunConfigurableBikeSharingSimulation;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class TestRegressionConfigurableExample {
	@Rule public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunDoesNotFail() {
		// this allows to change output folder
		final Config conf = utils.loadConfig( "examples/bikesharing/hugegrid/config-bikesharing.xml" );
		conf.controler().setOutputDirectory( utils.getOutputDirectory()+"/output/" );
		conf.controler().setLastIteration( 2 );
		String confFile = utils.getOutputDirectory()+"/config.xml";
		new ConfigWriter( conf ).write( confFile );
		RunConfigurableBikeSharingSimulation.main( confFile );
	}

	@Test
	public void testRunDoesNotFailFastAStar() {
		// this allows to change output folder
		final Config conf = utils.loadConfig( "examples/bikesharing/hugegrid/config-bikesharing.xml" );
		conf.controler().setOutputDirectory(utils.getOutputDirectory() + "/output/");
		conf.controler().setLastIteration( 2 );
		conf.controler().setRoutingAlgorithmType( RoutingAlgorithmType.FastAStarLandmarks );
		String confFile = utils.getOutputDirectory()+"/config.xml";
		new ConfigWriter( conf ).write( confFile );
		RunConfigurableBikeSharingSimulation.main( confFile );
	}
}
