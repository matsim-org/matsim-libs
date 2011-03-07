/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationWithCliquesFactory.java
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
package playground.thibautd.jointtripsoptimizer.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;

import org.matsim.api.core.v01.Scenario;

import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;

/**
 * @author thibautd
 */
public class PopulationWithCliquesFactory implements PopulationFactory {
	private PopulationFactory populationFactoryDelegate;

	public PopulationWithCliquesFactory(Scenario sc) {
		populationFactoryDelegate = new PopulationFactoryImpl(sc);
	}

	/*
	 * =========================================================================
	 * interface methods
	 * =========================================================================
	 */
	@Override
	public Person createPerson(Id id) {
		return populationFactoryDelegate.createPerson(id);
	}

	@Override
	public Plan createPlan() {
		return new PlanImpl();
	}

	//TODO: implement those methods with person reference
	@Override
	public Activity createActivityFromCoord(String actType, Coord coord) {
		return new JointActivity(actType, coord, null);
	}

	@Override
	public Activity createActivityFromLinkId(String actType, Id linkId) {
		return new JointActivity(actType, linkId, null);
	}

	@Override
	public Leg createLeg(String legMode) {
		return new JointLeg(legMode, null);
	}
}

