/* *********************************************************************** *
 * project: org.matsim.*
 * MyFileSamplerTest.java
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

package playground.jjoubert.Utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.testcases.MatsimTestCase;

import playground.jjoubert.Utilities.FileSampler.MyFileFilter;
import playground.jjoubert.Utilities.FileSampler.MyFileSampler;


public class MyFileSamplerTest extends MatsimTestCase{
	private final static Logger log = Logger.getLogger(MyFileSamplerTest.class);
	private File textInputFolder;
	private File textOutputFolder;

	/**
	 * This test checks that the right number of files of the right type are sampled
	 * <b><i>only</i></b>.If <code>a</code> is the number of files of correct type
	 * in a group of <code>b</code> files where <code>a < b</code>, then, given that
	 * <code>c <= a</code>, the right number of files sampled should be <code>c</code>.
	 */
	public void testSmallSampleOnly(){
		// Create sample files in the input folder.
		int rightFiles = 20;
		String rightExtension = ".txt";
		createFile(rightFiles, rightExtension);
		int wrongFiles = 20;
		String wrongExtention = ".abc";
		createFile(wrongFiles, wrongExtention);

		// Create the extension filter.
		MyFileFilter txtFilter = new MyFileFilter(rightExtension);

		// Sample the files
		MyFileSampler textSampler = new MyFileSampler(textInputFolder.getAbsolutePath());
		int numberToSample = (int) Math.max(1, Math.round(0.5*rightFiles));
		List<File> files = textSampler.sampleFiles(numberToSample, txtFilter);

		assertEquals("Incorrect number of files sampled.", numberToSample, files.size());
		assertEquals("Files should not have been copied.", 0, textOutputFolder.listFiles().length);
	}

	/**
	 * This test checks that the right number of files of the right type are sampled
	 * <b><i>only</i></b>. If <code>a</code> is the number of files of correct
	 * type in a group of <code>b</code> files where <code>a < b</code>, then, given
	 * that <code>c > a</code>, the right number of files sampled should be
	 * <code>a</code>.
	 */
	public void testLargeSampleOnly(){
		// Create sample files in the input folder.
		int rightFiles = 20;
		String rightExtension = ".txt";
		createFile(rightFiles, rightExtension);
		int wrongFiles = 20;
		String wrongExtention = ".abc";
		createFile(wrongFiles, wrongExtention);

		// Create the extension filter.
		MyFileFilter txtFilter = new MyFileFilter(rightExtension);

		// Sample the files
		MyFileSampler textSampler = new MyFileSampler(textInputFolder.getAbsolutePath());
		int numberToSample = 2*rightFiles;
		List<File> files = textSampler.sampleFiles(numberToSample, txtFilter);

		assertEquals("Incorrect number of files sampled.", rightFiles, files.size());
		assertEquals("Files should not have been copied.", 0, textOutputFolder.listFiles().length);
	}


	/**
	 * This test checks that the right number of files of the right type are sampled
	 * <b><i>and copied</i></b>. If <code>a</code> is the number of files of correct
	 * type in a group of <code>b</code> files where <code>a < b</code>, then, given
	 * that <code>c <= a</code>, the right number of files sampled should be
	 * <code>c</code>.
	 */
	public void testSmallSampleCopy(){
		// Create sample files in the input folder.
		int rightFiles = 20;
		String rightExtension = ".txt";
		createFile(rightFiles, rightExtension);
		int wrongFiles = 20;
		String wrongExtention = ".abc";
		createFile(wrongFiles, wrongExtention);

		// Create the extension filter.
		MyFileFilter txtFilter = new MyFileFilter(rightExtension);

		// Sample and copy the files
		MyFileSampler textSampler = new MyFileSampler(textInputFolder.getAbsolutePath(), textOutputFolder.getAbsolutePath());
		int numberToSample = (int) Math.max(1, Math.round(0.5*rightFiles));
		List<File> files = textSampler.sampleFiles(numberToSample, txtFilter);

		assertEquals("Incorrect number of files sampled.", numberToSample, files.size());
		assertEquals("Incorrect number of files copied.", numberToSample, textOutputFolder.listFiles().length);
		for (File file : textOutputFolder.listFiles()) {
			String thisExtension = file.getName().substring(file.getName().indexOf("."), file.getName().length());
			assertEquals("Wrong file identified by the filter", true, thisExtension.equals(rightExtension));
		}
	}

	/**
	 * This test checks that the right number of files of the right type are sampled
	 * <b><i>and copied</i></b>. If <code>a</code> is the number of files of correct
	 * type in a group of <code>b</code> files where <code>a < b</code>, then, given
	 * that <code>c > a</code>, the right number of files sampled should be
	 * <code>a</code>.
	 */
	public void testLargeSampleCopy(){
		// Create sample files in the input folder.
		int rightFiles = 20;
		String rightExtension = ".txt";
		createFile(rightFiles, rightExtension);
		int wrongFiles = 20;
		String wrongExtention = ".abc";
		createFile(wrongFiles, wrongExtention);

		// Create the extension filter.
		MyFileFilter txtFilter = new MyFileFilter(rightExtension);

		// Sample and copy the files
		MyFileSampler textSampler = new MyFileSampler(textInputFolder.getAbsolutePath(), textOutputFolder.getAbsolutePath());
		int numberToSample = 2*rightFiles;
		List<File> files = textSampler.sampleFiles(numberToSample, txtFilter);

		assertEquals("Incorrect number of files sampled.", rightFiles, files.size());
		assertEquals("Incorrect number of files copied.", rightFiles, textOutputFolder.listFiles().length);
		for (File file : textOutputFolder.listFiles()) {
			String thisExtension = file.getName().substring(file.getName().indexOf("."), file.getName().length());
			assertEquals("Wrong file identified by the filter", true, thisExtension.equals(rightExtension));
		}
	}

	/**
	 * This private method just creates a number of files with a specific extension.
	 * @param number the number of files to be created.
	 * @param extention the file extension of the created files. <b><i>Note:</i></b> the
	 * 		extension must include the '.' (full stop) character.
	 */
	private void createFile(int number, String extention){
		for(int i = 0; i < number; i++){
			try {
				BufferedWriter output = new BufferedWriter(new FileWriter(new File(textInputFolder + "/File" + String.valueOf(i) + extention)));
				try{
					output.write("File number ");
					output.write(String.valueOf(i));
				} finally{
					output.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		textInputFolder = new File(getOutputDirectory() + "Input/");
		boolean testMake = textInputFolder.mkdirs();
		if(!testMake){
			log.warn("Could not create the text input directory, or it already exists!");
		}
		textOutputFolder = new File(getOutputDirectory() + "Output/");
		testMake = textOutputFolder.mkdirs();
		if(!testMake){
			log.warn("Could not create the text output directory, or it already exists!");
		}
	}


}
