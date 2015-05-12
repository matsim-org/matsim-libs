/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleElevationScorerParameters.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.elevation.scoring;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.core.api.internal.MatsimParameters;

/**
 * @author thibautd
 */
public class SimpleElevationScorerParameters implements MatsimParameters {
	private final Map<String, Params> params = new LinkedHashMap<String, Params>();

	public Params getParams( final String mode ) {
		return params.get( mode );
	}

	public void addParams(
			final String mode,
			final double marginalUtilityOfUphillDenivelation_m,
			final double marginalUtilityOfDownhillDenivelation_m) {
		this.params.put(
				mode,
				new Params(
					mode,
					marginalUtilityOfUphillDenivelation_m,
					marginalUtilityOfDownhillDenivelation_m) );
	}

	public static class Params {
		public final String mode;
		public final double marginalUtilityOfUphillDenivelation_m;
		public final double marginalUtilityOfDownhillDenivelation_m;

		private Params(
				final String mode,
				final double marginalUtilityOfUphillDenivelation_m,
				final double marginalUtilityOfDownhillDenivelation_m) {
			this.mode = mode;
			this.marginalUtilityOfUphillDenivelation_m = marginalUtilityOfUphillDenivelation_m;
			this.marginalUtilityOfDownhillDenivelation_m = marginalUtilityOfDownhillDenivelation_m;
		}
	}
}

