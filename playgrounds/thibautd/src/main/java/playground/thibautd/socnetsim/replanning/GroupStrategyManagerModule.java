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
package playground.thibautd.socnetsim.replanning;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup.StrategyParameterSet;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * @author thibautd
 */
public class GroupStrategyManagerModule extends AbstractModule {
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
		public GroupStrategyRegistryFiller( Map<String, Provider<GroupPlanStrategy>> strategies , Map<String, Provider<ExtraPlanRemover>> removers , Scenario sc ) {
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
				strategyRegistry.addStrategy(
						strategies.get( set.getStrategyName() ).get(),
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
