/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatControlerTest.java
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

package org.matsim.planomat;

import org.apache.log4j.Logger;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

public class PlanomatControlerTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(PlanomatControlerTest.class);
	
	protected void setUp() throws Exception {
		super.setUp();
		
	}

	public void testMainCar() {
		
//		String[] args = new String[]{this.getInputDirectory() + "config.xml"};
//		
//		PlanomatControler.main(args);
//		
//		// actual test: compare checksums of the files
//		final long expectedChecksum = CRCChecksum.getCRCFromGZFile(this.getInputDirectory() + "plans.xml.gz");
//		final long actualChecksum = CRCChecksum.getCRCFromGZFile(this.getOutputDirectory() + "output_plans.xml.gz");
//		log.info("Expected checksum: " + Long.toString(expectedChecksum));
//		log.info("Actual checksum: " + Long.toString(actualChecksum));
//		assertEquals(expectedChecksum, actualChecksum);
	}

	public void testMainCarPt() {

		String[] args = new String[]{this.getInputDirectory() + "config.xml"};

		PlanomatControler.main(args);

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromGZFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromGZFile(this.getOutputDirectory() + "output_plans.xml.gz");
		log.info("Expected checksum: " + Long.toString(expectedChecksum));
		log.info("Actual checksum: " + Long.toString(actualChecksum));
		assertEquals(expectedChecksum, actualChecksum);
		
	}
	
}
