package org.matsim.core.mobsim.qsim.components;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsQSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.pt.TransitQSimComponentsConfigurator;

public class StandardQSimComponentsConfigurator implements QSimComponentsConfigurator {
	final private Config config;

	public StandardQSimComponentsConfigurator(Config config) {
		this.config = config;
	}

	@Override
	public void configure(QSimComponents components) {
		new DefaultQSimComponentsConfigurator().configure(components);
		new QSimComponentsFromConfigConfigurator(config).configure(components);
		new TransitQSimComponentsConfigurator(config).configure(components);
		new NetworkChangeEventsQSimComponentsConfigurator(config).configure(components);
	}

}
