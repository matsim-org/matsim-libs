package org.matsim.application.analysis.impact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mutable aggregation result used while streaming the standard MATSim analysis CSV files. */
final class ImpactAnalysisResult {

	// Linked maps keep output ordering reproducible while still allowing modes and pollutants to be discovered dynamically.
	final Map<String, ModeImpact> byMode = new LinkedHashMap<>();
	final Map<String, Map<String, Double>> emissions = new LinkedHashMap<>();
	final Map<String, PersonImpact> persons = new LinkedHashMap<>();
	boolean emissionsAvailable;
	double scoreSum;
	double scoredPersons;

	static final class ModeImpact {
		// Missing modes in one scenario are real computed zeros, not unavailable observations.
		static final ModeImpact EMPTY = new ModeImpact();
		double trips;
		double personDistanceMeters;
		double personTravelTimeSeconds;
		double vehicleLegs;
		double vehicleDistanceMeters;
		double vehicleTravelTimeSeconds;
	}

	static final class PersonImpact {
		// The ordered main-mode sequence is retained because aggregate mode totals cannot identify behavioral switchers.
		final List<String> modes = new ArrayList<>();
		double travelTimeSeconds;
		Double score;
	}
}
