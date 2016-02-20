/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.kai.integration.ptTutorialProblem;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class PtTutorialProblem {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void test() {
		String fileName = utils.getPackageInputDirectory() + "/0.config.xml";

		Config config = ConfigUtils.loadConfig( fileName ) ;
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		
		config.controler().setLastIteration(1);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles);
		// otherwise the config_before is gone again


		final String configFileBefore = utils.getOutputDirectory() + "/config_before.xml";
		new ConfigWriter(config).write(configFileBefore);
		Controler controler = new Controler(config);
		
		controler.run();
		final String configFileAfter = utils.getOutputDirectory() + "/config_after.xml";
		new ConfigWriter(config).write(configFileAfter);
		
		long before = CRCChecksum.getCRCFromFile(configFileBefore) ;
		long after = CRCChecksum.getCRCFromFile(configFileAfter) ;
		
		Assert.assertEquals(before, after);
	}
}
