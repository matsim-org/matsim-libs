package org.matsim.application.analysis.impact;

import java.util.Map;

/** Person-level comparison between reference and policy, matched by the stable MATSim person id. */
record ImpactComparison(int matched, int referenceOnly, int policyOnly, int remainers, int switchers,
		double referenceTravelTimeSeconds, double policyTravelTimeSeconds, double meanScoreDelta) {

	static ImpactComparison compare(ImpactAnalysisResult reference, ImpactAnalysisResult policy) {
		int matched = 0, remainers = 0, switchers = 0, policyOnly = 0, matchedScores = 0;
		double referenceTime = 0., policyTime = 0., scoreDelta = 0.;
		for (Map.Entry<String, ImpactAnalysisResult.PersonImpact> entry : policy.persons.entrySet()) {
			// Person id is the stable MATSim key across scenarios. Row order is deliberately irrelevant.
			ImpactAnalysisResult.PersonImpact base = reference.persons.get(entry.getKey());
			if (base == null) { policyOnly++; continue; }
			ImpactAnalysisResult.PersonImpact current = entry.getValue();
			matched++;
			// Equality of the complete mode sequence is a person-level behavioral definition. It does not claim that
			// individual trips are matched; a future trip-transition module needs its own semantic trip key.
			if (base.modes.equals(current.modes)) remainers++; else switchers++;
			referenceTime += base.travelTimeSeconds;
			policyTime += current.travelTimeSeconds;
			if (base.score != null && current.score != null) {
				// All comparisons consistently use policy minus reference. Scores remain MATSim utility units.
				scoreDelta += current.score - base.score;
				matchedScores++;
			}
		}
		return new ImpactComparison(matched, reference.persons.size() - matched, policyOnly, remainers, switchers,
			referenceTime, policyTime, matchedScores == 0 ? Double.NaN : scoreDelta / matchedScores);
	}
}
