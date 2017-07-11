package org.matsim.contrib.opdyts.example.networkparameters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;

import floetteroed.opdyts.DecisionVariableRandomizer;

class NetworkParametersRandomizer implements DecisionVariableRandomizer<NetworkParameters> {

	// -------------------- MEMBERS --------------------

	private final double relativeChange = 0.1;

	private final double changeProba = 2.0 / 3.0;

	private final int numberOfVariations;

	// -------------------- CONSTRUCTION --------------------

	NetworkParametersRandomizer(final int numberOfVariations) {
		this.numberOfVariations = numberOfVariations;
	}

	// ---------- IMPLEMENTATION OF DecisionVariableRandomizer ----------

	private double rndSgn() {
		if (MatsimRandom.getRandom().nextBoolean()) {
			return +1.0;
		} else {
			return -1.0;
		}
	}

	@Override
	public List<NetworkParameters> newRandomVariations(NetworkParameters decisionVariable) {

		final List<NetworkParameters> result = new ArrayList<>();

		for (int cnt = 0; cnt < this.numberOfVariations; cnt += 2) {

			final Map<Link, LinkParameters> variation1 = new LinkedHashMap<>();
			final Map<Link, LinkParameters> variation2 = new LinkedHashMap<>();

			for (Map.Entry<Link, LinkParameters> entry : decisionVariable.getLinkParameters().entrySet()) {
				final Link link = entry.getKey();
				final LinkParameters original = entry.getValue();

				if (MatsimRandom.getRandom().nextDouble() < this.changeProba) {

					final double deltaFreespeed = this.rndSgn() * this.relativeChange * original.getFreespeed();
					final double deltaFlowCapacity = this.rndSgn() * this.relativeChange * original.getFlowCapacity();
					final double deltaNofLanes = this.rndSgn() * this.relativeChange * original.getNofLanes();

					variation1.put(link, new LinkParameters(original.getFreespeed() - deltaFreespeed,
							original.getFlowCapacity() - deltaFlowCapacity, original.getNofLanes() - deltaNofLanes));
					variation2.put(link, new LinkParameters(original.getFreespeed() + deltaFreespeed,
							original.getFlowCapacity() + deltaFlowCapacity, original.getNofLanes() + deltaNofLanes));
				}
			}

			result.add(new NetworkParameters(variation1));
			result.add(new NetworkParameters(variation2));
		}

		return result;
	}
}
