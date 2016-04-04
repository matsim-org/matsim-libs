package org.matsim.core.mobsim.qsim;


import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLanesNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class QSimModule extends AbstractModule {
	@Inject Config config ;
	
	@Override
	protected void configure() {
		bind(Mobsim.class).toProvider(QSimProvider.class);
		if ( config.qsim().isUseLanes() ) { 
			bind(QNetworkFactory.class).to( QLanesNetworkFactory.class ) ;
		} else {
			bind(QNetworkFactory.class).to( DefaultQNetworkFactory.class ) ;
		}
	}

	@SuppressWarnings("static-method")
	@Provides
	Collection<AbstractQSimPlugin> provideQSimPlugins(TransitConfigGroup transitConfigGroup, NetworkConfigGroup networkConfigGroup, Config config) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));
		if (networkConfigGroup.isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config));
		}
		if (transitConfigGroup.isUseTransit()) {
			plugins.add(new TransitEnginePlugin(config));
		}
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new PopulationPlugin(config));
		return plugins;
	}
}
