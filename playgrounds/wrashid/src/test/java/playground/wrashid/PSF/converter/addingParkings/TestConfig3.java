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

package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;
import playground.wrashid.PSF.ParametersPSF;

public class TestConfig3 extends MatsimTestCase {

	public void testConfig(){
		String configFilePath = getPackageInputDirectory() + "config3.xml";
		Config config = loadConfig(configFilePath);
		ConfigUtils.addOrGetModule(config, ParametersPSF.PSF_MODULE, ParametersPSF.class);
		
		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime );
		// added by me to fix the test.  If you normally run with the default setting (now tryEndTimeThenDuration), I would suggest to remove
		// the above line and adapt the test outcome.  Kai, feb'14
		

		Controler controler = new Controler(config);
        controler.getConfig().controler().setCreateGraphs(false);

        new OptimizedChargerTestGeneral(controler).optimizedChargerTest();
	}

}
