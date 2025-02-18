/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.vehicles.VehicleType;

/**
 * @author smetzler, dziemke
 */
public final class BicycleModule extends AbstractModule {
	private static final Logger LOG = LogManager.getLogger(BicycleModule.class);

	@Inject private BicycleConfigGroup bicycleConfigGroup;

	@Override public void install() {
//		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( this.getConfig(), BicycleConfigGroup.class );
//		this.bind( BicycleConfigGroup.class ).toInstance( bicycleConfigGroup );
		// the above feels odd.  But it seems to work.  I actually have no idea where the config groups are bound, neither for the core config
		// groups nor for the added config groups.  In general, the original idea was that AbstractModule provides the config from
		// getConfig(), not from injection.  kai, jun'24

		// It actually does not work in general.  The ExplodedConfigModule injects all config groups that are materialized by then.  Which
		// means that it needs to be materialized "quite early", and in particular before this install method is called.  For the time being,
		// a run script using the contrib thus needs to materialize the config group.  kai, jul'24


		// The idea here is the following:
		// * scores are just added as score events.  no scoring function is replaced.

		// * link speeds are computed via a plugin handler to the DefaultLinkSpeedCalculator.  If the plugin handler returns a speed, it is
		// used, otherwise the default speed is used. This has the advantage that multiple plugins can register such special link speed calculators.

		// this gives the typical things to the router:
		addTravelTimeBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelTime.class).in(Singleton.class);
		addTravelDisutilityFactoryBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelDisutilityFactory.class).in(Singleton.class);
		// (the BicycleTravelTime uses the BicycleLinkSpeed Calculator bound below)
		// (the BicycleDisutility uses a BicycleTravelDisutility)

		// compute and throw the additional score events:
		this.addEventHandlerBinding().to( BicycleScoreEventsCreator.class );
		// (this uses the AdditionalBicycleLinkScore to compute and throw corresponding scoring events)
		// (it also computes and throws the motorized interaction events, if they are switched on)

		this.bind( AdditionalBicycleLinkScore.class ).to( AdditionalBicycleLinkScoreDefaultImpl.class );
		// (this computes the value of the per-link scoring event.  yyyy Very unfortunately, it is a re-implementation of the BicycleTravelDisutility (mentioned above).)

		this.installOverridingQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				this.addLinkSpeedCalculatorBinding().to( BicycleLinkSpeedCalculator.class );
			}
		} );

		bind( BicycleLinkSpeedCalculator.class ).to( BicycleLinkSpeedCalculatorDefaultImpl.class ) ;
		// (both the router and the mobsim need this)

		addControlerListenerBinding().to(ConsistencyCheck.class);
	}

	static class ConsistencyCheck implements StartupListener {
		@Inject private BicycleConfigGroup bicycleConfigGroup;
		@Inject private Scenario scenario;

		@Override public void notifyStartup(StartupEvent event) {
			Id<VehicleType> bicycleVehTypeId = Id.create(bicycleConfigGroup.getBicycleMode(), VehicleType.class);
			if (scenario.getVehicles().getVehicleTypes().get(bicycleVehTypeId) == null) {
				LOG.warn("There is no vehicle type '" + bicycleConfigGroup.getBicycleMode() + "' specified in the vehicle types. "
						     + "Can't check the consistency of the maximum velocity in the bicycle vehicle type and the bicycle config group. "
						     + "Should at least be approximately the same and randomization should be enabled.");
			} else {

				double mobsimSpeed = scenario.getVehicles().getVehicleTypes().get(bicycleVehTypeId).getMaximumVelocity();
				if (Math.abs(mobsimSpeed - bicycleConfigGroup.getMaxBicycleSpeedForRouting()) > 0.1) {
					LOG.warn("There is an inconsistency in the specified maximum velocity for " + bicycleConfigGroup.getBicycleMode() + ":"
							     + " Maximum speed specified in the 'bicycle' config group (used for routing): " + bicycleConfigGroup.getMaxBicycleSpeedForRouting() + " vs."
							     + " maximum speed specified for the vehicle type (used in mobsim): " + mobsimSpeed);
					if (scenario.getConfig().routing().getRoutingRandomness() == 0.) {
						throw new RuntimeException("The recommended way to deal with the inconsistency between routing and scoring/mobsim is to have a randomized router. Aborting... ");
					}
				}
			}
			if (!scenario.getConfig().qsim().getMainModes().contains(bicycleConfigGroup.getBicycleMode())) {
				LOG.warn(bicycleConfigGroup.getBicycleMode() + " not specified as main mode.");
			}
		}
	}
}
