package org.matsim.contribs.discrete_mode_choice.components.readers;

import java.util.HashMap;
import java.util.Map;

public class ApolloParameters {
	private Map<String, Double> parameters = new HashMap<>();

	public ApolloParameters(Map<String, Double> parameters) {
		this.parameters.putAll(parameters);
	}

	public double getParameter(String name) {
		if (!parameters.containsKey(name)) {
			throw new IllegalStateException(String.format("Apollo parameter not found: %s", name));
		}

		return parameters.get(name);
	}
}
