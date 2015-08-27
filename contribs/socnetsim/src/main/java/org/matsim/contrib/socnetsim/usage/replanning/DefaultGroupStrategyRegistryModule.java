/* *********************************************************************** *
 * project: org.matsim.*
 * GroupStrategyManagerModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.usage.replanning;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.contrib.socnetsim.framework.replanning.ExtraPlanRemover;
import org.matsim.contrib.socnetsim.framework.replanning.GroupPlanStrategy;
import org.matsim.contrib.socnetsim.framework.replanning.GroupStrategyRegistry;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup.StrategyParameterSet;

import java.util.Map;

/**
 * @author thibautd
 */
public class DefaultGroupStrategyRegistryModule extends AbstractModule {
	private static Logger log = Logger.getLogger( DefaultGroupStrategyRegistryModule.class );

	@Override
    public void install() {
        install(new DefaultJointStrategiesModule());
        bind(GroupStrategyRegistry.class).toProvider(GroupStrategyRegistryFiller.class).in(Singleton.class);
    }

    private static class GroupStrategyRegistryFiller implements Provider<GroupStrategyRegistry> {

		private final Map<String, Provider<GroupPlanStrategy>> strategies;
		private final Map<String, Provider<ExtraPlanRemover>> removers;

		private final Scenario sc;

		@Inject
		public GroupStrategyRegistryFiller(
				final Map<String, Provider<GroupPlanStrategy>> strategies,
				final Map<String, Provider<ExtraPlanRemover>> removers,
				final Scenario sc ) {
			this.strategies = strategies;
			this.removers = removers;
			this.sc = sc;
		}

        @Override
        public GroupStrategyRegistry get() {
			final GroupStrategyRegistry strategyRegistry  = new GroupStrategyRegistry();
			final Config config = sc.getConfig();
			final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup) config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

			strategyRegistry.setExtraPlanRemover( removers.get( weights.getSelectorForRemoval() ).get() );

			for ( StrategyParameterSet set : weights.getStrategyParameterSets() ) {
				final Provider<GroupPlanStrategy> provider = strategies.get( set.getStrategyName() );

				if ( provider == null ) {
					log.error( "Could not find strategy "+set.getStrategyName() );
					log.error( "valid strategy names are "+strategies.keySet() );
					throw new RuntimeException( "Could not find strategy "+set.getStrategyName() );
				}
				strategyRegistry.addStrategy(
						provider.get(),
						set.getSubpopulation(),
						set.getWeight(),
						set.isInnovative() ?
							weights.getDisableInnovationAfterIter() :
							-1 );
			}

            return strategyRegistry;
        }
    }

}
