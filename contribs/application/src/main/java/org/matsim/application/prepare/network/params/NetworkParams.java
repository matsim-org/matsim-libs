package org.matsim.application.prepare.network.params;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Object containing parameters for a model. Can be used to serialize and deserialize parameters.
 */
final class NetworkParams {

	double f;

	@JsonIgnore
	Map<String, double[]> params = new HashMap<>();

	/**
	 * Used by jackson
	 */
	public NetworkParams() {
	}

	public NetworkParams(double f) {
		this.f = f;
	}

	@JsonAnyGetter
	public double[] getParams(String type) {
		return params.get(type);
	}

	@JsonAnySetter
	public void setParams(String type, double[] params) {
		this.params.put(type, params);
	}

	public boolean hasParams() {
		return !params.isEmpty();
	}

	@Override
	public String toString() {
		if (f == 0)
			return "Request{" + params.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue().length).collect(Collectors.joining(",")) + '}';

		return "Request{f=" + f + "}";
	}
}
