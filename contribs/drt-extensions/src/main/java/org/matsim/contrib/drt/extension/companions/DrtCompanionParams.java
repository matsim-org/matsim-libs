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

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.matsim.core.utils.misc.StringUtils;

/**
 * @author steffenaxer
 */
public class DrtCompanionParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
	private static final char DEFAULT_COLLECTION_DELIMITER = ',';
	public static final String SET_NAME = "companions";
	private static final String DRT_COMPANION_SAMPLING_WEIGHTS_NAME = "drtCompanionSamplingWeights";

	private List<Double> drtCompanionSamplingWeights = List.of(0.7, 0.3);

	public DrtCompanionParams() {
		super(SET_NAME);

	}

	@StringGetter(DRT_COMPANION_SAMPLING_WEIGHTS_NAME)
	private String getDrtCompanionSamplingWeightsAsString() {

		return this.drtCompanionSamplingWeights.stream().map(Object::toString)
				.collect(Collectors.joining(String.valueOf(DEFAULT_COLLECTION_DELIMITER)));
	}

	@StringSetter(DRT_COMPANION_SAMPLING_WEIGHTS_NAME)
	private void setDrtCompanionSamplingWeightsAsStringList(String values) {

		if (values.equals("")) {
			throw new IllegalArgumentException("DrtCompanionParams can not be empty, please specify at least two values!");
		}

		this.drtCompanionSamplingWeights = Arrays.stream(StringUtils.explode(values, DEFAULT_COLLECTION_DELIMITER))
				.map(Double::parseDouble).collect(Collectors.toList());
	}

	public List<Double> getDrtCompanionSamplingWeights() {
		return this.drtCompanionSamplingWeights;
	}

	public void setDrtCompanionSamplingWeights(List<Double> values) {
		this.drtCompanionSamplingWeights = values;
	}


	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(DRT_COMPANION_SAMPLING_WEIGHTS_NAME,
				"Weights to sample an additional drt passenger. E.g. 70 % +0 pax, 30 % +1 pax. Please specify at least two values.");
		return map;
	}

	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (drtCompanionSamplingWeights.isEmpty()) {
			throw new IllegalStateException("drtCompanionSamplingWeights can not be empty. Please check configurations");
		}
	}


}
