package org.matsim.contrib.parking.parkingsearch.sim;

import java.util.Collection;
import java.util.LinkedList;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Provides;


public class ParkingSearchQSimModule extends com.google.inject.AbstractModule {
	@Override 
	protected void configure() {
		bind(Mobsim.class).toProvider(QSimProvider.class);
	}
	
	@SuppressWarnings("static-method")
	@Provides 
	Collection<AbstractQSimModule> provideQSimModules(TransitConfigGroup transitConfigGroup, NetworkConfigGroup networkConfigGroup, Config config) {
		Collection<AbstractQSimModule> modules = new LinkedList<>(QSimModule.getDefaultQSimModules());
		modules.removeIf(PopulationModule.class::isInstance);
		modules.add(new ParkingSearchPopulationModule());
		return modules;
	}
	
	@Provides
	QSimComponents provideQSimComponents(Config config) {
		QSimComponents components = new QSimComponents();
		new StandardQSimComponentsConfigurator(config).configure(components);
		
		components.activeAgentSources.remove(PopulationModule.POPULATION_AGENT_SOURCE_NAME);
		components.activeAgentSources.add(ParkingSearchPopulationModule.PARKING_SEARCH_AGENT_SOURCE_NAME);
		
		return components;
	}
}