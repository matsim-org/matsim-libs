/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.southafrica.projects.digicore;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.utilities.FileUtils;

/**
 * Test to check if automatic 'file sensing' occurs, and logged after 
 * processing.
 *  
 * @author jwjoubert
 */
public class AggregateRawDigicoreFilesTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMoveEventsFile() {
		String[] args = getArgs();
		
		String s1 = args[0] + (args[0].endsWith("/") ? "" : "/") + "events.csv.gz";
		File f1 = new File(s1);
		String s2 = args[1] + (args[1].endsWith("/") ? "" : "/") + "events.csv.gz";
		File f2 = new File(s2);

		/* Write the first events file. */
		writeCsvFile(s1,1);
		Assert.assertTrue("Origin file should exist.", f1.exists());
		Assert.assertFalse("Destination file should NOT exist.", f2.exists());
		
		/* Move the first events file. */
		try {
			AggregateRawDigicoreFiles.moveEventsFile(args);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to move 'events.csv.gz' file!");
		}
		Assert.assertFalse("Origin file should NOT exist anymore.", f1.exists());
		Assert.assertTrue("Destination file should exist.", f2.exists());
		
		/* Write the second events file. */
		writeCsvFile(s1,1);
		Assert.assertTrue("Origin file should exist.", f1.exists());
		Assert.assertTrue("Destination file should already exist.", f2.exists());
		
		/* Move the first events file. */
		try {
			AggregateRawDigicoreFiles.moveEventsFile(args);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to move 'events.csv.gz' file!");
		}
		Assert.assertFalse("Origin file should NOT exist anymore.", f1.exists());
		Assert.assertTrue("Destination file should exist.", f2.exists());
	}
	
	@Test
	public void testCheckFileStatus(){
		try{
			String[] sa = {"./dummy"};
			AggregateRawDigicoreFiles.checkFileStatus(sa);
			fail("Should have thrown an illegal argument exception. Folder does not exist.");
		} catch(IllegalArgumentException e){
			/* Correctly caught an illegal folder name. */
		}
		
		String[] args = getArgs();
		AggregateRawDigicoreFiles.checkFileStatus(args);
	}
	
	@Test
	public void testParseFileRegister(){
		String[] args = getArgs();
		List<String> files = null;
		try {
			files = AggregateRawDigicoreFiles.parseFileRegister(args);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to parse file register... even if returning an empty list.");
		}
		Assert.assertEquals("Should have 0 files for non-existing register.", 0, files.size());

		/* Register file with header and one additional line. */
		String s1 = args[2] + (args[2].endsWith("/") ? "" : "/") + "registerOfProcessedFiles.csv";
		writeCsvFile(s1,1);
		try {
			files = AggregateRawDigicoreFiles.parseFileRegister(args);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to parse file register... even if returning an empty list.");
		}
		Assert.assertEquals("Should have 1 file for dummy register.", 1, files.size());
	}
	
	@Test
	public void testProcessRawFiles(){
		String[] args = getArgs();
		
		try {
			List<String> register = AggregateRawDigicoreFiles.parseFileRegister(args);
			Assert.assertEquals("Should have NO register entries.", 0, register.size());
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to parse register file.");
		}

		/* Create 1st file. */
		String s1 = args[0] + (args[0].endsWith("/") ? "" : "/") + "1.csv.gz";
		writeCsvFile(s1, 10);
		try {
			AggregateRawDigicoreFiles.processRawFiles(args);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to process raw input file.");
		}
		try {
			List<String> register = AggregateRawDigicoreFiles.parseFileRegister(args);
			Assert.assertEquals("Should have ONE register entry.", 1, register.size());
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to parse register file.");
		}

		/* Create 2nd, duplicate file. */
		String s2 = args[0] + (args[0].endsWith("/") ? "" : "/") + "1.csv.gz";
		writeCsvFile(s2, 10);
		try {
			AggregateRawDigicoreFiles.processRawFiles(args);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to process raw input file.");
		}
		try {
			List<String> register = AggregateRawDigicoreFiles.parseFileRegister(args);
			Assert.assertEquals("Should still have ONE register entry.", 1, register.size());
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to parse register file.");
		}
		new File(s2).delete();

		/* Create 3rd, non-duplicate file. */
		String s3 = args[0] + (args[0].endsWith("/") ? "" : "/") + "2.csv.gz";
		writeCsvFile(s3, 10);
		try {
			AggregateRawDigicoreFiles.processRawFiles(args);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to process raw input file.");
		}
		try {
			List<String> register = AggregateRawDigicoreFiles.parseFileRegister(args);
			Assert.assertEquals("Should now have TWO register entries.", 2, register.size());
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should be able to parse register file.");
		}
	}
	
	
	/**
	 * Writes out a dummy CSV file to a given location.
	 * 
	 * @param filename
	 */
	private void writeCsvFile(String filename, int numberOfLinesAfterHeader){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("file,modified,processed");
			bw.newLine();
			for(int i=0; i < numberOfLinesAfterHeader; i++){
				bw.write(String.format("%d,12345,12345", i+1));
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				fail("Could not close " + filename);
			}
		}
	}

	
	/**
	 * Returns the same string arguments as created during setUp().
	 * @return
	 */
	private String[] getArgs(){
		String f1 = utils.getClassInputDirectory() + 
				(utils.getClassInputDirectory().endsWith("/") ? "" : "/") + 
				"raw/";
		String f2 = utils.getClassInputDirectory() + 
				(utils.getClassInputDirectory().endsWith("/") ? "" : "/") + 
				"processed/";
		String f3 = utils.getClassInputDirectory() + 
				(utils.getClassInputDirectory().endsWith("/") ? "" : "/") + 
				"log/";
		String[] sa = {f1,f2,f3};
		return sa;
	}
	
	/**
	 * Creates the necessary folders for a) input files; b) where processed 
	 * files are to be placed; and c) where log files are created and stored.
	 *  
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		String[] sa = getArgs();
		/* Create all the folders. */
		File inputFolder = new File(sa[0]);
		boolean inputCreated = inputFolder.mkdirs();
		if(!inputCreated){
			fail("Cannot create input folder!");
		}
		File processedFolder = new File(sa[1]);
		boolean processedCreated = processedFolder.mkdirs();
		if(!processedCreated){
			fail("Cannot create processed folder!");
		}
		File logFolder = new File(sa[2]);
		boolean logCreated = logFolder.mkdirs();
		if(!logCreated){
			fail("Cannot create log folder!");
		}
	}

	/** 
	 * Ensure the created folders, and its contents are cleaned up and deleted 
	 * again.
	 */
	@After
	public void cleanUp(){
		String[] sa = getArgs();
		/* Clean up the folders (possibly) created. I say 'possibly' because 
		 * the setUp() may itself have failed before all folders were created. */
		File inputFolder = new File(sa[0]);
		File processedFolder = new File(sa[1]);
		File logFolder = new File(sa[2]);
		FileUtils.delete(inputFolder);
		FileUtils.delete(processedFolder);
		FileUtils.delete(logFolder);
	}

}
