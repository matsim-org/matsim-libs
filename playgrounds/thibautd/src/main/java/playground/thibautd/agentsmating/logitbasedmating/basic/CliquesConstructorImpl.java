/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesConstructorImpl.java
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.thibautd.agentsmating.greedysavings.FacilitiesFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.CliquesConstructor;
import playground.thibautd.agentsmating.logitbasedmating.framework.Mating;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;

/**
 * Default implementation of a clique constructor. It can only handle
 * couples of co-travelers: an UnsupportedOperationException will be thrown
 * in case matings with more than one passenger are presented.
 *
 * @author thibautd
 */
public class CliquesConstructorImpl implements CliquesConstructor {
	private final ScenarioImpl scenario;

	public CliquesConstructorImpl(
			final ScenarioImpl scenario) {
		this.scenario = scenario;
	}

	/**
	 * Modifies the individual plans according to mating information,
	 * so that they can be exported with the standard plan writer,
	 * and returns clique information.
	 * It will also modify the facilities in the scenario used at initialisation,
	 * so that shared rides are scored correctly.
	 *
	 * @param individualPopulation the population to process. the members
	 * of the population must be instances of PersonImpl or a subtype.
	 * it must be the population returned by the scenario used at construction
	 * @param matings the mating information to process
	 *
	 * @return the clique composition map.
	 */
	@Override
	public Map<Id, List<Id>> processMatings(
			final Population individualPopulation,
			final List<Mating> matings) {
		if (individualPopulation != scenario.getPopulation()) {
			throw new IllegalArgumentException("argument population must be the scenario one");
		}

		CliqueInformation cliqueInfo = new CliqueInformation( individualPopulation );
		PopulationActuator populationActuator = new PopulationActuator( scenario );

		for (Mating mating : matings) {
			cliqueInfo.mate(mating);
			populationActuator.handleMating(mating);
		}

		populationActuator.finish();

		return cliqueInfo.getCliques();
	}
}

class CliqueInformation {
	// links persons ids with clique composition, with possibility of doublettes
	private final Map<Id, List<Id>> cliques = new HashMap<Id, List<Id>>();
	private final List<List<Id>> registeredCliques = new ArrayList<List<Id>>();

	public CliqueInformation(
			final Population pop) {
		for (Id id : pop.getPersons().keySet()) {
			List<Id> clique = new ArrayList<Id>(1);
			clique.add( id );
			cliques.put(id, clique);
			registeredCliques.add( clique );
		}
	}

	public void mate(final Mating mating) {
		Id driver = mating.getDriver().getDecisionMaker().getPersonId();

		for (TripRequest passenger : mating.getPassengers()) {
			mate(driver, passenger.getDecisionMaker().getPersonId());
		}
	}

	public void mate(final Id mate1, final Id mate2) {
		List<Id> mates1 = getMates(mate1);
		List<Id> mates2 = getMates(mate2);

		// merge the cliques composition of mate1 and mate2
		mates1.addAll(mates2);
		// associate mate2 with the merged clique
		cliques.put(mate2, mates1);
		// remove the old mate2 clique from the memory
		registeredCliques.remove(mates2);
	}

	private List<Id> getMates(final Id person) {
		List<Id> mates = cliques.get(person);

		//if (mates == null) {
		//	mates = new ArrayList<Id>();
		//	mates.add(person);
		//	cliques.put(person, mates);
		//	registeredCliques.add(mates);
		//}

		return mates;
	}

	public Map<Id, List<Id>> getCliques() {
		Map<Id, List<Id>> effectiveCliques = new HashMap<Id, List<Id>>();
		long currentId = 0;

		for (List<Id> composition : registeredCliques) {
			List<Id> realComposition = cleanDoublettes(composition);
			effectiveCliques.put( new IdImpl( currentId++ ), realComposition );
		}

		return effectiveCliques;
	}

	private List<Id> cleanDoublettes(final List<Id> composition) {
		List<Id> cleanList = new ArrayList<Id>();

		for (Id id : composition) {
			if ( !cleanList.contains(id) ) {
				cleanList.add(id);
			}
		}

		return cleanList;
	}
}

class PopulationActuator {
	private final Map<Id, PlanActuator> plans =
		new HashMap<Id, PlanActuator>();
	private final Population population;
	private final FacilitiesFactory facilitiesFactory;
	private long rideCount = 0;

	public PopulationActuator(
			final ScenarioImpl scenario) {
		this.population = scenario.getPopulation();

		if (scenario.getConfig().facilities().getInputFile() != null) {
			facilitiesFactory = new FacilitiesFactory(
					scenario.getActivityFacilities(),
					scenario.getNetwork() );
		}
		else {
			facilitiesFactory = null;
		}
	}

	public void handleMating(
			final Mating mating) {
		if (mating.getPassengers().size() > 1) {
			throw new UnsupportedOperationException("cannot handle multi-passenger legs");
		}
		rideCount++;

		TripRequest driver = mating.getDriver();
		TripRequest passenger = mating.getPassengers().get(0);

		PlanActuator passengerActuator =
			getPlanActuator( passenger.getDecisionMaker().getPersonId() );
		List<PlanElement> elements = passengerActuator.getPlan().getPlanElements();
		int indexInPassengerPlan = passenger.getIndexInPlan();
		Activity origin = (Activity) elements.get(indexInPassengerPlan - 1);
		Activity destination = (Activity) elements.get(indexInPassengerPlan + 1);

		getPlanActuator(driver.getDecisionMaker().getPersonId()).addJointTrip(
				driver.getIndexInPlan(),
				rideCount,
				origin,
				destination,
				driver.getTripType());
		passengerActuator.addJointTrip(
				indexInPassengerPlan,
				rideCount,
				origin,
				destination,
				passenger.getTripType());
	}

	public void finish() {
		for (PlanActuator plan : plans.values()) {
			plan.modifyBackPlan();
		}
	}

	private PlanActuator getPlanActuator(
			final Id person) {
		PlanActuator actuator = plans.get(person);

		if (actuator == null) {
			actuator = new PlanActuator(
					population.getPersons().get(person).getSelectedPlan(),
					facilitiesFactory);
			plans.put(person, actuator);
		}

		return actuator;
	}
}

class PlanActuator {
	private final Plan plan;
	private final PlanElement[] planElements;
	private final FacilitiesFactory facilitiesFactory;

	public PlanActuator(
			final Plan plan,
			final FacilitiesFactory facilitiesFactory) {
		this.plan = plan;
		this.planElements = plan.getPlanElements().toArray(new PlanElement[0]);
		this.facilitiesFactory = facilitiesFactory;
	}

	public void addJointTrip(
			final int indexInPlan,
			final long jointTripNumber,
			final Activity origin,
			final Activity destination,
			final TripRequest.Type tripType) {
		this.planElements[indexInPlan] =
			new SharedRide(
					jointTripNumber,
					tripType,
					origin,
					destination);
	}

	public Plan getPlan() {
		return plan;
	}

	public void modifyBackPlan() {
		List<PlanElement> newPlanElements = new ArrayList<PlanElement>(planElements.length);

		for (PlanElement element : planElements) {
			if (element instanceof SharedRide) {
				SharedRide sharedRide = (SharedRide) element;

				String interMode, jointMode;

				switch (sharedRide.type) {
					case DRIVER:
						interMode = TransportMode.car;
						jointMode = TransportMode.car;
						break;
					case PASSENGER:
						interMode = TransportMode.walk;
						jointMode = JointActingTypes.PASSENGER;
						break;
					default:
						throw new RuntimeException("unknown enum value");
				}

				ActivityImpl pickUp = new ActivityImpl(
							sharedRide.puName,
							sharedRide.origin.getCoord(),
							sharedRide.origin.getLinkId());
				ActivityImpl dropOff =  new ActivityImpl(
							JointActingTypes.DROP_OFF,
							sharedRide.destination.getCoord(),
							sharedRide.destination.getLinkId());

				if ( !(facilitiesFactory == null) ) {
					pickUp.setFacilityId(
							facilitiesFactory.getPickUpDropOffFacility(
								sharedRide.origin.getLinkId()));
					dropOff.setFacilityId(
							facilitiesFactory.getPickUpDropOffFacility(
								sharedRide.destination.getLinkId()));
				}

				newPlanElements.add( new LegImpl(interMode) );
				newPlanElements.add( pickUp );
				newPlanElements.add( new LegImpl(jointMode) );
				newPlanElements.add( dropOff );
				newPlanElements.add( new LegImpl(interMode) );
			}
			else {
				newPlanElements.add(element);
			}
		}

		List<PlanElement> actualPlanElements = plan.getPlanElements();
		actualPlanElements.clear();
		actualPlanElements.addAll( newPlanElements );
	}

	/**
	 * "dummy" marker class
	 */
	private static class SharedRide implements PlanElement {
		public final String puName;
		public final TripRequest.Type type;
		public final Activity origin;
		public final Activity destination;

		public SharedRide(
				final long puNumber,
				final TripRequest.Type type,
				final Activity origin,
				final Activity destination) {
			this.puName = JointActingTypes.PICK_UP_BEGIN + JointActingTypes.PICK_UP_SPLIT_EXPR + puNumber;
			this.type = type;
			this.origin = origin;
			this.destination = destination;
		}
	}
}

