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
package playground.ivt.maxess.nestedlogitaccessibility.framework;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioByInstanceModule;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Sets the basic elements of the computation.
 *
 * Configuration needs to be completed by binding choice set identifier and utility.
 * This module already binds the scenario and its components, for convenience.
 *
 * Due to the way java handles generic types, one needs to create an empty anonymous subclass for the type
 * parameters to be found.
 *
 * @author thibautd
 */
public abstract class BaseNestedAccessibilityComputationModule<N extends Enum<N>> extends AbstractModule {
	private final Scenario scenario;
	private final TypeLiteral<N> type;

	@SuppressWarnings( "unchecked" )
	public BaseNestedAccessibilityComputationModule(
			final Scenario scenario ) {
		this.scenario = scenario;

		// see http://gafter.blogspot.ch/2006/12/super-type-tokens.html
		// As far as I can see, this is safe (the type parameter can only be N)
		// The website above is from a guy who actually worked on the development of generics
		// at Sun, so I believe there is no nicest way...
		final Type superclass = getClass().getGenericSuperclass();
		final Type typeParameter = ((ParameterizedType) superclass).getActualTypeArguments()[ 0 ];
		this.type = (TypeLiteral<N>) TypeLiteral.get( typeParameter );
	}

	@Override
	public final void install() {
		// bind scenario elements
		//bind( Scenario.class ).toInstance( scenario );
		//bind( ActivityFacilities.class ).toInstance( scenario.getActivityFacilities() );
		//bind( Population.class ).toInstance( scenario.getPopulation() );
		////bind( Config.class ).toInstance( scenario.getConfig() );
		//bind( Network.class ).toInstance( scenario.getNetwork() );
		//bind( TransitSchedule.class ).toInstance( scenario.getTransitSchedule() );

		install( new ScenarioByInstanceModule( scenario ) );
		install( new TripRouterModule() );

		install( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( "car" ).to( FreespeedTravelTimeAndDisutility.class );
				addTravelDisutilityFactoryBinding( "car" ).toInstance(
						tt -> (TravelDisutility) tt);
			}
		} );

		// Do not really understand how this can possibly work, but this allows to bind generic types
		// with specific type parameters.
		// to read on the underlying process:
		// http://gafter.blogspot.ch/2006/12/super-type-tokens.html
		// http://gafter.blogspot.ch/2006/12/type-literals.html
		bind( newGenericType( NestedLogitAccessibilityCalculator.class ) );
		bind( newGenericType( NestedLogitModel.class ) );
	}

	@Provides @Singleton
	public FreespeedTravelTimeAndDisutility createTravelTime( final PlanCalcScoreConfigGroup config ) {
		return new FreespeedTravelTimeAndDisutility( config );
	}

	/**
	 * hack from http://stackoverflow.com/questions/7385858/guice-module-with-type-parameters
	 */
	@SuppressWarnings( "unchecked" )
	private <V> TypeLiteral<V> newGenericType( Class<?> base ) {
		final ParameterizedType newType = Types.newParameterizedType( base , type.getType() );
		return (TypeLiteral<V>) TypeLiteral.get( newType );
	}
}
