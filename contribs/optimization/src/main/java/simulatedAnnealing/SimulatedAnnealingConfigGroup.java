/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package simulatedAnnealing;

import jakarta.validation.constraints.PositiveOrZero;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ReflectiveConfigGroup;
import simulatedAnnealing.temperature.TemperatureFunction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class SimulatedAnnealingConfigGroup extends ReflectiveConfigGroup {

	private static final Logger log = LogManager.getLogger(SimulatedAnnealingConfigGroup.class);


	private final static String NAME = "simulatedAnnealing";

	public enum ResetOption {temperatureOnly, solutionOnly, temperatureAndSolution}


	public SimulatedAnnealingConfigGroup() {
		super(NAME);
	}

	@Parameter
	@Comment("Cooling Temperature schedule. Defines the shape of the cooling gradient. {linear, exponential}")
	public TemperatureFunction.DefaultFunctions coolingSchedule = TemperatureFunction.DefaultFunctions.exponentialMultiplicative;// seconds

	@Parameter
	@Comment("Cooling parameter alpha. Used to control the speed of cooling. Should be >0. " +
			"Typical values 0.8 <= alpha <= 0.9 for exponential cooling. alpha > 1 for logarithmic cooling.")
	@PositiveOrZero
	public double alpha = 0.85;

	@Parameter
	@Comment("Initial temperature T0, should be >0.")
	@PositiveOrZero
	public double initialTemperature = 1000;

	@Parameter
	@Comment("Number of cooling cycles n.")
	@PositiveOrZero
	public int nCoolingCycles = Integer.MAX_VALUE;

	@Parameter
	@Comment("Final temperature Tn of the system. Should >=0.")
	@PositiveOrZero
	public double finalTemperature = 0;

	@Parameter
	@Comment("Number of simulated annealing iterations (!= MATSim iterations, see iterationRatio) spent at each temperature.")
	@PositiveOrZero
	public int iterationsPerTemperature = 3;

	@Parameter
	@Comment("Number of MATSim iterations for each simulated annealing iteration. This can make sense if other components" +
			"of MATSim have to react to a new solution first before evaluating it (e.g., DRT rebalancing)")
	@PositiveOrZero
	public int iterationRatio = 1;


	@Parameter
	@Comment("Tuning parameter k. See http://doi.org/10.5772/5560. If in doubt, set to 1.")
	@PositiveOrZero
	public double k = 1;

	@Parameter
	@Comment("Reset whenever a new best solution has been found. This may help to widen the search. See https://doi.org/10.3390/math9141625")
	public boolean resetUponBestSolution = false;

	@Parameter
	@Comment("Number of iterations without any improvement after which a reset occurs")
	public int deadEndIterationReset = Integer.MAX_VALUE;

	@Parameter
	@Comment("Defines whether only temperature, only the current solution or both are reset.")
	public ResetOption resetOption = ResetOption.temperatureOnly;


	@Parameter
	@Comment("Number of iterations after which there won't be any more resets.")
	public int lastResetIteration = Integer.MAX_VALUE;

	@Parameter
	@Comment("Last iteration after which there will be no more optimization and the solution will be fixed to the best" +
			"found solution.")
	public int lastIteration = Integer.MAX_VALUE;


	public static abstract class PerturbationParams extends ReflectiveConfigGroup implements MatsimParameters {

		public final static String SET_TYPE = "perturbationParams";

		public static final String PERTURBATION_IDENTIFIER = "identifier";
		public static final String PERTURBATION_WEIGHT = "weight";


		private String identifier;

		private double weight;

		public PerturbationParams(String identifier, double weight) {
			super(SET_TYPE);
			this.identifier = identifier;
			this.weight = weight;
		}

		public PerturbationParams(String identifier) {
			this(identifier, 1.);
		}

		/**
		 * {@value -- PERTURBATION_IDENTIFIER}
		 */
		@StringGetter(PERTURBATION_IDENTIFIER)
		public String getIdentifier() {
			return this.identifier;
		}

		/**
		 * {@value -- PERTURBATION_IDENTIFIER}
		 */
		@StringSetter(PERTURBATION_IDENTIFIER)
		public void setIdentifier(final String identifier) {
			testForLocked();
			this.identifier = identifier;
		}

		/**
		 * {@value -- PERTURBATION_IDENTIFIER}
		 */
		@StringGetter(PERTURBATION_WEIGHT)
		public double getWeight() {
			return this.weight;
		}

		/**
		 * {@value -- PERTURBATION_IDENTIFIER}
		 */
		@StringSetter(PERTURBATION_WEIGHT)
		public void setWeight(final double weight) {
			testForLocked();
			this.weight = weight;
		}
	}

	public void addPerturbationParams(final PerturbationParams params) {
		final PerturbationParams previous = this.getPerturbationParams(params.getIdentifier());

		if (previous != null) {
			log.info("perturbation parameters for identifier " + previous.getIdentifier()
						+ " were just overwritten.");

			final boolean removed = removeParameterSet(previous);
			if (!removed)
				throw new RuntimeException("problem replacing perturbator params ");
		}

		super.addParameterSet(params);
	}

	public PerturbationParams getPerturbationParams(final String identifier) {
		return this.getPerturbationParamsPerType().get(identifier);
	}

	public Map<String, PerturbationParams> getPerturbationParamsPerType() {
		final Map<String, PerturbationParams> map = new LinkedHashMap<>();

		for (PerturbationParams pars : getPerturbationParams()) {
			map.put(pars.getIdentifier(), pars);
		}

		return map;
	}

	public Collection<PerturbationParams> getPerturbationParams() {
		@SuppressWarnings("unchecked")
		Collection<PerturbationParams> collection = (Collection<PerturbationParams>) getParameterSets(
				PerturbationParams.SET_TYPE);
		for (PerturbationParams params : collection) {
			if (this.isLocked()) {
				params.setLocked();
			}
		}
		return collection;
	}
}
