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
	private Map<String, Params> params = new LinkedHashMap<String, Params>();

	public Params getParams( final String mode ) {
		return params.get( mode );
	}

	public void addParams(
			final String mode,
			final double marginalUtilityOfDenivelation_m) {
		this.params.put(
				mode,
				new Params(
					mode,
					marginalUtilityOfDenivelation_m ) );
	}

	public static class Params {
		public final String mode;
		public final double marginalUtilityOfDenivelation_m;

		private Params(
				final String mode,
				final double marginalUtilityOfDenivelation_m) {
			this.mode = mode;
			this.marginalUtilityOfDenivelation_m = marginalUtilityOfDenivelation_m;
		}
	}
}

