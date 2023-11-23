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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;

/**
 * @author mrieser
 */
public class ScenarioUtilsTest {

	private final static Logger log = LogManager.getLogger(ScenarioUtilsTest.class);

	@Test
	public void testCreateScenario_nullConfig() {
		try {
			Scenario s = ScenarioUtils.createScenario(null);
			Assert.fail("expected NPE, but got none." + s.toString());
		}
		catch (NullPointerException e) {
			log.info("Catched expected NPE.", e);
			Assert.assertTrue("Message in NPE should not be empty.", e.getMessage().length() > 0);
		}
	}
}
