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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * @author thibautd
 */
public class OutputDirectoryHierarchyTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testFailureIfDirectoryExists() {
		final String outputDirectory = utils.getOutputDirectory();
		IOUtils.deleteDirectoryRecursively(new File( outputDirectory ).toPath());

		// directory creation is a side effect of instanciation...
		new OutputDirectoryHierarchy(
				outputDirectory,
				OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		Assert.assertTrue(
				"Directory was not created",
				new File( outputDirectory ).exists() );

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
					OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
		}
		catch ( RuntimeException e ) {
			return;
		}
		Assert.fail( "no exception thrown when directory exists!" );
	}

	@Test
	public void testOverrideIfDirectoryExists() {
		final String outputDirectory = utils.getOutputDirectory();
		IOUtils.deleteDirectoryRecursively(new File( outputDirectory ).toPath());

		// directory creation is a side effect of instanciation...
		new OutputDirectoryHierarchy(
				outputDirectory,
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		Assert.assertTrue(
				"Directory was not created",
				new File( outputDirectory ).exists() );

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
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		Assert.assertTrue(
				"Directory was cleared",
				new File( outputDirectory+"/some_file" ).exists() );

	}

	@Test
	public void testDeleteIfDirectoryExists() {
		final String outputDirectory = utils.getOutputDirectory();
		IOUtils.deleteDirectoryRecursively(new File( outputDirectory ).toPath());

		// directory creation is a side effect of instanciation...
		new OutputDirectoryHierarchy(
				outputDirectory,
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		Assert.assertTrue(
				"Directory was not created",
				new File( outputDirectory ).exists() );

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
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		Assert.assertTrue(
				"Directory was deleted but not re-created!",
				new File( outputDirectory ).exists() );

		Assert.assertFalse(
				"Directory was not cleared",
				new File( outputDirectory+"/some_file" ).exists() );

	}
}
