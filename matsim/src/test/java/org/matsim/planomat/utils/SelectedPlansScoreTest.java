/* *********************************************************************** *
 * project: org.matsim.*
 * SelectedPlansScoreChecker.java
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

package org.matsim.planomat.utils;

import java.util.HashMap;

import org.junit.Ignore;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.testcases.MatsimTestCase;

@Ignore
public class SelectedPlansScoreTest extends MatsimTestCase implements IterationEndsListener {

	private final HashMap<Id,Double> expectedPlanScores;
	private final int testIterationNumber;
	
	public SelectedPlansScoreTest(HashMap<Id, Double> expectedPlanScores, int testIterationNumber) {
		super();
		this.expectedPlanScores = expectedPlanScores;
		this.testIterationNumber = testIterationNumber;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		int iterationNumber = event.getIteration();
		if (iterationNumber == this.testIterationNumber) {
			
			for (Id personId : this.expectedPlanScores.keySet()) {
				Double expectedScore = this.expectedPlanScores.get(personId);
				Double actualScore = event.getControler().getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				if (expectedScore == null) {
					assertNull(actualScore);
				} else {
					assertEquals(
							"Unexpected score for selected plan of agent with id " + personId.toString() + ".", 
							expectedScore, 
							actualScore, 
							MatsimTestCase.EPSILON);
				}
			}
		}
	
	}

}
