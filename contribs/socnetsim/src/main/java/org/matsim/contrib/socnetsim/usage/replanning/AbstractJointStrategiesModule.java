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
package org.matsim.contrib.socnetsim.usage.replanning;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;
import org.matsim.contrib.socnetsim.framework.replanning.ExtraPlanRemover;
import org.matsim.contrib.socnetsim.framework.replanning.GroupPlanStrategy;
import org.matsim.contrib.socnetsim.framework.replanning.NonInnovativeStrategyFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import org.matsim.core.controler.AbstractModule;

import java.util.Map;

/**
 * @author thibautd
 */
public abstract class AbstractJointStrategiesModule extends AbstractModule {
	private MapBinder<String, GroupPlanStrategy> planStrategyBinder;
	private MapBinder<String, GroupLevelPlanSelector> selectorBinder;
	private MapBinder<String, ExtraPlanRemover> removerBinder;
	private MapBinder<String, NonInnovativeStrategyFactory> nonInnovativeBinder;

	protected final void addFactory(
			final String name,
			final Class<? extends Provider<? extends GroupPlanStrategy>> f) {
		getPlanStrategyBinder().addBinding(name).toProvider( f );
	}

	protected final void addFactory(
			final String name,
			final Provider<? extends GroupPlanStrategy> f) {
		getPlanStrategyBinder().addBinding(name).toProvider( f );
	}

	protected final void addSelectorFactory(
			final String name,
			final Provider<GroupLevelPlanSelector> f) {
		getSelectorBinder().addBinding(name).toProvider( f );
	}

	protected final void addSelectorAndStrategyFactory(
			final String name,
			final Class<? extends NonInnovativeStrategyFactory> f) {
		// Not really nice, but could not come with something better right now:
		// still need constructor to be injected,
		// and the class to provide two providers (which one cannot implement
		// at the same time)
		getNonInnovativeBinder().addBinding(name).to( f );
		addFactory( name , f );
		addSelectorFactory( name ,
				new Provider<GroupLevelPlanSelector>() {
					@Inject
					Map<String, NonInnovativeStrategyFactory> map;

					@Override
					public GroupLevelPlanSelector get() {
						return map.get( name ).createSelector();
					}
				} );
	}

	protected final void addSelectorAndStrategyFactory(
			final String name,
			final NonInnovativeStrategyFactory f) {
		// Not really nice, but could not come with something better right now:
		// still need constructor to be injected,
		// and the class to provide two providers (which one cannot implement
		// at the same time)
		getNonInnovativeBinder().addBinding(name).toInstance( f );
		addFactory( name , f );
		addSelectorFactory( name ,
				new Provider<GroupLevelPlanSelector>() {
					@Inject Map<String, NonInnovativeStrategyFactory> map;

					@Override
					public GroupLevelPlanSelector get() {
						return map.get( name ).createSelector();
					}
				} );
	}

	protected final void addRemoverFactory(
			final String name,
			final Class<? extends Provider<ExtraPlanRemover>> f) {
		getRemoverBinder()
			.addBinding(name)
			.toProvider( f );
	}

	private MapBinder<String, GroupPlanStrategy> getPlanStrategyBinder() {
		if ( planStrategyBinder == null ) planStrategyBinder = MapBinder.newMapBinder(binder(), String.class, GroupPlanStrategy.class);
		return planStrategyBinder;
	}

	private MapBinder<String, GroupLevelPlanSelector> getSelectorBinder() {
		if ( selectorBinder == null ) selectorBinder =  MapBinder.newMapBinder( binder() , String.class , GroupLevelPlanSelector.class );
		return selectorBinder;
	}

	private MapBinder<String, ExtraPlanRemover> getRemoverBinder() {
		if ( removerBinder == null ) removerBinder =  MapBinder.newMapBinder( binder() , String.class , ExtraPlanRemover.class );
		return removerBinder;
	}

	private MapBinder<String, NonInnovativeStrategyFactory> getNonInnovativeBinder() {
		if ( nonInnovativeBinder == null ) nonInnovativeBinder =  MapBinder.newMapBinder( binder() , String.class , NonInnovativeStrategyFactory.class );
		return nonInnovativeBinder;
	}
}
