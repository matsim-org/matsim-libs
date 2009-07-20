/* *********************************************************************** *
 * project: org.matsim.*
 * KTIYear3ScoringFunctionFactoryTest.java
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

package playground.meisterk.org.matsim.scoring.ktiYear3;

import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.org.matsim.run.ktiYear3.KTIControler;
import playground.meisterk.org.matsim.scoring.ktiYear3.KTIYear3ScoringFunctionFactory.KTIScoringParameters;

public class KTIYear3ScoringFunctionFactoryTest extends MatsimTestCase {

	private Config config;
	
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig(this.getPackageInputDirectory() + "config.xml");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.config = null;
	}

	public void testKTIYear3ScoringFunctionFactory() {
		
		Module ktiConfigModule = this.config.getModule(KTIControler.KTI_CONFIG_MODULE_NAME);
		
		KTIYear3ScoringFunctionFactory testee = new KTIYear3ScoringFunctionFactory(this.config.charyparNagelScoring(), null, ktiConfigModule);
		
		KTIScoringParameters ktiScoringParameters = testee.getKtiScoringParameters();
		
		assertEquals(999.9, ktiScoringParameters.getConstBike());
		
	}
	
}
