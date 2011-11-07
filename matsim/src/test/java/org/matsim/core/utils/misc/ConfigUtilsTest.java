/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.utils.misc;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

/**
 * @author mrieser
 */
public class ConfigUtilsTest {

	@Test
	public void testLoadConfig_filenameOnly() throws IOException {
		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		Assert.assertNotNull(config);
		Assert.assertEquals("test/scenarios/equil/network.xml", config.network().getInputFile());
	}

	@Test
	public void testLoadConfig_emptyConfig() throws IOException {
		Config config = new Config();
		Assert.assertNull(config.network());
		ConfigUtils.loadConfig(config, "test/scenarios/equil/config.xml");
		Assert.assertNotNull(config.network());
		Assert.assertEquals("test/scenarios/equil/network.xml", config.network().getInputFile());
	}

	@Test
	public void testLoadConfig_preparedConfig() throws IOException {
		Config config = new Config();
		config.addCoreModules();
		Assert.assertNotNull(config.network());
		Assert.assertNull(config.network().getInputFile());
		ConfigUtils.loadConfig(config, "test/scenarios/equil/config.xml");
		Assert.assertEquals("test/scenarios/equil/network.xml", config.network().getInputFile());
	}

	@Test
	public void testModifyPaths_missingSeparator() throws IOException {
		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		Assert.assertEquals("test/scenarios/equil/network.xml", config.network().getInputFile());
		ConfigUtils.modifyFilePaths(config, "/home/username/matsim");
		Assert.assertEquals("/home/username/matsim/test/scenarios/equil/network.xml", config.network().getInputFile());
	}

	@Test
	public void testModifyPaths_withSeparator() throws IOException {
		Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		Assert.assertEquals("test/scenarios/equil/network.xml", config.network().getInputFile());
		ConfigUtils.modifyFilePaths(config, "/home/username/matsim/");
		Assert.assertEquals("/home/username/matsim/test/scenarios/equil/network.xml", config.network().getInputFile());
	}

}
