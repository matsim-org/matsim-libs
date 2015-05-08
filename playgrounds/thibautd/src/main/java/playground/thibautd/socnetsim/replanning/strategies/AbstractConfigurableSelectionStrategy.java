/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractConfigurableSelectionStrategy.java
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
package playground.thibautd.socnetsim.replanning.strategies;

import java.util.Map;

import org.matsim.core.config.Config;

import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public abstract class AbstractConfigurableSelectionStrategy implements GroupPlanStrategyFactory {
	@Inject
	private Map<String, Provider<GroupLevelPlanSelector>> factoryRegistry;

	protected GroupPlanStrategy instantiateStrategy( final Config config ) {
		final GroupReplanningConfigGroup conf = (GroupReplanningConfigGroup)
			config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

		return new GroupPlanStrategy( factoryRegistry.get( conf.getSelectorForModification() ).get() );
	}
}

