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
package org.matsim.contrib.bicycle.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.contrib.bicycle.MotorizedInteractionEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * @author smetzler, dziemke
 */
public class BicycleModule extends AbstractModule {
	private boolean considerMotorizedInteraction;
	
	@Override
	public void install() {
		bind(BicycleTravelTime.class).asEagerSingleton();
		addTravelTimeBinding("bicycle").to(BicycleTravelTime.class);
		bind(BicycleTravelDisutilityFactory.class).asEagerSingleton();
		addTravelDisutilityFactoryBinding("bicycle").to(BicycleTravelDisutilityFactory.class);		
		this.bindScoringFunctionFactory().toInstance(new BicycleScoringFunctionFactory());
		
		if (considerMotorizedInteraction) {
			addMobsimListenerBinding().to(MotorizedInteractionEngine.class);
		}
	}
	
	@Singleton @Provides
	QNetworkFactory provideQNetworkFactory(Scenario scenario, EventsManager eventsManager, MobsimTimer mobsimTimer, AgentCounter agentCounter) {
		ConfigurableQNetworkFactory qNetworkFactory = new ConfigurableQNetworkFactory(eventsManager, scenario, mobsimTimer, agentCounter) ;
		qNetworkFactory.setLinkSpeedCalculator(new LinkSpeedCalculator(){
			LinkSpeedCalculator delegate = new DefaultLinkSpeedCalculator() ;
			@Override public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
				if ( vehicle.getVehicle().getType().getId().equals( Id.create("bicycle", VehicleType.class) ) ) {
//					return vehicle.getMaximumVelocity(); // return the same as vehicleType.getMaximumVelocity()
//					return vehicle.getVehicle().getType().getMaximumVelocity();
					return BicycleUtils.getSpeed("bicycle");
				} else {
					return delegate.getMaximumVelocity(vehicle, link, time) ;
				}
			}
		});
		return qNetworkFactory;
	}

	public void setConsiderMotorizedInteraction(boolean considerMotorizedInteraction) {
		this.considerMotorizedInteraction = considerMotorizedInteraction;
	}
}