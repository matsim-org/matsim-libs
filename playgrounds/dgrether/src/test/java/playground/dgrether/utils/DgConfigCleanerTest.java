/* *********************************************************************** *
 * project: org.matsim.*
 * DgConfigCleanerTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.dgrether.utils.DgConfigCleaner;



public class DgConfigCleanerTest {
	
	@Rule
	public  MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void testCleanAndWriteConfig(){
		String originalConfigFilename = this.testUtils.getClassInputDirectory() + "749.output_config.xml.gz";
		String cleanedConfigFilename = this.testUtils.getOutputDirectory() + "cleaned_config.xml";
		try {
			new DgConfigCleaner().cleanAndWriteConfig(originalConfigFilename, cleanedConfigFilename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		Config config = ConfigUtils.loadConfig(cleanedConfigFilename);
		Assert.assertNotNull(config);
		
	}

}
