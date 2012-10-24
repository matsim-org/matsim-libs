/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioGeneratorTest.java
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

package org.matsim.contrib.grips.scenariogenerator;

import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

public class ScenarioGeneratorTest extends MatsimTestCase {
	
	@Test
	public void testScenarioGenerator() {
		String inputDir = getInputDirectory();
		String outputDir = getOutputDirectory();
		new ScenarioGenerator(inputDir + "/sgen_config.xml").run();
		
//		FIXME [GL] improve config-file test so it only checks the relevant parts
//		assertEquals("different config-files.", CRCChecksum.getCRCFromFile(inputDir + "/config.xml"), CRCChecksum.getCRCFromFile(outputDir + "/config.xml"));
		
		assertEquals("different network-files.", CRCChecksum.getCRCFromFile(inputDir + "/network.xml.gz"), CRCChecksum.getCRCFromFile(outputDir + "/network.xml.gz"));
		
		assertEquals("different plans-files.", CRCChecksum.getCRCFromFile(inputDir + "/population.xml.gz"), CRCChecksum.getCRCFromFile(outputDir + "/population.xml.gz"));
	}

}
