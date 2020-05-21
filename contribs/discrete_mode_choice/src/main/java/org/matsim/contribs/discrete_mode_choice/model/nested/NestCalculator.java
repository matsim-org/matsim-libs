package org.matsim.contribs.discrete_mode_choice.model.nested;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;

public class NestCalculator {
	private final Map<Nest, Collection<UtilityCandidate>> candidates = new HashMap<>();
	private final NestStructure structure;

	private final Logger logger = Logger.getLogger(NestCalculator.class);

	public NestCalculator(NestStructure structure) {
		this.structure = structure;

		for (Nest nest : structure.getNests()) {
			candidates.put(nest, new LinkedList<>());
		}
	}

	public void addCandidate(NestedUtilityCandidate candidate) {
		candidates.get(candidate.getNest()).add(candidate);
	}

	private double guardExp(double value) {
		if (value < -300.0) {
			logger.warn("Utility value <-300.0 was truncated. Check your model configuration!");
			value = -300.0;
		} else if (value > 300.0) {
			logger.warn("Utility value >300.0 was truncated. Check your model configuration!");
			value = 300.0;
		}

		return Math.exp(value);
	}

	public double calculateDenominator(Nest nest) {
		double denominator = 0.0;

		for (UtilityCandidate candidate : candidates.get(nest)) {
			denominator += guardExp(nest.getScaleParameter() * candidate.getUtility());
		}

		for (Nest child : structure.getChildren(nest)) {
			denominator += guardExp(nest.getScaleParameter() * calculateExpectedUtility(child));
		}

		return denominator;
	}

	public double calculateLogSumTerm(Nest nest) {
		return Math.log(calculateDenominator(nest));
	}

	public double calculateExpectedUtility(Nest nest) {
		return calculateLogSumTerm(nest) / nest.getScaleParameter();
	}

	/**
	 * Calculates the probability of choosing the candidate, given that its nest is
	 * chosen.
	 */
	public double calculateConditionalProbability(NestedUtilityCandidate candidate) {
		double denominator = calculateDenominator(candidate.getNest());
		return guardExp(candidate.getNest().getScaleParameter() * candidate.getUtility()) / denominator;
	}

	/**
	 * Calculates the probability of choosing a nest, given that its parent nest is
	 * chosen.
	 */
	public double calculateConditionalProbability(Nest nest) {
		Nest parent = structure.getParent(nest);
		double denominator = calculateDenominator(parent);
		return guardExp(parent.getScaleParameter() * calculateExpectedUtility(nest)) / denominator;
	}

	/**
	 * Calculates the total probability of a candidate (by chaining the conditional
	 * probabilities of its nests).
	 */
	public double calculateProbability(NestedUtilityCandidate candidate) {
		double probability = calculateConditionalProbability(candidate);

		Nest nest = candidate.getNest();

		while (nest != structure.getRoot()) {
			probability *= calculateConditionalProbability(nest);
			nest = structure.getParent(nest);
		}

		return probability;
	}
}