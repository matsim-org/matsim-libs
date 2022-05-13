/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package example.lsp.initialPlans;

import example.lsp.initialPlans.ExampleSchedulingOfTransportChainHubsVsDirect.SolutionType;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.fail;

public class ExampleSchedulingOfTransportChainHubsVsDirectTest{
	private static final Logger log = Logger.getLogger( ExampleSchedulingOfTransportChainHubsVsDirectTest.class );
	@Rule public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMain1(){

		try{
			ExampleSchedulingOfTransportChainHubsVsDirect.main( new String []{
					"--config:controler.outputDirectory=" + utils.getOutputDirectory()
					, "--config:controler.lastIteration=2"
					,"--solutionType=" + SolutionType.onePlan_withHub
			} );

		} catch ( Exception ee ) {
			log.fatal(ee) ;
			fail() ;
		}

		//Compare written out schedule.
		MatsimTestUtils.compareFilesLineByLine(utils.getInputDirectory() + "schedules.txt" , utils.getOutputDirectory() + "schedules.txt" );
	}
	@Test
	public void testMain2_direct(){

		try{
			ExampleSchedulingOfTransportChainHubsVsDirect.main( new String []{
					"--config:controler.outputDirectory=" + utils.getOutputDirectory()
					, "--config:controler.lastIteration=2"
					, "--solutionType=" + SolutionType.onePlan_direct
			} );

		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail() ;
		}

		//Compare written out schedule.
		MatsimTestUtils.compareFilesLineByLine(utils.getInputDirectory() + "schedules.txt" , utils.getOutputDirectory() + "schedules.txt" );
	}
}
