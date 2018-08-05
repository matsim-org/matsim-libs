package org.matsim.core.mobsim.qsim;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsModule;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueueModule;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.SimpleTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLanesNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

public class QSimModule extends com.google.inject.AbstractModule {
	@Inject Config config ;
	
	@Override
	protected void configure() {
		install(new QSimComponentsModule());
		bind(new TypeLiteral<Collection<AbstractQSimModule>>() {}).toInstance(getDefaultQSimModules());
		
		bind(Mobsim.class).toProvider(QSimProvider.class);
		if ( config.qsim().isUseLanes() ) { 
			bind(QNetworkFactory.class).to( QLanesNetworkFactory.class ) ;
		} else {
			bind(QNetworkFactory.class).to( DefaultQNetworkFactory.class ) ;
		}
		
		if ( config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim() ) {
			bind( TransitStopHandlerFactory.class ).to( ComplexTransitStopHandlerFactory.class ) ;
		} else {
			// Explicit bindings are required, so although it may not be used, we need provide something.
			bind( TransitStopHandlerFactory.class ).to( SimpleTransitStopHandlerFactory.class );
		}
		// yy see MATSIM-756
	}

	static public Collection<AbstractQSimModule> getDefaultQSimModules() {
		return Arrays.asList(
				new MessageQueueModule(),
				new ActivityEngineModule(),
				new QNetsimEngineModule(),
				new TeleportationModule(),
				new PopulationModule(),
				new NetworkChangeEventsModule(),
				new TransitEngineModule()
				);
	}
}
