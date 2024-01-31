/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator.Result;

public class RunWithParkingProxyIT {
        private static final Logger log = LogManager.getLogger(RunWithParkingProxyIT.class);
        @RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Disabled
	void testMain(){
                RunWithParkingProxy.main( new String []{ IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "chessboard" ), "config.xml" ).toString()
                                , "--config:controler.outputDirectory=" + utils.getOutputDirectory()
                                , "--config:controler.lastIteration=2"
                                , "--config:controler.writePlansInterval=1"
                                , "--config:parkingProxy.method=events"
                                , "--config:global.numberOfThreads=1"
                                , "--config:controler.routingAlgorithmType=FastAStarLandmarks"
                } );
                {
                        String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
                        String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
                        Result result = EventsUtils.compareEventsFiles( expected, actual );
                        if(!result.equals(Result.FILES_ARE_EQUAL)) {
                        	throw new RuntimeException("Events comparison ended with result " + result.name());
                        }
                }
                {
                        final Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
                        PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_experienced_plans.xml.gz" );
                        final Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
                        PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_experienced_plans.xml.gz" );
                        if(!PopulationUtils.comparePopulations( expected, actual )) {
                        	throw new RuntimeException("Plans file comparison ended with result false");
                        }
                }
        }
}
