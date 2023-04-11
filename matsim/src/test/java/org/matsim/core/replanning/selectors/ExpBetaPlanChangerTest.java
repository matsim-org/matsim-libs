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

package org.matsim.core.replanning.selectors;

/**
 * Tests for {@link ExpBetaPlanSelector}.
 *
 * @author mrieser
 */
public class ExpBetaPlanChangerTest extends AbstractPlanSelectorTest {

	@Override
	protected ExpBetaPlanChanger getPlanSelector() {
		return new ExpBetaPlanChanger(2.0);
	}

	// TODO write specific tests for ExpBetaPlanChanger, see ExpBetaPlanSelectorTest for examples

}
