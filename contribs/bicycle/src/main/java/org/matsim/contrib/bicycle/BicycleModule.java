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

	@Inject
	private BicycleConfigGroup bicycleConfigGroup;

	@Override
	public void install() {
		// The idea here is the following:
		// * scores are just added as score events.  no scoring function is replaced.

		// * link speeds are computed via a plugin handler to the DefaultLinkSpeedCalculator.  If the plugin handler returns a speed, it is
		// used, otherwise the default speed is used. This has the advantage that multiple plugins can register such special link speed calculators.

		addTravelTimeBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelTime.class).in(Singleton.class);
		addTravelDisutilityFactoryBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelDisutilityFactory.class).in(Singleton.class);

		this.addEventHandlerBinding().to( BicycleScoreEventsCreator.class );
		// (the motorized interaction is in the BicycleScoreEventsCreator)

		this.bind( AdditionalBicycleLinkScore.class ).to( AdditionalBicycleLinkScoreDefaultImpl.class );

		bind( BicycleLinkSpeedCalculator.class ).to( BicycleLinkSpeedCalculatorDefaultImpl.class ) ;
		// this is still needed because the bicycle travel time calculator for routing needs to use the same bicycle speed as the mobsim.  kai, jun'23

		this.installOverridingQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				this.addLinkSpeedCalculator().to( BicycleLinkSpeedCalculator.class );
			}
		} );

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
