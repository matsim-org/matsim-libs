package org.matsim.contrib.carsharing.relocation.qsim;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.contrib.carsharing.qsim.CarsharingQSimPlugin;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

import com.google.inject.Provides;

public class RelocationQSimModule extends AbstractModule {
	@Provides
	public Collection<AbstractQSimPlugin> provideQSimPlugins(Config config, QNetworkFactory networkFactory) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();

		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));

		if (config.network().isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config));
		}
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new CarsharingQSimPlugin(config));
		plugins.add(new RelocationQSimPlugin(config));

		return plugins;
	}

	@Override
	public void install() {
	}
}
