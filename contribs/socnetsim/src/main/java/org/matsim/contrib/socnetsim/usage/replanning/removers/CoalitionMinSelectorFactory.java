/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.socnetsim.usage.replanning.removers;

import com.google.inject.Inject;
import org.matsim.core.config.Config;
import org.matsim.contrib.socnetsim.framework.replanning.removers.AbstractDumbRemoverFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.InverseScoreWeight;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;

public class CoalitionMinSelectorFactory extends AbstractDumbRemoverFactory {
	
	@Inject
	public CoalitionMinSelectorFactory( final Config conf ) {
		super( getMaxPlansPerAgent( conf ) );
	}

	private static int getMaxPlansPerAgent(Config conf) {
		final GroupReplanningConfigGroup group = (GroupReplanningConfigGroup) conf.getModule( GroupReplanningConfigGroup.GROUP_NAME );
		return group.getMaxPlansPerAgent();
	}

	@Override
	public GroupLevelPlanSelector createSelector() {
		return new CoalitionSelector( new InverseScoreWeight() );
	}
}
