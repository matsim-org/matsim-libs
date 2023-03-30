/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package simulatedannealing.perturbation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import simulatedannealing.SimulatedAnnealingConfigGroup;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class ChainedPerturbatorParams extends SimulatedAnnealingConfigGroup.PerturbationParams {

	private static final Logger log = LogManager.getLogger(ChainedPerturbatorParams.class);

	public final static String IDENTIFIER = "chainedPerturbator";

	@Parameter
	@Comment("Maximum number of perturbations per perturbator iteration.")
	public int maxPerturbations = 10;

	@Parameter
	@Comment("Minimum number of perturbations per perturbator iteration.")
	public int minPerturbations = 1;


	public ChainedPerturbatorParams() {
		super(IDENTIFIER);
	}

	public void addPerturbationParams(final SimulatedAnnealingConfigGroup.PerturbationParams params) {
		final SimulatedAnnealingConfigGroup.PerturbationParams previous = this.getPerturbationParams(params.getIdentifier());

		if (previous != null) {
			log.info("perturbation parameters for identifier " + previous.getIdentifier()
					+ " were just overwritten.");

			final boolean removed = removeParameterSet(previous);
			if (!removed)
				throw new RuntimeException("problem replacing perturbator params ");
		}

		super.addParameterSet(params);
	}

	public SimulatedAnnealingConfigGroup.PerturbationParams getPerturbationParams(final String identifier) {
		return this.getPerturbationParamsPerType().get(identifier);
	}

	public Map<String, SimulatedAnnealingConfigGroup.PerturbationParams> getPerturbationParamsPerType() {
		final Map<String, SimulatedAnnealingConfigGroup.PerturbationParams> map = new LinkedHashMap<>();

		for (SimulatedAnnealingConfigGroup.PerturbationParams pars : getPerturbationParams()) {
			map.put(pars.getIdentifier(), pars);
		}

		return map;
	}

	public Collection<SimulatedAnnealingConfigGroup.PerturbationParams> getPerturbationParams() {
		@SuppressWarnings("unchecked")
		Collection<SimulatedAnnealingConfigGroup.PerturbationParams> collection = (Collection<SimulatedAnnealingConfigGroup.PerturbationParams>) getParameterSets(
				SimulatedAnnealingConfigGroup.PerturbationParams.SET_TYPE);
		for (SimulatedAnnealingConfigGroup.PerturbationParams params : collection) {
			if (this.isLocked()) {
				params.setLocked();
			}
		}
		return collection;
	}
}
