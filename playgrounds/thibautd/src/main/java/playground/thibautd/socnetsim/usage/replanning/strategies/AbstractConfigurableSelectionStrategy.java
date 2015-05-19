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
package playground.thibautd.socnetsim.usage.replanning.strategies;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.core.config.Config;
import playground.thibautd.socnetsim.framework.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.usage.replanning.GroupReplanningConfigGroup;

import java.util.Map;

/**
 * @author thibautd
 */
public abstract class AbstractConfigurableSelectionStrategy implements Provider<GroupPlanStrategy> {
	@Inject
	private Map<String, Provider<GroupLevelPlanSelector>> factoryRegistry;

	protected GroupPlanStrategy instantiateStrategy( final Config config ) {
		final GroupReplanningConfigGroup conf = (GroupReplanningConfigGroup)
			config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

		return new GroupPlanStrategy( factoryRegistry.get( conf.getSelectorForModification() ).get() );
	}
}

