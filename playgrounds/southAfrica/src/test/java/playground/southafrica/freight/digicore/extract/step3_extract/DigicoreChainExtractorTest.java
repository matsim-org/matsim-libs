/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreChainExtractorTest.java
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

package playground.southafrica.freight.digicore.extract.step3_extract;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.Counter;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreChainElement;
import playground.southafrica.freight.digicore.containers.DigicoreTrace;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class DigicoreChainExtractorTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	
	@Test
	public void testDigicoreChainExtractor() {
		try{
			@SuppressWarnings("unused")
			DigicoreChainExtractor dce = setup();
		} catch(Exception r){
			fail("Should have created class without exceptions.");
		}
	}

	@Test
	public void testRun() {
		DigicoreChainExtractor dce = setup();
		try{
			dce.run();
		} catch(Exception e){
			fail("Should have run without exceptions.");
		}
		assertTrue("Output file should exist.", new File(utils.getOutputDirectory() + "test.xml.gz").exists());
	}

	@Test
	public void testGetVehicle() {
		DigicoreChainExtractor dce = setup();
		dce.run();
		DigicoreVehicle vehicle = dce.getVehicle();
		List<DigicoreChain> chains = vehicle.getChains();
		assertEquals("Wrong number of chains.", 1, chains.size());
		
		/* Check each element. */
		DigicoreChain chain = chains.get(0);
		DigicoreChainElement e1 = chain.get(0);
		assertTrue("Wrong element type.", e1 instanceof DigicoreActivity);
		DigicoreChainElement e2 = chain.get(1);
		assertTrue("Wrong element type.", e2 instanceof DigicoreTrace);
		DigicoreChainElement e3 = chain.get(2);
		assertTrue("Wrong element type.", e3 instanceof DigicoreActivity);
	}
	
	private DigicoreChainExtractor setup(){
		File inputFile = new File(utils.getClassInputDirectory() + "test.txt");
		File outputFolder = new File(utils.getOutputDirectory());
		
		List<String> ignitionOn = new ArrayList<String>(1);
		ignitionOn.add("0");
		List<String> ignitionOff = new ArrayList<>(1);
		ignitionOff.add("15");

		Counter threadCounter = new Counter("   test # ");
		DigicoreChainExtractor dce = new DigicoreChainExtractor(inputFile, outputFolder, 100.0, 10.0, ignitionOn , ignitionOff , null, threadCounter);
		return dce;
	}

}
