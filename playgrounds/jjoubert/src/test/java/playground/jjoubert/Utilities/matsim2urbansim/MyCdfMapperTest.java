/* *********************************************************************** *
 * project: org.matsim.*
 * MyCdfMapperTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.testcases.MatsimTestUtils;

import nl.knaw.dans.common.dbflib.CorruptedTableException;
import playground.fabrice.primloc.CumulativeDistribution;


public class MyCdfMapperTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	private final Logger log = Logger.getLogger(MyCdfMapperTest.class);
	
	public void testMyCdfMapperConstructor(){
		
	}
	
	@Test
	public void testReadCarDbf(){
		String dbf = new File(utils.getInputDirectory()).getParent() + "/dbfCar.dbf";
		MyCdfMapper mcm = new MyCdfMapper();
		
		// Try and read non-existing file;
		try {
			mcm.readCarDbf("dummy.dbf");
		} catch (CorruptedTableException e1) {
			Assert.fail("Nonexisting Dbf file should not be corrupted.");
		} catch (IOException e1) {
			log.info("Caught expected IOException.");
		}
		
		// Try and read existing file;
		try {
			mcm.readCarDbf(dbf);
		} catch (CorruptedTableException e) {
			Assert.fail("Dbf file should not be corrupted.");
		} catch (IOException e) {
			Assert.fail("Dbf file should exist.");
		}
		Assert.assertEquals("Wrong number of bins.", 100, mcm.getCdfCar().getNumBins());
		CumulativeDistribution c = mcm.getCdfCar();
	}

	private CumulativeDistribution setupNormal(){
		MatsimRandom.reset(0);
		CumulativeDistribution result = new CumulativeDistribution(0, 100, 100);
		for(int i = 0; i < 10000; i++){
			result.addObservation(MatsimRandom.getRandom().nextDouble());
		}
		return result;
	}
}

