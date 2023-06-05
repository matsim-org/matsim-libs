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

		switch ( bicycleConfigGroup.getBicycleScoringType() ) {
			case legBased -> {
				this.addEventHandlerBinding().to( BicycleScoreEventsCreator.class );
			}
			case linkBased -> {
				// yyyy the leg based scoring was moved to score events, so that it does not change the scoring function.  For the
				// link based scoring, this has not yet been done.  It seems to me that the link based scoring is needed for the
				// motorized interaction.  However, from a technical point of vew it should be possible to use the score events as
				// well, since they are computed link-by-link.  That is, optimally the link based scoring would go away completely,
				// and only motorized interaction would be switched on or off.  kai, jun'23

				bindScoringFunctionFactory().to(BicycleScoringFunctionFactory.class).in(Singleton.class);
			}
			default -> throw new IllegalStateException( "Unexpected value: " + bicycleConfigGroup.getBicycleScoringType() );
		}

		bind( BicycleLinkSpeedCalculator.class ).to( BicycleLinkSpeedCalculatorDefaultImpl.class ) ;
		// this is still needed because the bicycle travel time calculator needs to use the same bicycle speed as the mobsim.  kai, jun'23

		if (bicycleConfigGroup.isMotorizedInteraction()) {
			addMobsimListenerBinding().to(MotorizedInteractionEngine.class);
		}

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
					if (scenario.getConfig().plansCalcRoute().getRoutingRandomness() == 0.) {
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
