/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.companions;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.common.util.WeightedRandomSelection;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.passenger.PassengerGroupIdentifier;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.matsim.contrib.drt.extension.companions.DrtCompanionUtils.DRT_COMPANION_AGENT_PREFIX;

/**
 * @author Steffen Axer
 */
final class DrtCompanionRideGenerator implements BeforeMobsimListener, AfterMobsimListener {

	private static final Logger LOG = LogManager.getLogger(DrtCompanionRideGenerator.class);
	public static final String DRT_COMPANION_TYPE = "drtCompanion";
	private final Scenario scenario;
	private final String drtMode;
	private final MainModeIdentifier mainModeIdentifier;
	private final int maxCapacity;

	private final Set<Id<Person>> companionAgentIds = new HashSet<>();
	private WeightedRandomSelection<Integer> sampler;

	private final Map<Id<PassengerGroupIdentifier.PassengerGroup>,List<GroupTrip>> passengerGroups = new HashMap<>();

	private int passengerGroupIdentifier = 0; // Should be unique over the entire simulation

	private final AtomicInteger personIdentifierSuffix = new AtomicInteger(0);

	DrtCompanionRideGenerator(final String drtMode, final MainModeIdentifier mainModeIdentifier,
							  final FleetSpecification fleet,
							  final Scenario scenario,
							  final DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup) {
		this.scenario = scenario;
		this.mainModeIdentifier = mainModeIdentifier;
		this.drtMode = drtMode;
		this.maxCapacity = fleet.getVehicleSpecifications()
				.values()
				.stream()
				.mapToInt(DvrpVehicleSpecification::getCapacity)
				.max()
				.orElse(0);;
		installSampler(drtWithExtensionsConfigGroup);
	}

	private String getCompanionPrefix(String drtMode) {
		return DRT_COMPANION_AGENT_PREFIX + "_" + drtMode;
	}

	private void installSampler(DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup) {
		if (!drtWithExtensionsConfigGroup.getDrtCompanionParams().orElseThrow().getDrtCompanionSamplingWeights()
			.isEmpty()) {
			this.sampler = DrtCompanionUtils.createIntegerSampler(
				drtWithExtensionsConfigGroup.getDrtCompanionParams().orElseThrow()
					.getDrtCompanionSamplingWeights());
		} else {
			throw new IllegalStateException(
				"drtCompanionSamplingWeights are empty, please check your DrtCompanionParams");
		}
	}

	private Id<PassengerGroupIdentifier.PassengerGroup> getGroupIdentifier()
	{
		return Id.create(this.passengerGroupIdentifier, PassengerGroupIdentifier.PassengerGroup.class);
	}

	record GroupTrip(TripStructureUtils.Trip trip, Id<Person> personId) {}

	private void addCompanionAgents() {
		Collection<Person> companions = new ArrayList<>();
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
				if (this.drtMode.equals(mainMode)) {
					int additionalCompanions = sampler.select();

					// Initial person travels now in a group
					Id<PassengerGroupIdentifier.PassengerGroup> currentGroupIdentifier = getGroupIdentifier();
					DrtCompanionUtils.setPassengerGroupIdentifier(person, currentGroupIdentifier);

					// Add person to group map
					this.passengerGroups.computeIfAbsent(currentGroupIdentifier, k-> new ArrayList<>()).add(new GroupTrip(trip,person.getId()));

					int groupSize = additionalCompanions + 1;
					int currentGroupSize = 1; // Initial person

					// Add companions
					for (int i = 0; i < additionalCompanions; i++) {

						// currentGroupSize+1 exceeds maxCapacity, create a new passengerGroupIdentifier
						if(currentGroupSize+1>this.maxCapacity)
						{
							passengerGroupIdentifier++; // increment due to vehicle capacity
							currentGroupSize=0;
							currentGroupIdentifier = getGroupIdentifier();
						}

						// Bypass passengerGroupIdentifierId to each group member
						companions.add(createCompanionAgent(mainMode, person, trip, trip.getOriginActivity(),
							trip.getDestinationActivity(), i, groupSize, personIdentifierSuffix.getAndIncrement(), currentGroupIdentifier));
						currentGroupSize++;
					}
					passengerGroupIdentifier++; // increment due to a new trip
				}
			}
		}
		companions.forEach(p -> {
			this.scenario.getPopulation().addPerson(p);
			this.companionAgentIds.add(p.getId());
		});

		validateGroups();
		LOG.info("Added # {} drt companion agents for mode {}", companions.size(), this.drtMode);
	}

	private void validateGroups() {
		this.passengerGroups.values().forEach(g -> {
			GroupTrip representative = g.get(0);

			Id<Link> fromLinkId = PopulationUtils.decideOnLinkIdForActivity(
				representative.trip.getOriginActivity(),scenario);
			Id<Link> toLinkId = PopulationUtils.decideOnLinkIdForActivity(
				representative.trip.getDestinationActivity(),scenario);

			Preconditions.checkNotNull(fromLinkId);
			Preconditions.checkNotNull(toLinkId);

			Preconditions.checkArgument(g.stream().allMatch(
				a -> PopulationUtils.decideOnLinkIdForActivity(
				a.trip.getOriginActivity(),scenario).equals(fromLinkId)));

			Preconditions.checkArgument(g.stream().allMatch(
				a -> PopulationUtils.decideOnLinkIdForActivity(
				a.trip.getDestinationActivity(),scenario).equals(toLinkId)));

			Preconditions.checkArgument(g.size() <= this.maxCapacity);
		});
	}

	private Person createCompanionAgent(final String drtMode, final Person originalPerson, final TripStructureUtils.Trip trip,
										final Activity fromActivity, final Activity toActivity, final int groupPart, final int groupSize, final int personIdentifier, final Id<PassengerGroupIdentifier.PassengerGroup> passengerGroupIdentifierId) {
		String prefix = getCompanionPrefix(drtMode);
		String companionId = prefix + "_" + originalPerson.getId().toString() + "_" + personIdentifier;
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(companionId));
		DrtCompanionUtils.setDRTCompanionType(person, DRT_COMPANION_TYPE);

		// Copy attributes from person
		AttributesUtils.copyAttributesFromTo(originalPerson, person);

		// Ensure that we have a timed end
		Activity copyFromActivity = PopulationUtils.createActivity(fromActivity);
		if (copyFromActivity.getEndTime().isUndefined()) {
			copyFromActivity.setEndTime(trip.getLegsOnly().get(0).getDepartureTime().seconds());
		}

		Plan plan = PopulationUtils.createPlan();
		plan.getPlanElements().add(copyFromActivity);
		plan.getPlanElements().addAll(trip.getTripElements());
		plan.getPlanElements().add(toActivity);

		person.addPlan(plan);

		// Add group information to trip
		DrtCompanionUtils.setAdditionalGroupPart(person, groupPart);
		DrtCompanionUtils.setAdditionalGroupSize(person, groupSize);
		DrtCompanionUtils.setPassengerGroupIdentifier(person, passengerGroupIdentifierId);

		// Add companion to group map
		this.passengerGroups.computeIfAbsent(passengerGroupIdentifierId, k-> new ArrayList<>()).add(new GroupTrip(trip,person.getId()));
		return person;
	}

	private void removeCompanionAgents() {
		int counter = 0;

		for (Id<Person> drtCompanion : companionAgentIds) {
			this.scenario.getPopulation().getPersons().remove(drtCompanion);
			counter++;
		}

		// Reset
		companionAgentIds.clear();
		passengerGroups.clear();
		personIdentifierSuffix.set(0);
		LOG.info("Removed # {} drt companion agents", counter);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		this.removeCompanionAgents();
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.addCompanionAgents();
	}

}
