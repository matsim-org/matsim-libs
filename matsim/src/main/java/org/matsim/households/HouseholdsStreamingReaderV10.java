/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdsStreamingReaderV10
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.households;

import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.misc.Counter;



/**
 * @author dgrether
 *
 */
public class HouseholdsStreamingReaderV10 extends AbstractHouseholdsReaderV10 {

	
	private static final Logger log = LogManager.getLogger(HouseholdsStreamingReaderV10.class);
	
	private HouseholdsWriterV10 hhWriter;
	private HouseholdsAlgorithmRunner algoRunner;
	
	private final Counter counter = new Counter (" household # ");
	
	public HouseholdsStreamingReaderV10(HouseholdsAlgorithmRunner algoRunner){
		super(new HouseholdsImpl());
		this.algoRunner = algoRunner;
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (HouseholdsSchemaV10Names.HOUSEHOLD.equalsIgnoreCase(name)) {
			Household household = createHousehold();
			this.algoRunner.runAlgorithms(household);
			this.hhWriter.writeHousehold(household);
			counter.incCounter();
		}
	}
	
	public void readFileRunAlgorithmsAndWriteFile(String inputFile, String outputFile){
		if (inputFile.equalsIgnoreCase(outputFile)){
			throw new IllegalArgumentException("Inputfile and outputfile must not refer to the same filename!");
		}
		hhWriter = new HouseholdsWriterV10(super.getHouseholds());
		hhWriter.openFileAndWritePreamble(outputFile);
		super.readFile(inputFile);
		counter.printCounter();
		hhWriter.writeEndAndCloseFile();
		log.info("Done reading households, running algorithms, and writing file");
	}
	
}
