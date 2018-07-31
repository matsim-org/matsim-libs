package org.matsim.core.mobsim.qsim;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.components.QSimComponentsModule;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLanesNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

import com.google.inject.Provides;

public class QSimModule extends com.google.inject.AbstractModule {
	@Inject Config config ;
	
	@Override
	protected void configure() {
		install(new QSimComponentsModule());
		
		bind(Mobsim.class).toProvider(QSimProvider.class);
		if ( config.qsim().isUseLanes() ) { 
			bind(QNetworkFactory.class).to( QLanesNetworkFactory.class ) ;
		} else {
			bind(QNetworkFactory.class).to( DefaultQNetworkFactory.class ) ;
		}
		if ( config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim() ) {
			bind( TransitStopHandlerFactory.class ).to( ComplexTransitStopHandlerFactory.class ) ;
		}
		// yy see MATSIM-756
	}

	@SuppressWarnings("static-method")
	@Provides
	Collection<AbstractQSimPlugin> provideQSimPlugins(Config config) {
		return getDefaultQSimPlugins(config);
	}
	
	static public Collection<AbstractQSimPlugin> getDefaultQSimPlugins(Config config) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		
		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));
		
		if (config.network().isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config));
		}
		
		if (config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim() ) {
			plugins.add(new TransitEnginePlugin(config));
		}
		
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new PopulationPlugin(config));
		
		return Collections.unmodifiableCollection(plugins);
	}
}
