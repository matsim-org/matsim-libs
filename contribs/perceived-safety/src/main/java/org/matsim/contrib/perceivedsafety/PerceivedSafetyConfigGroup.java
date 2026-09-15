/* *********************************************************************** *
* project: org.matsim.*
* *********************************************************************** *
*                                                                         *
* copyright       : (C) ${2024} by the members listed in the COPYING,     *
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

package org.matsim.contrib.perceivedsafety;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ptzouras @author.
 */
public final class PerceivedSafetyConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = LogManager.getLogger(PerceivedSafetyConfigGroup.class);

	public static final String GROUP_NAME = "PerceivedSafety";

	private static final String INPUT_PERCEIVED_SAFETY_THRESHOLD = "inputPerceivedSafetyThreshold_m";

	private int inputPerceivedSafetyThreshold;

	public PerceivedSafetyConfigGroup(){
		super(GROUP_NAME);
		}

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(INPUT_PERCEIVED_SAFETY_THRESHOLD, "inputPerceivedSafetyThreshold");
		return map;
	}

	@StringSetter(INPUT_PERCEIVED_SAFETY_THRESHOLD)
	public void setInputPerceivedSafetyThresholdPerM(final int value) {
		this.inputPerceivedSafetyThreshold = value;
	}
	@StringGetter(INPUT_PERCEIVED_SAFETY_THRESHOLD)
	public int getInputPerceivedSafetyThresholdPerM() {
		return this.inputPerceivedSafetyThreshold;
	}

	public PerceivedSafetyModeParams getOrCreatePerceivedSafetyModeParams(String modeName) {
		PerceivedSafetyModeParams modeParams = getModes().get(modeName);
		if (modeParams == null) {
			modeParams = new PerceivedSafetyModeParams(modeName);
			addParameterSet(modeParams);
		}
		return modeParams;
	}

	public Map<String, PerceivedSafetyModeParams> getModes() {
		@SuppressWarnings("unchecked")
		final Collection<PerceivedSafetyModeParams> modes = (Collection<PerceivedSafetyModeParams>) getParameterSets(PerceivedSafetyModeParams.SET_TYPE);
		final Map<String, PerceivedSafetyModeParams> map = new LinkedHashMap<>();

		for (PerceivedSafetyModeParams pars : modes) {
			if (this.isLocked()) {
				pars.setLocked();
			}
			map.put(pars.getMode(), pars);
		}
		if (this.isLocked()) {
			return Collections.unmodifiableMap(map);
		} else {
			return map;
		}
	}

	public void addModeParams(final PerceivedSafetyModeParams params) {
		final PerceivedSafetyModeParams previous = this.getModes().get(params.getMode());

		if (previous != null) {
			final boolean removed = removeParameterSet(previous);
			if (!removed)
				throw new IllegalStateException("problem replacing mode params ");
			log.warn("perceived safety mode parameters for mode {} were just overwritten.", previous.getMode());
		}
		super.addParameterSet(params);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// CLASSES
	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * perceived safety mode params. Built parallel to ScoringConfigGroup.ModeParams
	 */
	public static class PerceivedSafetyModeParams extends ReflectiveConfigGroup implements MatsimParameters {
		static final String SET_TYPE = "perceivedSafetyModeParams";
		public static final String MODE_PARAM = "mode";

		private static final String INPUT_PERCEIVED_SAFETY = "marginalUtilityOfPerceivedSafety_m";
		private static final String INPUT_PERCEIVED_SAFETY_SD = "marginalUtilityOfPerceivedSafety_m_sd";
		private static final String INPUT_DMAX = "dMax_m";
		private static final String PERCEIVED_SAFETY_NET_ATTR_NAME = "perceivedSafetyNetworkAttributeName";


		private String mode = null;
		private double marginalUtilityOfPerceivedSafetyPerM = 0.;
		private double marginalUtilityOfPerceivedSafetyPerMSd = 0.;
		private double dMax = 0.;
		private String networkAttributeName = null;

		public PerceivedSafetyModeParams(final String mode) {
			super(SET_TYPE);
//			the following method sets mode attr + network attribute name for the mode
			setMode(mode);
		}

		PerceivedSafetyModeParams() {
			super(SET_TYPE);
		}

		@Override
		public Map<String, String> getComments() {
			final Map<String, String> map = super.getComments();
			map.put(INPUT_PERCEIVED_SAFETY, "[utils/m] marginal utility of perceived safety per meter.");
			map.put(INPUT_PERCEIVED_SAFETY_SD, "standard deviation of marginal utility of perceived safety per meter.");
			map.put(INPUT_DMAX, "[m] The maximum relevant length for perceived safety calculation of a given mode. dMax is used to normalize the distanceBasedPerceivedSafety." +
					"This is useful e.g. for links with low perceived safety in the first 50m of a link of length=200m. It is assumed that travelling the whole 200m does not feel 4x as unsafe as the first 50m." +
					"The effect of distance based perceived safety saturates.");
			map.put(PERCEIVED_SAFETY_NET_ATTR_NAME, "name of perceived safety link attribute.");

			return map;
		}

		@StringSetter(MODE_PARAM)
		public PerceivedSafetyModeParams setMode(final String mode) {
			testForLocked();
			this.mode = mode;
			this.networkAttributeName = mode + GROUP_NAME;
			return this;
		}
		@StringGetter(MODE_PARAM)
		public String getMode() {
			return mode;
		}
		@StringSetter(INPUT_PERCEIVED_SAFETY)
		public void setMarginalUtilityOfPerceivedSafetyPerM(final double value) {
			// PROSTHESE MONTE CARLO SIMULATION
			this.marginalUtilityOfPerceivedSafetyPerM = value;
		}
		@StringGetter(INPUT_PERCEIVED_SAFETY)
		public double getMarginalUtilityOfPerceivedSafetyPerM() {
			return this.marginalUtilityOfPerceivedSafetyPerM;
		}
		@StringSetter(INPUT_PERCEIVED_SAFETY_SD)
		public void setMarginalUtilityOfPerceivedSafetyPerMSd(final double value) {
			// PROSTHESE MONTE CARLO SIMULATION
			this.marginalUtilityOfPerceivedSafetyPerMSd = value;
		}
		@StringGetter(INPUT_PERCEIVED_SAFETY_SD)
		public double getMarginalUtilityOfPerceivedSafetyPerMSd() {
			return this.marginalUtilityOfPerceivedSafetyPerMSd;
		}
		@StringSetter(INPUT_DMAX)
		public void setDMaxPerM(final double value) {
			this.dMax = value;
		}
		@StringGetter(INPUT_DMAX)
		public double getDMaxPerM() {
			return this.dMax;
		}
		@StringSetter(PERCEIVED_SAFETY_NET_ATTR_NAME)
		public void setNetworkAttributeName(final String value) {
			this.networkAttributeName = value;
		}
		@StringGetter(PERCEIVED_SAFETY_NET_ATTR_NAME)
		public String getNetworkAttributeName() {
			return this.networkAttributeName;
		}
	}
}
