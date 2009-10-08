/* *********************************************************************** *
 * project: org.matsim.*
 * BKickRouterTestIATBR.java
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

package playground.benjamin;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestCase;









/**
 * Tests the routing of the BKickIncomeControler2
 * @author dgrether
 *
 */
public class BKickRouterTestIATBR extends MatsimTestCase {

	/*package*/ final static Id id1 = new IdImpl("1");
	/*package*/ final static Id id2 = new IdImpl("2");
	/*package*/ final static Id id3 = new IdImpl("3");
	
		
	
	public void testSingleIterationIncomeScoring() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "configRouterTestIATBR.xml");
		String netFileName = this.getClassInputDirectory() + "network.xml";
		config.network().setInputFile(netFileName);
		config.plans().setInputFile(this.getClassInputDirectory() + "plansRouterTest.xml");
		//hh loading
		config.scenario().setUseHouseholds(true);
		config.households().setInputFile(this.getClassInputDirectory() + "households.xml");
}
}