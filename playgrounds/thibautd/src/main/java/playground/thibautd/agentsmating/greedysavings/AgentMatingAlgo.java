/* *********************************************************************** *
 * project: org.matsim.*
 * AgentMatingAlgo.java
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
package playground.thibautd.agentsmating.greedysavings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;

/**
 * 
 * @author thibautd
 */
public class AgentMatingAlgo {

	private static final String HOME_REGEXP = "h.*";
	private static final String WORK_REGEXP = "w.*";
	private final boolean onlyMateDrivers = false;

	private static final double PU_DUR = 0d;
	private static final double DO_DUR = 0d;

	private final FacilitiesFactory facilitiesFactory;

	private final AgentTopology agentTopology;
	private final Population population;
	private final List<Person> unaffectedAgents;
	private final Comparator<Tuple<? extends Object, Double>> comparator =
		new PassengerComparator();
	private final List<Tuple<Person, Person>> carPoolingAffectations =
		new ArrayList<Tuple<Person, Person>>(1000);

	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	public AgentMatingAlgo(
			final Population population,
			final Network network,
			final double acceptableDistance,
			final ActivityFacilitiesImpl facilities) {
		this.agentTopology = 
			new AgentTopology(network, population, acceptableDistance, onlyMateDrivers);
		this.population = population;
		this.unaffectedAgents = new ArrayList<Person>(population.getPersons().values());

		if (facilities != null) {
			this.facilitiesFactory = new FacilitiesFactory(facilities, network);
		}
		else {
			this.facilitiesFactory = null;
		}
	}

	/*
	 * =========================================================================
	 * core method
	 * =========================================================================
	 */
	/**
	 * computes all information necessary to use getters.
	 */
	public void run() {
		List<Tuple<Person, Double>> neighbors = null;
		Tuple<Person, Double> passenger;

		for (Person driver : this.population.getPersons().values()) {
			try {
				neighbors = this.agentTopology.getNeighbors(driver);
			} catch (AgentTopology.UnknownPersonException e) {
				//the person has been affected: jump to the next
				continue;
			}

			if ((neighbors == null) || (neighbors.size() == 0)) {
				continue;
			}

			passenger = Collections.max(neighbors, this.comparator);

			// Do not mate if negative savings (ie if it increases the overall
			// travelled distance)
			if (passenger.getSecond() > 0) {
				affectAndRemoveFromTopology(driver, passenger.getFirst());
			}
		}
	}

	private void affectAndRemoveFromTopology(
			final Person driver,
			final Person passenger) {
		this.carPoolingAffectations.add(
				new Tuple<Person, Person>(driver, passenger));

		this.agentTopology.remove(driver);
		this.agentTopology.remove(passenger);

		this.unaffectedAgents.remove(driver);
		this.unaffectedAgents.remove(passenger);
	}

	/*
	 * =========================================================================
	 * getters
	 * =========================================================================
	 */
	/**
	 * @return a population were the plans define joint plans for the cliques
	 * returned by getCliques.
	 * This is done by modifying the initial population and returning it.
	 *
	 * Should be encapsulated in a PopulationWithCliques, but there is currently
	 * no XML writer for such a data structure.
	 *
	 * TODO
	 */
	public Population getPopulation() {
		cleanPlansInPopulation();

		PickUpNameFactory factory = new PickUpNameFactory();

		// modify the plans of the affected agents
		for (Tuple<Person, Person> couple : this.carPoolingAffectations) {
			createJointLegs(
					couple.getFirst().getSelectedPlan().getPlanElements(),
					couple.getSecond().getSelectedPlan().getPlanElements(),
					factory);
		}

		return this.population;
	}

	private void createJointLegs(
			final List<PlanElement> planDriver,
			final List<PlanElement> planPassenger,
			final PickUpNameFactory factory) {
		// keep a copy of the old plans
		List<PlanElement> driverElements =
			new ArrayList<PlanElement>(planDriver);
		List<PlanElement> passengerElements =
			new ArrayList<PlanElement>(planPassenger);

		// reinitialize the actual plans
		planDriver.clear();
		planPassenger.clear();

		// create the first joint trip
		planUntilWork(planDriver, planPassenger, driverElements, passengerElements, factory);

		if ( sharedReturnIsFeasible(driverElements, passengerElements) ) {
			planSharedReturn(planDriver, planPassenger, driverElements, passengerElements, factory);
		}
		else {
			for (PlanElement pe : driverElements.subList(3, driverElements.size())) {
				planDriver.add(pe);
			}
			for (PlanElement pe : passengerElements.subList(3, passengerElements.size())) {
				planPassenger.add(pe);
			}
		}
	}

	/**
	 * Shared return coonsidered as feasible only if the beginning of the plan
	 * is of H-W-H form.
	 */
	private boolean sharedReturnIsFeasible(
			final List<PlanElement> driverElements,
			final List<PlanElement> passengerElements) {
		return ((Activity) driverElements.get(4)).getType().matches(HOME_REGEXP) &&
			((Activity) passengerElements.get(4)).getType().matches(HOME_REGEXP);
	}

	private void planSharedReturn (
			final List<PlanElement> planDriver,
			final List<PlanElement> planPassenger,
			final List<PlanElement> driverElements,
			final List<PlanElement> passengerElements,
			final PickUpNameFactory factory) {
		String puName = factory.createName();
		ActivityImpl passengerHome = (ActivityImpl) passengerElements.get(4);
		ActivityImpl passengerWork = (ActivityImpl) passengerElements.get(2);
		ActivityImpl act;

		// /////////// DRIVER //////////////
		// access leg
		planDriver.add(new LegImpl(TransportMode.car));
		// PU
		act = new ActivityImpl(
					puName, 
					passengerHome.getCoord(),
					passengerHome.getLinkId());
		act.setMaximumDuration(PU_DUR);
		setFacility(act, passengerHome.getLinkId());
		planDriver.add(act);

		// shared leg
		planDriver.add(new LegImpl(TransportMode.car));
		// DO
		act = new ActivityImpl(
					JointActingTypes.DROP_OFF, 
					passengerWork.getCoord(),
					passengerWork.getLinkId());
		act.setMaximumDuration(DO_DUR);
		setFacility(act, passengerWork.getLinkId());
		planDriver.add(act);

		// egress leg
		planDriver.add(new LegImpl(TransportMode.car));
		//home 
		planDriver.add(driverElements.get(4));

		// /////////// PASSENGER //////////////
		// access leg
		planPassenger.add(new LegImpl(TransportMode.walk));
		// PU
		act = new ActivityImpl(
					puName, 
					passengerHome.getCoord(),
					passengerHome.getLinkId());
		act.setMaximumDuration(PU_DUR);
		setFacility(act, passengerHome.getLinkId());
		planPassenger.add(act);
				
		// shared leg
		planPassenger.add(new LegImpl(JointActingTypes.PASSENGER));
		// DO
		act = new ActivityImpl(
					JointActingTypes.DROP_OFF, 
					passengerWork.getCoord(),
					passengerWork.getLinkId());
		act.setMaximumDuration(DO_DUR);
		setFacility(act, passengerWork.getLinkId());
		planPassenger.add(act);

			// egress leg
		planPassenger.add(new LegImpl(TransportMode.walk));
		// home
		planPassenger.add(passengerElements.get(4));

		// remaining of the plans
		if (driverElements.size() > 5) {
			for (PlanElement pe : driverElements.subList(5, driverElements.size())) {
				planDriver.add(pe);
			}
		}
		if (passengerElements.size() > 5) {
			for (PlanElement pe : passengerElements.subList(5, passengerElements.size())) {
				planPassenger.add(pe);
			}
		}
	}

	private void planUntilWork(
			final List<PlanElement> planDriver,
			final List<PlanElement> planPassenger,
			final List<PlanElement> driverElements,
			final List<PlanElement> passengerElements,
			final PickUpNameFactory factory) {
		String puName = factory.createName();
		ActivityImpl passengerHome = (ActivityImpl) passengerElements.get(0);
		ActivityImpl passengerWork = (ActivityImpl) passengerElements.get(2);
		ActivityImpl act;

		// /////////// DRIVER //////////////
		//home
		planDriver.add(driverElements.get(0));
		// access leg
		planDriver.add(new LegImpl(TransportMode.car));
		// PU
		act = new ActivityImpl(
					puName, 
					passengerHome.getCoord(),
					passengerHome.getLinkId());
		act.setMaximumDuration(PU_DUR);
		setFacility(act, passengerHome.getLinkId());
		planDriver.add(act);
				
		// shared leg
		planDriver.add(new LegImpl(TransportMode.car));
		// DO
		act = new ActivityImpl(
					JointActingTypes.DROP_OFF, 
					passengerWork.getCoord(),
					passengerWork.getLinkId());
		act.setMaximumDuration(DO_DUR);
		setFacility(act, passengerWork.getLinkId());
		planDriver.add(act);
				
		// egress leg
		planDriver.add(new LegImpl(TransportMode.car));
		// work
		planDriver.add(driverElements.get(2));
		
		// /////////// PASSENGER //////////////
		// home
		planPassenger.add(passengerElements.get(0));
		// access leg
		planPassenger.add(new LegImpl(TransportMode.walk));
		// PU
		act = new ActivityImpl(
					puName, 
					passengerHome.getCoord(),
					passengerHome.getLinkId());
		act.setMaximumDuration(PU_DUR);
		setFacility(act, passengerHome.getLinkId());
		planPassenger.add(act);
				
		// shared leg
		planPassenger.add(new LegImpl(JointActingTypes.PASSENGER));
		// DO
		act = new ActivityImpl(
					JointActingTypes.DROP_OFF, 
					passengerWork.getCoord(),
					passengerWork.getLinkId());
		act.setMaximumDuration(DO_DUR);
		setFacility(act, passengerWork.getLinkId());
		planPassenger.add(act);
				
		// egress leg
		planPassenger.add(new LegImpl(TransportMode.walk));
		// work
		planPassenger.add(passengerElements.get(2));
	}

	private void setFacility(final ActivityImpl act, final Id linkId) {
		if (this.facilitiesFactory != null) {
			act.setFacilityId(
					this.facilitiesFactory.getPickUpDropOffFacility(linkId));
		}
	}

	/**
	 * removes all non selected plans from the agentDBs.
	 */
	private void cleanPlansInPopulation() {
		for (Person person : this.population.getPersons().values()) {
			((PersonImpl) person).removeUnselectedPlans();
		}
	}

	/**
	 * @return clique pertenancy information, in a format compatible with the
	 * XML clique writer (could change)
	 */
	public Map<Id, List<Id>> getCliques() {
		Map<Id, List<Id>> output = new HashMap<Id, List<Id>>();
		IdFactory factory = new IdFactory();
		List<Id> currentClique;

		for (Tuple<Person, Person> couple : this.carPoolingAffectations) {
			currentClique = new ArrayList<Id>(2);
			currentClique.add(couple.getFirst().getId());
			currentClique.add(couple.getSecond().getId());

			output.put(factory.createId(), currentClique);
		}

		for (Person person : this.unaffectedAgents) {
			currentClique = new ArrayList<Id>(1);
			currentClique.add(person.getId());

			output.put(factory.createId(), currentClique);
		}

		return output;
	}

	/*
	 * =========================================================================
	 * classes
	 * =========================================================================
	 */
	/**
	 * Comparator aimed at classing passenger in ascending order according to
	 * their savings value.
	 * That means that this comparator considers a passenger A "greater than" 
	 * another passenger B if A's saving value is greater than the one of B.
	 */
	private class PassengerComparator 
			implements Comparator<Tuple<? extends Object, Double>> {

		public PassengerComparator() {}

		@Override
		public int compare(
				final Tuple<? extends Object, Double> arg0,
				final Tuple<? extends Object, Double> arg1) {
			double val1 = arg0.getSecond();
			double val2 = arg1.getSecond();

			return Double.compare(val1, val2);
		}
	}

	/**
	 * Creates a series of unique Ids.
	 */
	private class IdFactory {
		private long lastId = 0;

		public Id createId() {
			lastId++;
			return new IdImpl(lastId);
		}
	}

	private class PickUpNameFactory {
		private long lastN = 0;

		public String createName() {
			lastN++;
			return
				JointActingTypes.PICK_UP_BEGIN +
				JointActingTypes.PICK_UP_SPLIT_EXPR +
				lastN;
		}
	}
}

