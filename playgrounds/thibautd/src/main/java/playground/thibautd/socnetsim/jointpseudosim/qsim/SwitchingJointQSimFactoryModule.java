/* *********************************************************************** *
 * project: org.matsim.*
 * SwitchingJointQSimFactoryModule.java
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
package playground.thibautd.socnetsim.jointpseudosim.qsim;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.thibautd.pseudoqsim.DeactivableTravelTimeProvider;
import playground.thibautd.pseudoqsim.DeactivableTravelTimeProvider.PSimIterationsCriterion;
import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;

/**
 * @author thibautd
 */
public class SwitchingJointQSimFactoryModule extends AbstractModule {

	@Override
	public void install() {
		// TODO XXX check that this works!!!!
		bind( Mobsim.class ).toProvider( SwitchingJointQSimFactory.class );
		addControlerListenerBinding().to( SwitchingJointQSimFactory.class );

		bind( DeactivableTravelTimeProvider.class )
			.toProvider(
				new Provider<DeactivableTravelTimeProvider>() {
					@Inject TravelTimeCalculator calculator;
					@Inject Config conf;

					@Override
					public DeactivableTravelTimeProvider get() {
						return new DeactivableTravelTimeProvider(
							new PSimIterationsCriterion( (PseudoSimConfigGroup) conf.getModule( PseudoSimConfigGroup.GROUP_NAME ) ),
							calculator );
					}
				})
			.in( Scopes.SINGLETON );
		addEventHandlerBinding().to( DeactivableTravelTimeProvider.class );
	}
}

