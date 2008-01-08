/* *********************************************************************** *
 * project: org.matsim.*
 * BestPlanSelectorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.replanning.selectors;

import org.apache.log4j.Logger;
import org.matsim.config.Config;

/**
 * Tests for {@link ExpBetaPlanSelector}.
 *
 * @author mrieser
 */
public class ExpBetaPlanChangerTest extends AbstractPlanSelectorTest {

	private final static Logger log = Logger.getLogger(RandomPlanSelectorTest.class);
	private Config config = null;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.config = loadConfig(null); // required for planCalcScore.beta to be defined
	}

	@Override
	protected PlanSelectorI getPlanSelector() {
		return new ExpBetaPlanChanger();
	}

	// TODO write specific tests for ExpBetaPlanChanger, see ExpBetaPlanSelectorTest for examples

}
