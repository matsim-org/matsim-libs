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

package org.matsim.population.algorithms;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

/**
 * @author mrieser
 */
public class TripPlanMutateTimeAllocationTest {

	@Test
	public void testRun() {
		// setup population with one person
		PersonImpl person = (PersonImpl) PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		Activity act = PopulationUtils.createAndAddActivityFromCoord("home", new Coord((double) 0, (double) 0), plan);
		act.setEndTime(8.0 * 3600);
		plan.createAndAddLeg(TransportMode.transit_walk);
		Activity ptAct1 = PopulationUtils.createAndAddActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 100), plan);
		ptAct1.setMaximumDuration(0);
		plan.createAndAddLeg(TransportMode.pt);
		Activity ptAct2 = PopulationUtils.createAndAddActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 100), plan);
		ptAct2.setMaximumDuration(0);
		plan.createAndAddLeg(TransportMode.transit_walk);
		act = PopulationUtils.createAndAddActivityFromCoord("work", new Coord((double) 0, (double) 500), plan);
		act.setEndTime(16*3600);
		plan.createAndAddLeg(TransportMode.transit_walk);
		Activity ptAct3 = PopulationUtils.createAndAddActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 100), plan);
		ptAct3.setMaximumDuration(0);
		plan.createAndAddLeg(TransportMode.pt);
		Activity ptAct4 = PopulationUtils.createAndAddActivityFromCoord(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 100), plan);
		ptAct4.setMaximumDuration(0);
		plan.createAndAddLeg(TransportMode.transit_walk);
		PopulationUtils.createAndAddActivityFromCoord("work", new Coord((double) 0, (double) 500), plan);
		boolean affectingDuration = true ;

		TripPlanMutateTimeAllocation mutator =
				new TripPlanMutateTimeAllocation(
						new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ),
						3600.,
						affectingDuration, new Random(2011));
		mutator.run(plan);

		Assert.assertEquals(0.0, ptAct1.getMaximumDuration(), 1e-8);
		Assert.assertEquals(0.0, ptAct2.getMaximumDuration(), 1e-8);
		Assert.assertEquals(0.0, ptAct3.getMaximumDuration(), 1e-8);
		Assert.assertEquals(0.0, ptAct4.getMaximumDuration(), 1e-8);
	}
}
