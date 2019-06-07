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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

/**
 * @author smetzler, dziemke
 */
public final class BicycleModule extends AbstractModule {
	// necessary to have this public
	
	private final Scenario scenario;
	
	public BicycleModule(Scenario scenario) {
		// at some point let's replace this by some guice approach
		this.scenario = scenario;
	}
	
	@Override
	public void install() {
		
		BicycleConfigGroup bicycleConfigGroup = (BicycleConfigGroup) scenario.getConfig().getModules().get(BicycleConfigGroup.GROUP_NAME);

		bind(BicycleTravelTime.class).asEagerSingleton();
		addTravelTimeBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelTime.class);
		bind(BicycleTravelDisutilityFactory.class).asEagerSingleton();
		addTravelDisutilityFactoryBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelDisutilityFactory.class);
		bindScoringFunctionFactory().toInstance(new BicycleScoringFunctionFactory());
		// The following leads to "Tried proxying org.matsim.core.scoring.ScoringFunctionsForPopulation to support a circular dependency, but it is not an interface."
//		bindScoringFunctionFactory().to(BicycleScoringFunctionFactory.class);
		
		if (bicycleConfigGroup.isMotorizedInteraction()) {
			addMobsimListenerBinding().to(MotorizedInteractionEngine.class);
		}
		
//		installQSimModule(new AbstractQSimModule() {
//			@Inject Scenario scenario;
//
//            @Override
//            protected void configureQSim() {
//                bind(QNetworkFactory.class).toProvider(new Provider<QNetworkFactory>() {
//                    @Inject
//                    private EventsManager events;
//
//                    @Override
//                    public QNetworkFactory get() {
//                        final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
//                        factory.setLinkSpeedCalculator(new BicycleLinkSpeedCalculator(scenario));
//                        return factory;
//                    }
//                });
//            }
//        });
	}
	
}