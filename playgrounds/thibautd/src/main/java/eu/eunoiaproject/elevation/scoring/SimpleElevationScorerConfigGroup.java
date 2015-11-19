/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleElevationScorerConfigGroup.java
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

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.Collections;
import java.util.Set;

/**
 * A config group to configure a {@link SimpleElevationScorer}.
 * It deliberately does not define a <tt>GROUP_NAME</tt> constant,
 * so that several of those groups can be created for different modes.
 * @author thibautd
 */
public class SimpleElevationScorerConfigGroup extends ReflectiveConfigGroup {
	private Set<String> modes = Collections.emptySet();
	private double marginalUtilityOfDenivelation_m = 0;

	public SimpleElevationScorerConfigGroup(final String name) {
		super(name);
	}

	public Set<String> getModes() {
		return this.modes;
	}

	@StringGetter( "modes" )
	private String getStringModes() {
		return CollectionUtils.setToString( modes );
	}

	public void setModes(final Set<String> modes) {
		this.modes = modes;
	}

	@StringSetter( "modes" )
	private void setStringModes(final String modes) {
		this.modes = CollectionUtils.stringToSet( modes );
	}

	@StringGetter( "marginalUtilityOfDenivelation_m" )
	public double getMarginalUtilityOfDenivelation_m() {
		return this.marginalUtilityOfDenivelation_m;
	}

	@StringSetter( "marginalUtilityOfDenivelation_m" )
	public void setMarginalUtilityOfDenivelation_m(
			final double marginalUtilityOfDenivelation_m) {
		this.marginalUtilityOfDenivelation_m = marginalUtilityOfDenivelation_m;
	}
}

