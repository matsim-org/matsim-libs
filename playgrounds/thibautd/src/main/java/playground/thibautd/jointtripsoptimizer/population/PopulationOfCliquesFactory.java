/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationOfCliquesFactory.java
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

/**
 * @author thibautd
 */
public class PopulationOfCliquesFactory implements PopulationFactory {
	//not used
	//private final ScenarioWithCliques scenario;
	private final PopulationWithCliquesFactory factoryDelegate;

	public PopulationOfCliquesFactory(ScenarioWithCliques sc) {
		//this.scenario = sc;
		this.factoryDelegate = new PopulationWithCliquesFactory(sc);
	}

	/*
	 * =========================================================================
	 * clique methods
	 * =========================================================================
	 */
	public Clique createClique(Id id) {
		return new Clique(id);
	}

	/*
	 * =========================================================================
	 * PopulationFactory interface methods
	 * =========================================================================
	 */
	/**
	 * @return a clique
	 */
	@Override
	public Person createPerson(Id id) {
		return this.createClique(id);
	}

	/**
	 * @return a Joint plan, with clique initialized to null
	 */
	@Override
	public Plan createPlan() {
		return new JointPlan(null);
	}

	@Override
	public Activity createActivityFromCoord(String actType, Coord coord) {
		return this.factoryDelegate.createActivityFromCoord(actType, coord);
	}

	@Override
	public Activity createActivityFromLinkId(String actType, Id linkId) {
		return this.factoryDelegate.createActivityFromLinkId(actType, linkId);
	}

	@Override
	public Leg createLeg(String legMode) {
		return this.factoryDelegate.createLeg(legMode);
	}
}

