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
package playground.thibautd.phd;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.router.TripRouter;

/**
 * @author thibautd
 */
public class StartingPointRandomizerModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to( Randomizer.class );
	}

	@Singleton
	private static class Randomizer implements StartupListener {
		private final SubtourModeChoice mode;
		private final TimeAllocationMutator time;
		private final Population population;

		@Inject
		public Randomizer(
				final Provider<TripRouter> tripRouterProvider,
				final GlobalConfigGroup globalConfigGroup,
				final SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup,
				final PlansConfigGroup plansConfigGroup,
				final TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup, final Population population ) {
			this.population = population;
			mode = new SubtourModeChoice( tripRouterProvider , globalConfigGroup , subtourModeChoiceConfigGroup );
			time = new TimeAllocationMutator( tripRouterProvider, plansConfigGroup, timeAllocationMutatorConfigGroup, globalConfigGroup );
		}

		@Override
		public void notifyStartup( final StartupEvent event ) {
			run( mode );
			run( time );
		}

		private void run( final AbstractMultithreadedModule module ) {
			module.prepareReplanning( () -> 0 );
			population.getPersons().values().stream()
					.flatMap( person -> person.getPlans().stream() )
					.forEach( module::handlePlan );
			module.finishReplanning();
		}
	}
}
