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
import lsp.LSPs;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.controler.CarrierAgentTracker;
import org.matsim.contrib.freight.controler.LSPAgentSource;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreator;
import lsp.replanning.LSPReplanningModule;
import lsp.scoring.LSPScoringModule;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;

import java.util.Collection;
import java.util.List;


public class LSPModule extends AbstractModule {
	private static final Logger log = org.apache.log4j.Logger.getLogger( LSPModule.class );

	private final LSPs lsps;
	private final LSPReplanningModule replanningModule;
	private final LSPScoringModule scoringModule;
	private final Collection<LSPEventCreator> creators;

	private final FreightConfigGroup carrierConfig = new FreightConfigGroup();

	public LSPModule(LSPs  lsps, LSPReplanningModule replanningModule, LSPScoringModule scoringModule, Collection<LSPEventCreator> creators) {
		// yyyy LSPReplanningModule & LSPScoringModule are bound later anyways, so we can as well bind them directly.
		// Not sure about the other parameters.  kai, sep'20

		this.lsps = lsps;
		this.replanningModule = replanningModule;
		this.scoringModule = scoringModule;
		this.creators = creators;
	}


	@Override
	public void install() {
		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule( getConfig(), FreightConfigGroup.class ) ;

		bind(FreightConfigGroup.class).toInstance(carrierConfig);
		bind(LSPs.class).toInstance(lsps);
		if(replanningModule != null) {
			bind(LSPReplanningModule.class).toInstance(replanningModule);
		}
		if(scoringModule != null) {
			bind(LSPScoringModule.class).toInstance(scoringModule);
		}

		bind( LSPControlerListenerImpl.class ).in( Singleton.class );
		addControlerListenerBinding().to( LSPControlerListenerImpl.class );

		// replace ...

//		bindMobsim().toProvider( LSPQSimFactory.class );

		// ... by ...

		// this switches on certain qsim components:
		QSimComponentsConfigGroup qsimComponents = ConfigUtils.addOrGetModule( getConfig(), QSimComponentsConfigGroup.class );
		List<String> abc = qsimComponents.getActiveComponents();
		abc.add( LSPAgentSource.COMPONENT_NAME ) ;
		switch ( freightConfig.getTimeWindowHandling() ) {
			case ignore:
				break;
			case enforceBeginnings:
//				abc.add( WithinDayActivityReScheduling.COMPONENT_NAME );
				log.warn("LSP has never hedged against time window openings; this is probably wrong; but I don't know what to do ...");
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + freightConfig.getTimeWindowHandling() );
		}
		qsimComponents.setActiveComponents( abc );

		// this installs qsim components, which are switched on (or not) via the above syntax:
		this.installQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				this.bind( LSPAgentSource.class ).in( Singleton.class );
				this.addQSimComponentBinding( LSPAgentSource.COMPONENT_NAME ).to( LSPAgentSource.class );
				switch( freightConfig.getTimeWindowHandling() ) {
					case ignore:
						break;
					case enforceBeginnings:
//						this.addQSimComponentBinding(WithinDayActivityReScheduling.COMPONENT_NAME).to( WithinDayActivityReScheduling.class );
						log.warn("LSP has never hedged against time window openings; this is probably wrong; but I don't know what to do ...");
						break;
					default:
						throw new IllegalStateException( "Unexpected value: " + freightConfig.getTimeWindowHandling() );
				}
			}
		} );

		// ... up to here.  kai, sep'20

	}

	@Provides
	Collection<LSPEventCreator> provideEventCreators(){
		return this.creators;
	}

	@Provides
	CarrierAgentTracker provideCarrierResourceTracker( LSPControlerListenerImpl lSPControlerListener ) {
		return lSPControlerListener.getCarrierResourceTracker();
	}

}
