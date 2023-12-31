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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.common.util.WeightedRandomSelection;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

/**
 * @author Steffen Axer
 */
public final class DrtCompanionRideGenerator implements BeforeMobsimListener, AfterMobsimListener {

	private static final Logger LOG = LogManager.getLogger(DrtCompanionRideGenerator.class);
	public final static String DRT_COMPANION_AGENT_PREFIX = "COMPANION";
	public static final String DRT_COMPANION_TYPE = "drtCompanion";

	private final Scenario scenario;
	private final String drtModes;
	private final MainModeIdentifier mainModeIdentifier;

	private Set<Id<Person>> companionAgentIds = new HashSet<>();
	private WeightedRandomSelection<Integer> sampler;

	DrtCompanionRideGenerator(final String drtMode, final MainModeIdentifier mainModeIdentifier,
							  final Scenario scenario, final Network network,
							  final DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup) {
		this.scenario = scenario;
		this.mainModeIdentifier = mainModeIdentifier;
		this.drtModes = drtMode;
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

	void addCompanionAgents() {
		int identifier = 0;
		HashMap<String, Integer> drtCompanionAgents = new HashMap<>();
		Collection<Person> companions = new ArrayList<>();
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
				if (this.drtModes.equals(mainMode)) {

					int additionalCompanions = sampler.select();

					for (int i = 0; i < additionalCompanions; i++) {
						int currentCounter = drtCompanionAgents.getOrDefault(mainMode, 0);
						currentCounter++;
						drtCompanionAgents.put(mainMode, currentCounter);
						int groupSize = additionalCompanions + 1;
						int groupPart = i;

						companions.add(createCompanionAgent(mainMode, person, trip, trip.getOriginActivity(),
								trip.getDestinationActivity(), groupPart, groupSize, identifier));
						identifier++;
					}
				}
			}
		}
		companions.forEach(p -> {
			this.scenario.getPopulation().addPerson(p);
			this.companionAgentIds.add(p.getId());
		});

		for (Map.Entry<String, Integer> drtModeEntry : drtCompanionAgents.entrySet()) {
			LOG.info("Added # {} drt companion agents for mode {}", drtModeEntry.getValue(), drtModeEntry.getKey());
		}
	}

	private Person createCompanionAgent(String drtMode, Person originalPerson, TripStructureUtils.Trip trip,
										Activity fromActivity, Activity toActivity, int groupPart, int groupSize, int identifier) {
		String prefix = getCompanionPrefix(drtMode);
		String companionId = prefix + "_" + originalPerson.getId().toString() + "_" + identifier;
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

		return person;
	}

	void removeCompanionAgents() {
		int counter = 0;

		for (Id<Person> drtCompanion : companionAgentIds) {
			this.scenario.getPopulation().getPersons().remove(drtCompanion);
			counter++;
		}
		companionAgentIds.clear();
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
