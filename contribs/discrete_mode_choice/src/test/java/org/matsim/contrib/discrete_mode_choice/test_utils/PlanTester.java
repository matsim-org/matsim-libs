package org.matsim.contrib.discrete_mode_choice.test_utils;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class PlanTester {
	static public List<String> getModeChain(Plan plan) {
		List<String> modes = new LinkedList<>();

		for (PlanElement element : plan.getPlanElements()) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;
				modes.add(leg.getMode());
			}
		}

		return modes;
	}

	static public List<String> getModeChain(List<TripCandidate> candidates) {
		return candidates.stream().map(TripCandidate::getMode).collect(Collectors.toList());
	}
}
