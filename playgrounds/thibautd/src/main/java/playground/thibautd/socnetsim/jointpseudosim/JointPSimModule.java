/* *********************************************************************** *
 * project: org.matsim.*
 * JointPSimModule.java
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
package playground.thibautd.socnetsim.jointpseudosim;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import playground.thibautd.socnetsim.framework.replanning.ExtraPlanRemover;
import playground.thibautd.socnetsim.framework.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.framework.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.jointpseudosim.qsim.SwitchingJointQSimFactoryModule;
import playground.thibautd.socnetsim.usage.replanning.DefaultJointStrategiesModule;
import playground.thibautd.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.usage.replanning.GroupReplanningConfigGroup.StrategyParameterSet;
import playground.thibautd.socnetsim.jointpseudosim.replanning.InnovationSwitchingGroupReplanningListenner.Innovative;
import playground.thibautd.socnetsim.jointpseudosim.replanning.InnovationSwitchingGroupReplanningListenner.NonInnovative;
import playground.thibautd.utils.TravelTimeRetrofittingEventHandler;

import java.util.Map;

/**
 * @author thibautd
 */
public class JointPSimModule extends AbstractModule {

	@Override
	public void install() {
		// qsim
		install( new SwitchingJointQSimFactoryModule() );

		// replanning
		install( new DefaultJointStrategiesModule() );

		bind( GroupStrategyRegistry.class )
			.annotatedWith( Innovative.class )
			.toProvider( new InnovativeGroupStrategyRegistryFiller( true ) )
			.in( Scopes.SINGLETON );

		bind( GroupStrategyRegistry.class )
			.annotatedWith( NonInnovative.class )
			.toProvider( new InnovativeGroupStrategyRegistryFiller( false ) )
			.in( Scopes.SINGLETON );

		// This is pretty hackish, and only needed for "teleported" QSim...
		// Something like this is necessary, otherwise out-of-date
		// travel times keep being used in pSim (out-of-date including
		// the freeflow estimations from the first iterations!).
		// This goes in the direction of the "traditional" pSim,
		// without the cost of iterating through all links of all routes
		// (which is the costly part of QSim, much more than what is done
		// for each link: it is mainly a complexity problem, that pSim does
		// solve only by allowing more parallelism).
		addEventHandlerBinding().to( TravelTimeRetrofittingEventHandler.class );
	}

    private static class InnovativeGroupStrategyRegistryFiller implements Provider<GroupStrategyRegistry> {

		private @Inject Map<String, Provider<GroupPlanStrategy>> strategies;
		private @Inject Map<String, Provider<ExtraPlanRemover>> removers;

		private @Inject Scenario sc;

		private final boolean innovativeness;

		@Inject
		public InnovativeGroupStrategyRegistryFiller( final boolean innovativeness ) {
			this.innovativeness = innovativeness;
		}

        @Override
        public GroupStrategyRegistry get() {
			final GroupStrategyRegistry strategyRegistry  = new GroupStrategyRegistry();
			final Config config = sc.getConfig();
			final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup) config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

			strategyRegistry.setExtraPlanRemover( removers.get( weights.getSelectorForRemoval() ).get() );

			for ( StrategyParameterSet set : weights.getStrategyParameterSets() ) {
				if ( set.isInnovative() == innovativeness ) {
					strategyRegistry.addStrategy(
						strategies.get( set.getStrategyName() ).get(),
						set.getSubpopulation(),
						set.getWeight(),
						-1 );
				}
			}

            return strategyRegistry;
        }
    }


}

