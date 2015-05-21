/* *********************************************************************** *
 * project: org.matsim.*
 * WeakSelectorFactory.java
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
package org.matsim.contrib.socnetsim.usage.replanning.strategies;

import org.matsim.contrib.socnetsim.framework.replanning.GroupLevelPlanSelectorFactory;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.WeakSelector;

/**
 * @author thibautd
 */
public class WeakSelectorFactory implements GroupLevelPlanSelectorFactory {
	private final GroupLevelPlanSelectorFactory delegateFactory;
	private final PlanLinkIdentifier weakPlanLinkIdentifier;
	
	public WeakSelectorFactory(
			final PlanLinkIdentifier weakPlanLinkIdentifier,
			final GroupLevelPlanSelectorFactory delegateFactory ) {
		this.delegateFactory = delegateFactory;
		this.weakPlanLinkIdentifier = weakPlanLinkIdentifier;
	}

	@Override
	public GroupLevelPlanSelector createSelector() {
		return new WeakSelector(
				weakPlanLinkIdentifier,
				delegateFactory.createSelector( ) );
	}
}

