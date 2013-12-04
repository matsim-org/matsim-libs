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

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryRegistry;

/**
 * @author thibautd
 */
public abstract class AbstractConfigurableSelectionStrategy implements GroupPlanStrategyFactory {
	private final GroupPlanStrategyFactoryRegistry factoryRegistry;

	public AbstractConfigurableSelectionStrategy( final GroupPlanStrategyFactoryRegistry factoryRegistry ) {
		this.factoryRegistry = factoryRegistry;
	}

	protected final GroupPlanStrategy instantiateStrategy( final ControllerRegistry registry ) {
		final GroupReplanningConfigGroup conf = (GroupReplanningConfigGroup)
			registry.getScenario().getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME );

		return new GroupPlanStrategy(
				factoryRegistry.createSelector(
					conf.getSelectorForModification(),
					registry ) );
	}
}

