/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CarrierModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.freight.controler;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;

import java.util.List;

public final class CarrierModule extends AbstractModule {

	private CarrierPlanStrategyManagerFactory strategyManagerFactory;
	private CarrierScoringFunctionFactory scoringFunctionFactory;

	public CarrierModule() {
	}

//	/**
//	 * CarrierPlanStrategyManagerFactory and CarrierScoringFunctionFactory must be bound separately
//	 * when this constructor is used.
//	 *
//	 * @deprecated please use FreightUtils.getCarriers(Scenario scenario) to load carriers into scenario and use CarrierModule()
//	 */
//	@Deprecated
//	public CarrierModule(Carriers carriers) {
//		this.carriers = carriers;
//	}

	/**
	 * @deprecated please use FreightUtils.getCarriers(Scenario scenario) to load carriers into scenario and use CarrierModule()
	 */
	@Deprecated
	public CarrierModule(CarrierPlanStrategyManagerFactory strategyManagerFactory, CarrierScoringFunctionFactory scoringFunctionFactory) {
//		this.carriers = carriers;
		this.strategyManagerFactory = strategyManagerFactory;
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	@Override public void install() {
		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule( getConfig(), FreightConfigGroup.class ) ;

		bind(Carriers.class).toProvider( new CarrierProvider() ).asEagerSingleton(); // needs to be eager since it is still scenario construction. kai, oct'19
		// this is probably ok

		if (strategyManagerFactory != null) {
			bind(CarrierPlanStrategyManagerFactory.class).toInstance(strategyManagerFactory);
		}
		if (scoringFunctionFactory != null) {
			bind(CarrierScoringFunctionFactory.class).toInstance(scoringFunctionFactory);
		}

//		bind(CarrierControlerListener.class).in( Singleton.class );
		bind(CarrierControlerListener.class).asEagerSingleton();
		addControlerListenerBinding().to(CarrierControlerListener.class);

		// this switches on certain qsim components:
		QSimComponentsConfigGroup qsimComponents = ConfigUtils.addOrGetModule( getConfig(), QSimComponentsConfigGroup.class );
		List<String> abc = qsimComponents.getActiveComponents();
		abc.add( FreightAgentSource.COMPONENT_NAME ) ;
		switch ( freightConfig.getTimeWindowHandling() ) {
			case ignore:
				break;
			case enforceBeginnings:
				abc.add( WithinDayActivityReScheduling.COMPONENT_NAME );
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + freightConfig.getTimeWindowHandling() );
		}
		qsimComponents.setActiveComponents( abc );

		// this installs qsim components, which are switched on (or not) via the above syntax:
		this.installQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				this.bind( FreightAgentSource.class ).in( Singleton.class );
				this.addQSimComponentBinding( FreightAgentSource.COMPONENT_NAME ).to( FreightAgentSource.class );
				switch( freightConfig.getTimeWindowHandling() ) {
					case ignore:
						break;
					case enforceBeginnings:
						this.addQSimComponentBinding(WithinDayActivityReScheduling.COMPONENT_NAME).to( WithinDayActivityReScheduling.class );
						break;
					default:
						throw new IllegalStateException( "Unexpected value: " + freightConfig.getTimeWindowHandling() );
				}
			}
		} );


		this.addControlerListenerBinding().toInstance((ShutdownListener) event -> writeAdditionalRunOutput( event.getServices().getControlerIO(), event.getServices().getConfig(), FreightUtils.getCarriers( event.getServices().getScenario() ) ));

	}

	// We export CarrierAgentTracker, which is kept by the ControlerListener, which happens to re-create it every iteration.
	// The freight QSim needs it (see below [[where?]]).
	// yyyy this feels rather scary.  kai, oct'19
	// Since we are exporting it anyways, we could as well also inject it.  kai, sep'20
	@Provides
	CarrierAgentTracker provideCarrierAgentTracker(CarrierControlerListener carrierControlerListener) {
		return carrierControlerListener.getCarrierAgentTracker();
	}

	private static void writeAdditionalRunOutput( OutputDirectoryHierarchy controllerIO, Config config, Carriers carriers ) {
		// ### some final output: ###
		String compression = config.controler().getCompressionType().fileEnding;
		new CarrierPlanWriter(carriers).write( controllerIO.getOutputFilename("output_carriers.xml" + compression));
		new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write(controllerIO.getOutputFilename("output_carriersVehicleTypes.xml" + compression));
	}


	private static class CarrierProvider implements Provider<Carriers> {
		@Inject Scenario scenario;
		@Override public Carriers get() {
//			if ( carriers!=null ){
//				if ( scenario.getScenarioElement( FreightUtils.CARRIERS ) != null ) {
//					throw new RuntimeException("carriers are provided as scenario element AND per the CarrierModule constructor.  I could check if " +
//										     "they are the same, but in general the second way to do this is deprecated so please put " +
//										     "null into your constructor (and expect more cleanup here).") ;
//				}
//				scenario.addScenarioElement( FreightUtils.CARRIERS, carriers );
//			}
			return FreightUtils.getCarriers(scenario);
		}
	}
}
