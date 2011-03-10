/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.scenario;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ScenarioLoaderImplTest {

	@Rule public MatsimTestUtils util = new MatsimTestUtils();

	@Test
	public void testLoadScenario_loadTransitData() {
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(util.getClassInputDirectory() + "transitConfig.xml");
		Assert.assertEquals(0, ((ScenarioImpl) sl.getScenario()).getTransitSchedule().getTransitLines().size());
		Assert.assertEquals(0, ((ScenarioImpl) sl.getScenario()).getTransitSchedule().getFacilities().size());
		sl.loadScenario();
		Assert.assertEquals(1, ((ScenarioImpl) sl.getScenario()).getTransitSchedule().getTransitLines().size());
		Assert.assertEquals(2, ((ScenarioImpl) sl.getScenario()).getTransitSchedule().getFacilities().size());
	}
}
