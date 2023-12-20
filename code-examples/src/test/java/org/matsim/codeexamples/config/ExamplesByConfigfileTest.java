/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class ExamplesByConfigfileTest {
	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	public static Stream<String> arguments() {
		return Stream.of("scenarios/equil/config.xml",
				"scenarios/equil/config-with-minimal-plans-file.xml",
				"scenarios/equil/config-with-mobsim.xml",
				"scenarios/equil/example2-config.xml",
				"scenarios/equil/example5-config.xml",
				"scenarios/equil/config-with-controlerListener.xml",
				"scenarios/equil/config-with-pluggablePlanStrategy.xml",
				"scenarios/equil-extended/config-extended.xml",
				"scenarios/equil-extended/config-with-lanes.xml",
				"scenarios/equil-extended/config-with-network-change-events.xml",
				"scenarios/equil-extended/config-with-subpopulation.xml",
				"scenarios/equil-extended/config-with-trips.xml",
				"scenarios/equil-mixedTraffic/config-with-mode-vehicles.xml",
				"scenarios/equil-mixedTraffic/config-with-all-vehicles-from-file.xml",
				"examples/tutorial/config/externalReplanning.xml");
	}

	private String outputDir ;


	/**
	 * Test method for {@link RunFromConfigfileExample#main(java.lang.String[])}.
	 */
	@SuppressWarnings("static-method")
	@ParameterizedTest
	@MethodSource("arguments")
	final void testMain(String configFile) {
		if ( outputDir==null ) {
			outputDir = utils.getOutputDirectory() ; // removes output dir every time this is run so run it only once
		}

		final Path fileName = Paths.get( configFile ).getFileName();
		// yy would be nice to remove the file extension. kai, jul'19

		try{

			if( configFile.contains( "config-with-mobsim" ) || configFile.contains( "example2-config" ) ){
				final String[] args = {configFile
					  , "--config:controler.outputDirectory=" + outputDir + "/" + fileName
				};
				RunFromConfigfileExample.main( args );
			} else{
				final String[] args = {configFile
					  , "--config:controler.outputDirectory=" + outputDir + "/" + fileName
					  , "--config:controler.lastIteration=2"
				};
				RunFromConfigfileExample.main( args );
			}
		} catch( Exception ee  ){
			ee.printStackTrace();
			Assertions.fail();
		}
	}

}
