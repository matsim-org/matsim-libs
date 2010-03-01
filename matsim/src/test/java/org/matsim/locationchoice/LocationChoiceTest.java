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

package org.matsim.locationchoice;

import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.testcases.MatsimTestCase;

public class LocationChoiceTest extends MatsimTestCase {

	private LocationChoice initialize() {
		Initializer initializer = new Initializer();
    initializer.init(this);
		return new LocationChoice(initializer.getControler().getNetwork(),
				initializer.getControler(), (initializer.getControler().getScenario()).getKnowledges());
	}

	public void testConstructorandInitLocal() {
		LocationChoice locationchoice = initialize();
		assertNotNull("controler not initialized", locationchoice.getControler());
		assertNotNull("network not initialized", locationchoice.getNetwork());
	}

	public void testGetPlanAlgoInstance() {
		LocationChoice locationchoice = initialize();
		locationchoice.setConstrained(false);
		assertEquals(locationchoice.getPlanAlgoInstance().getClass(), RandomLocationMutator.class);
		locationchoice.setConstrained(true);
		assertEquals(locationchoice.getPlanAlgoInstance().getClass(), LocationMutatorwChoiceSet.class);
	}

	public void testFinish() {
		LocationChoice locationchoice = initialize();
		locationchoice.getPlanAlgoInstance();
		assertEquals(false, locationchoice.getPlanAlgoInstances().isEmpty());
		locationchoice.prepareReplanning();
		locationchoice.finishReplanning();
		assertEquals(true , locationchoice.getPlanAlgoInstances().isEmpty());
	}

}