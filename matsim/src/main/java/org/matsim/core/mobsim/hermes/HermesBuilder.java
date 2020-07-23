package org.matsim.core.mobsim.hermes;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.AllowsConfiguration;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

public class HermesBuilder implements AllowsConfiguration {

	@Override
	public AllowsConfiguration addOverridingModule(AbstractModule abstractModule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AllowsConfiguration addOverridingQSimModule(AbstractQSimModule qsimModule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AllowsConfiguration addQSimModule(AbstractQSimModule qsimModule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AllowsConfiguration configureQSimComponents(QSimComponentsConfigurator configurator) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Hermes build(Scenario scenario, EventsManager eventsmanager) {
		return new Hermes(scenario, eventsmanager);
	}

}
