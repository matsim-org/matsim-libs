/* *********************************************************************** *
 * project: org.matsim.*
 * IOUtilsTest
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
package org.matsim.core.utils.io;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 * @author dgrether
 */
public class IOUtilsTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private final static Logger log = Logger.getLogger(IOUtilsTest.class);

	/**
	 * Simple test that checks the creation of logfiles and the filter for the errorLogFile
	 * @throws IOException
	 * @author dgrether
	 */
	@Test
	public void testInitOutputDirLogging() throws IOException {
		System.out.println(utils.getOutputDirectory());
		String outDir = utils.getOutputDirectory();
		IOUtils.initOutputDirLogging(outDir, null);

		File l = new File(outDir + IOUtils.LOGFILE);
		File errorLog = new File(outDir + IOUtils.WARNLOGFILE);
		Assert.assertNotNull(l);
		Assert.assertNotNull(errorLog);
		Assert.assertTrue(l.exists());
		Assert.assertTrue(errorLog.exists());
		Assert.assertEquals(0, l.length());
		Assert.assertEquals(0, errorLog.length());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testRenameFile() throws IOException {
		System.out.println(utils.getOutputDirectory());
		String outputDir = utils.getOutputDirectory();
		String fromFileName = outputDir + "a.txt";
		String toFileName = outputDir + "b.txt";
		Assert.assertTrue(new File(fromFileName).createNewFile());
		Assert.assertTrue(IOUtils.renameFile(fromFileName, toFileName));
		Assert.assertFalse(new File(fromFileName).exists());
		Assert.assertTrue(new File(toFileName).exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testRenameFile_ToDirectory() throws IOException {
		String outputDir = utils.getOutputDirectory();
		String fromFileName = outputDir + "a.txt";
		String toFileName = outputDir + "b";
		Assert.assertTrue(new File(fromFileName).createNewFile());
		Assert.assertTrue(new File(toFileName).mkdir());
		Assert.assertTrue(IOUtils.renameFile(fromFileName, toFileName));
		Assert.assertFalse(new File(fromFileName).exists());
		Assert.assertTrue(new File(toFileName).isDirectory());
		Assert.assertTrue(new File(toFileName + "/a.txt").exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testRenameFile_MissingFromFile() {
		String outputDir = utils.getOutputDirectory();
		String fromFileName = outputDir + "c.txt";
		String toFileName = outputDir + "b.txt";
		Assert.assertFalse(IOUtils.renameFile(fromFileName, toFileName));
		Assert.assertFalse(new File(fromFileName).exists());
		Assert.assertFalse(new File(toFileName).exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testRenameFile_UnreadableFromFile() throws IOException {
		String outputDir = utils.getOutputDirectory();
		String fromDirName = outputDir + "a";
		String fromFileName = fromDirName + "/a.txt";
		String toFileName = outputDir + "b.txt";
		File fromDir = new File(fromDirName);
		Assert.assertTrue(fromDir.mkdir());
		File fromFile = new File(fromFileName);
		Assert.assertTrue(fromFile.createNewFile());
		Assert.assertTrue(fromFile.setReadable(false));
		Assert.assertTrue(fromDir.setWritable(false)); // lock from directory, so the created file cannot be moved away
		Assert.assertFalse(IOUtils.renameFile(fromFileName, toFileName));
		Assert.assertTrue(fromDir.setWritable(true)); // make it writable again, or we may have problems when we run the test again
		Assert.assertTrue(new File(fromFileName).exists());
		Assert.assertFalse(new File(toFileName).exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testRenameFile_BadDestinationFile() throws IOException {
		String outputDir = utils.getOutputDirectory();
		String fromFileName = outputDir + "a.txt";
		String toFileName = outputDir + "not/existing/path/b.txt";
		Assert.assertTrue(new File(fromFileName).createNewFile());
		Assert.assertFalse(IOUtils.renameFile(fromFileName, toFileName));
		Assert.assertTrue(new File(fromFileName).exists());
		Assert.assertFalse(new File(toFileName).exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testRenameFile_LockedDestinationDirectory() throws IOException {
		String outputDir = utils.getOutputDirectory();
		String fromFileName = outputDir + "a.txt";
		String toFileName = outputDir + "locked";
		File dir = new File(toFileName);
		Assert.assertTrue(dir.mkdir());
		Assert.assertTrue(dir.setWritable(false));

		Assert.assertTrue(new File(fromFileName).createNewFile());
		Assert.assertFalse(IOUtils.renameFile(fromFileName, toFileName));
		Assert.assertTrue(new File(fromFileName).exists());
		Assert.assertTrue(new File(toFileName).exists());
		Assert.assertFalse(new File(toFileName + "/a.txt").exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testDeleteDir() throws IOException {
		String outputDir = utils.getOutputDirectory();
		String testDir = outputDir + "a";
		String someFilename = testDir + "/a.txt";
		File dir = new File(testDir);
		Assert.assertTrue(dir.mkdir());
		File someFile = new File(someFilename);
		Assert.assertTrue(someFile.createNewFile());

		IOUtils.deleteDirectory(dir);

		Assert.assertFalse(someFile.exists());
		Assert.assertFalse(dir.exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testDeleteDir_InexistentDir() {
		String outputDir = utils.getOutputDirectory();
		String testDir = outputDir + "a";
		File dir = new File(testDir);

		try {
			IOUtils.deleteDirectory(dir);
			Assert.fail("expected Exception.");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
		Assert.assertFalse(dir.exists());
	}

}
