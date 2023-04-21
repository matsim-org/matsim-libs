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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultTurnAcceptanceLogic;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;
import org.matsim.vehicles.VehicleType;
import org.matsim.withinday.mobsim.WithinDayQSimModule;

/**
 * @author smetzler, dziemke
 */
public final class BicycleModule extends AbstractModule {

	private static final Logger LOG = LogManager.getLogger(BicycleModule.class);

	@Inject
	private BicycleConfigGroup bicycleConfigGroup;

	@Override
	public void install() {

		addTravelTimeBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelTime.class).in(Singleton.class);
		addTravelDisutilityFactoryBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelDisutilityFactory.class).in(Singleton.class);

		// TODO: bicycle contrib can not be used with other scoring functions at the moment, as only one can be installed
		// TODO: scoring should work via a score event so it can be used together with other scoring functions

		switch ( bicycleConfigGroup.getBicycleScoringType() ) {
			case legBased -> {

//				yyyyyy the status here is that this seems to work.  but because of numerical imprecision the results are _slightly_
//				different.  This needs to be documented diligently test by test.  kai, dec'22
				// yyyyyy remember that the 10it test needs to be un-ignored.  kai, dec'22

				this.addEventHandlerBinding().to( BicycleScoreEventsCreator.class );
				this.bind( AdditionalBicycleLinkScore.class ).to( AdditionalBicycleLinkScoreDefaultImpl.class );
			}
			case linkBased -> {
				bindScoringFunctionFactory().to(BicycleScoringFunctionFactory.class).in(Singleton.class);
			}
			default -> throw new IllegalStateException( "Unexpected value: " + bicycleConfigGroup.getBicycleScoringType() );
		}

		bind( BicycleLinkSpeedCalculator.class ).to( BicycleLinkSpeedCalculatorDefaultImpl.class ) ;

		if (bicycleConfigGroup.isMotorizedInteraction()) {
			addMobsimListenerBinding().to(MotorizedInteractionEngine.class);
		}
		addControlerListenerBinding().to(ConsistencyCheck.class);

		this.installOverridingQSimModule( new AbstractQSimModule(){
			@Inject EventsManager events;
			@Inject Scenario scenario;
			@Inject BicycleLinkSpeedCalculator bicycleLinkSpeedCalculator;
			@Override protected void configureQSim(){
				final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
				factory.setLinkSpeedCalculator( bicycleLinkSpeedCalculator );
				bind( QNetworkFactory.class ).toInstance(factory );
				// NOTE: Other than when using a provider, this uses the same factory instance over all iterations, re-configuring
				// it in every iteration via the initializeFactory(...) method. kai, mar'16
			}
		} );
	}

	static class ConsistencyCheck implements StartupListener {

		@Inject
		private BicycleConfigGroup bicycleConfigGroup;

		@Inject
		private Scenario scenario;

		@Override
		public void notifyStartup(StartupEvent event) {

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
