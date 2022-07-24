/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package lsp.controler;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import lsp.LSP;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.controler.CarrierAgentTracker;
import org.matsim.contrib.freight.controler.FreightAgentSource;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.scoring.ScoringFunction;

import java.util.List;


public class LSPModule extends AbstractModule {
	private static final Logger log = org.apache.log4j.Logger.getLogger( LSPModule.class );

//	private final FreightConfigGroup carrierConfig = new FreightConfigGroup();

	@Override
	public void install() {
		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule( getConfig(), FreightConfigGroup.class ) ;

		bind( LSPControlerListener.class ).in( Singleton.class );
		addControlerListenerBinding().to( LSPControlerListener.class );

		// this switches on certain qsim components:
		QSimComponentsConfigGroup qsimComponents = ConfigUtils.addOrGetModule( getConfig(), QSimComponentsConfigGroup.class );
		List<String> abc = qsimComponents.getActiveComponents();
		abc.add( FreightAgentSource.COMPONENT_NAME );
		switch ( freightConfig.getTimeWindowHandling() ) {
			case ignore:
				break;
//			case enforceBeginnings:
////				abc.add( WithinDayActivityReScheduling.COMPONENT_NAME );
//				log.warn("LSP has never hedged against time window openings; this is probably wrong; but I don't know what to do ...");
//				break;
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
//					case enforceBeginnings:
////						this.addQSimComponentBinding(WithinDayActivityReScheduling.COMPONENT_NAME).to( WithinDayActivityReScheduling.class );
//						log.warn("LSP has never hedged against time window openings; this is probably wrong; but I don't know what to do ...");
//						break;
					default:
						throw new IllegalStateException( "Unexpected value: " + freightConfig.getTimeWindowHandling() );
				}
			}
		} );

		bind( LSPScoringFunctionFactory.class ).to( LSPScoringFunctionFactoryDummyImpl.class );

	}

	@Provides CarrierAgentTracker provideCarrierResourceTracker( LSPControlerListener lspControlerListener ) {
		return lspControlerListener.getCarrierResourceTracker();
	}

	private static class LSPScoringFunctionFactoryDummyImpl implements LSPScoringFunctionFactory {
		@Override public ScoringFunction createScoringFunction( LSP lsp ){
			return new ScoringFunction(){
				@Override public void handleActivity( Activity activity ){
				}
				@Override public void handleLeg( Leg leg ){
				}
				@Override public void agentStuck( double time ){
				}
				@Override public void addMoney( double amount ){
				}
				@Override public void addScore( double amount ){
				}
				@Override public void finish(){
				}
				@Override public double getScore(){
					return Double.NEGATIVE_INFINITY;
				}
				@Override public void handleEvent( Event event ){
				}
			};
		}
	}
}
