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
package org.matsim.core.controler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
public class OutputDirectoryHierarchyTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testFailureIfDirectoryExists() {
		final String outputDirectory = utils.getOutputDirectory();
		IOUtils.deleteDirectoryRecursively(new File( outputDirectory ).toPath());

		// directory creation is a side effect of instanciation...
		new OutputDirectoryHierarchy(
				outputDirectory,
				OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists,
				ControllerConfigGroup.CompressionType.none);

		Assertions.assertTrue(
				new File( outputDirectory ).exists(),
				"Directory was not created" );

		// put something in the directory
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputDirectory+"/some_file" ) ) {
			writer.write( "stuff" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}

		try {
			// directory creation is a side effect of instanciation...
			new OutputDirectoryHierarchy(
					outputDirectory,
					OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists,
					ControllerConfigGroup.CompressionType.none);
		}
		catch ( RuntimeException e ) {
			return;
		}
		Assertions.fail( "no exception thrown when directory exists!" );
	}

	@Test
	void testOverrideIfDirectoryExists() {
		final String outputDirectory = utils.getOutputDirectory();
		IOUtils.deleteDirectoryRecursively(new File( outputDirectory ).toPath());

		// directory creation is a side effect of instanciation...
		new OutputDirectoryHierarchy(
				outputDirectory,
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles,
				ControllerConfigGroup.CompressionType.none);

		Assertions.assertTrue(
				new File( outputDirectory ).exists(),
				"Directory was not created" );

		// put something in the directory
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputDirectory+"/some_file" ) ) {
			writer.write( "stuff" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}

		// directory creation is a side effect of instanciation...
		new OutputDirectoryHierarchy(
				outputDirectory,
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles,
				ControllerConfigGroup.CompressionType.none);

		Assertions.assertTrue(
				new File( outputDirectory+"/some_file" ).exists(),
				"Directory was cleared" );

	}

	@Test
	void testDeleteIfDirectoryExists() {
		final String outputDirectory = utils.getOutputDirectory();
		IOUtils.deleteDirectoryRecursively(new File( outputDirectory ).toPath());

		// directory creation is a side effect of instanciation...
		new OutputDirectoryHierarchy(
				outputDirectory,
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists,
				ControllerConfigGroup.CompressionType.none);

		Assertions.assertTrue(
				new File( outputDirectory ).exists(),
				"Directory was not created" );

		// put something in the directory
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputDirectory+"/some_file" ) ) {
			writer.write( "stuff" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}

		// directory creation is a side effect of instanciation...
		new OutputDirectoryHierarchy(
				outputDirectory,
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists,
				ControllerConfigGroup.CompressionType.none);

		Assertions.assertTrue(
				new File( outputDirectory ).exists(),
				"Directory was deleted but not re-created!" );

		Assertions.assertFalse(
				new File( outputDirectory+"/some_file" ).exists(),
				"Directory was not cleared" );

	}

	@Test
	void testTmpDir() {

		String javaTempDir = System.getProperty("java.io.tmpdir");
		System.setProperty(OutputDirectoryHierarchy.MATSIM_TEMP_DIR_PROPERTY, javaTempDir);
		Assertions.assertNotNull(javaTempDir);

		System.setProperty("matsim.preferLocalDtds", "true");

		URL scenarioURL = ExamplesUtils.getTestScenarioURL("siouxfalls-2014");

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(scenarioURL, "config_default.xml"));
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(1);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controller = new Controler(scenario);
		controller.run();

		String matsimTempDir = controller.getControlerIO().getTempPath();
		Assertions.assertEquals(javaTempDir, matsimTempDir);
	}

}
