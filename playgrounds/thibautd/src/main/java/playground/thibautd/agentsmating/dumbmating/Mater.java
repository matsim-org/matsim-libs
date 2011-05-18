/* *********************************************************************** *
 * project: org.matsim.*
 * Mater.java
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
package playground.thibautd.agentsmating.dumbmating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.householdsfromcensus.CliquesWriter;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;

/**
 * Mates agents and creates joint plans.
 * Meant to be used on files were:
 * <ul>
 * <li> all agents have car available
 * <li> plan structure is of type H-W-H
 * </ul>
 *
 * This is a quick implementation: no consistency check is done!
 *
 * @author thibautd
 */
public class Mater {

	private final Population population;
	private final Scenario scenario;
	private final Map<Id, List<Id>> cliques = new HashMap<Id, List<Id>>();
	private final TripChaining chainingMode;
	private final int cliquesSize;
	private final IdFactory idFactory = new IdFactory();
	private final PuNameFactory puFactory = new PuNameFactory();

	/**
	 * Defines the way shared rides are created.
	 *
	 * ALL_TOGETHER: all trips with same OD are joint
	 * ONE_BY_ONE: the driver serves the passengers in a "shuttle" way
	 */
	public enum TripChaining {
		ALL_TOGETHER,
		ONE_BY_ONE
	}

	public Mater(
			final Scenario scenario,
			final TripChaining chainingMode,
			final int cliquesSize) {
		this.population = scenario.getPopulation();
		this.scenario = scenario;
		this.chainingMode = chainingMode;
		this.cliquesSize = cliquesSize;
	}

	/**
	 * Returns the mating, and modifies plans in the population.
	 */
	public Map<Id, List<Id>> run() {
		List<Person> clique = new ArrayList<Person>(cliquesSize);
		int count = 0;

		this.cliques.clear();

		for (Person person : this.population.getPersons().values()) {
			if (count < this.cliquesSize) {
				count++;
				clique.add(person);
			}
			else {
				processClique(clique);
				clique.clear();
				clique.add(person);
				count = 1;
			}
		}

		return this.cliques;
	}

	// /////////////////////////////////////////////////////////////////////////
	// run helpers
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Takes a list of persons and:
	 * -creates the according clique
	 * -modifies the plans
	 */
	private void processClique(final List<Person> clique) {
		addClique(clique);
		setPlans(clique);
	}

	private void addClique(final List<Person> clique) {
		List<Id> newClique = new ArrayList<Id>(this.cliquesSize);

		for (Person person : clique) {
			newClique.add(person.getId());
		}

		this.cliques.put(this.idFactory.createId(), newClique);
	}

	private void setPlans(final List<Person> clique) {
		switch (this.chainingMode) {
			case ALL_TOGETHER :
				setAllTogetherPlan(clique);
				break;
			case ONE_BY_ONE :
				setShuttlePlan(clique);
				break;
		}
	}
	
	private void setAllTogetherPlan(final List<Person> clique) {
		String firstPuName = this.puFactory.createName();
		String secondPuName = this.puFactory.createName();
		Tuple<ActivityImpl, ActivityImpl> firstOD = getFirstOD(clique.get(1));
		Tuple<ActivityImpl, ActivityImpl> secondOD = getSecondOD(clique.get(1));

		putJointTrips(
				clique.get(0).getSelectedPlan().getPlanElements(),
				TransportMode.car,
				firstPuName,
				secondPuName,
				firstOD,
				secondOD);

		for (int i=1; i < clique.size(); i++) {
			putJointTrips(
					clique.get(i).getSelectedPlan().getPlanElements(),
					JointActingTypes.PASSENGER,
					firstPuName,
					secondPuName,
					firstOD,
					secondOD);
		}
	}

	private void setShuttlePlan(final List<Person> clique) {
		Person driver = clique.get(0);
		List<Person> couple = new ArrayList<Person>(2);
		couple.add(driver);
		couple.add(driver);

		for ( Person passenger : clique.subList(1, clique.size()) ) {
			couple.set(1, passenger);
			setAllTogetherPlan(couple);
		}
	}

	private Tuple<ActivityImpl, ActivityImpl> getFirstOD(final Person passenger) {
		List<PlanElement> plan = passenger.getSelectedPlan().getPlanElements();
		return new Tuple<ActivityImpl, ActivityImpl>(
				(ActivityImpl) plan.get(0),
				(ActivityImpl) plan.get(2));
	}

	private Tuple<ActivityImpl, ActivityImpl> getSecondOD(final Person passenger) {
		List<PlanElement> plan = passenger.getSelectedPlan().getPlanElements();
		int size = plan.size();
		return new Tuple<ActivityImpl, ActivityImpl>(
				(ActivityImpl) plan.get(size - 3),
				(ActivityImpl) plan.get(size - 1));
	}

	/**
	 * replaces the first and the last trips by joint trips.
	 */
	private void putJointTrips(
			final List<PlanElement> plan,
			final String mode,
			final String firstPuName,
			final String secondPuName,
			final Tuple<ActivityImpl,ActivityImpl> firstOD,
			final Tuple<ActivityImpl,ActivityImpl> secondOD) {
		String accessMode = (mode.equals(TransportMode.car) ?
				TransportMode.car : TransportMode.walk);
		List<PlanElement> oldPlan = new ArrayList<PlanElement>(plan);
		ActivityImpl act;

		plan.clear();
		// first act
		plan.add(oldPlan.get(0));

		// joint trip
		plan.add(new LegImpl(accessMode));
		act = new ActivityImpl(
					firstPuName,
					firstOD.getFirst().getCoord(),
					firstOD.getFirst().getLinkId());
		act.setMaximumDuration(0d);
		plan.add(act);
		plan.add(new LegImpl(mode));
		act = new ActivityImpl(
					JointActingTypes.DROP_OFF,
					firstOD.getSecond().getCoord(),
					firstOD.getSecond().getLinkId());
		act.setMaximumDuration(0d);
		plan.add(act);
		plan.add(new LegImpl(accessMode));

		// middle
		plan.addAll( oldPlan.subList(2, oldPlan.size() - 2) );

		// joint trip
		plan.add(new LegImpl(accessMode));
		act = new ActivityImpl(
					secondPuName,
					secondOD.getFirst().getCoord(),
					secondOD.getFirst().getLinkId());
		act.setMaximumDuration(0d);
		plan.add(act);
		plan.add(new LegImpl(mode));
		act = new ActivityImpl(
					JointActingTypes.DROP_OFF,
					secondOD.getSecond().getCoord(),
					secondOD.getSecond().getLinkId());
		act.setMaximumDuration(0d);
		plan.add(act);
		plan.add(new LegImpl(accessMode));

		// last act
		plan.add(oldPlan.get(oldPlan.size() - 1));

	}

	// /////////////////////////////////////////////////////////////////////////
	// IO
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @param prefix the beginning of the file names, path included.
	 */
	public void write(final String prefix) {
		PopulationWriter popWriter =
			new PopulationWriter(population, scenario.getNetwork()); 
		popWriter.write(prefix+"Population.xml.gz");
		CliquesWriter cliqueWriter = new CliquesWriter(this.cliques);
		try {
			cliqueWriter.writeFile(prefix+"Cliques.xml.gz");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// helper classes
	// /////////////////////////////////////////////////////////////////////////
	private class IdFactory {
		private int id = 0;

		public Id createId() {
			id++;
			return new IdImpl(id);
		}
	}

	private class PuNameFactory {
		private static final String PREFIX = 
			JointActingTypes.PICK_UP_BEGIN +
			JointActingTypes.PICK_UP_SPLIT_EXPR;
		private int n = 0;

		public String createName() {
			n++;
			return PREFIX + n;
		}
	}
}

