/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.locationchoice.constrained;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.locationchoice.Initializer;
import org.matsim.testcases.MatsimTestCase;

public class ManageSubchainsTest extends MatsimTestCase {

	public void testPrimarySecondaryActivityFound() {
		Initializer initializer = new Initializer();
		initializer.init(this);
		ManageSubchains manager = new ManageSubchains();

		Plan plan = initializer.getControler().getPopulation().getPersons().get(new IdImpl("1")).getSelectedPlan();
		ActivityImpl act = ((PlanImpl) plan).getFirstActivity();
		LegImpl leg = ((PlanImpl) plan).getNextLeg(act);
		manager.primaryActivityFound(act, leg);
		assertEquals(act, manager.getSubChains().get(0).getFirstPrimAct());

		act = ((PlanImpl) plan).getNextActivity(leg);
		manager.secondaryActivityFound(act, ((PlanImpl) plan).getNextLeg(act));
		assertEquals(act, manager.getSubChains().get(0).getSlActs().get(0));
	}
}