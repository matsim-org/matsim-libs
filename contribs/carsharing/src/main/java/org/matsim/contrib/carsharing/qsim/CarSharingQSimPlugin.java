package org.matsim.contrib.carsharing.qsim;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CarSharingQSimPlugin extends AbstractQSimPlugin {
	static public String CARSHARING_PARKING_VEHICLES_SOURCE = "CarsharingParkingVehiclesSource";

	private final CarsharingSupplyInterface carsharingSupply;
	private final CarsharingManagerInterface carsharingManager;

	public CarSharingQSimPlugin(Config config, CarsharingSupplyInterface carsharingSupply,
			CarsharingManagerInterface carsharingManager) {
		super(config);
		this.carsharingSupply = carsharingSupply;
		this.carsharingManager = carsharingManager;
	}

	public Collection<? extends Module> modules() {
		return Collections.singleton(new AbstractModule() {
			@Override
			protected void configure() {
				bind(PopulationAgentSource.class).asEagerSingleton();
			}

			@Provides
			@Singleton
			ParkCSVehicles provideParkCSVehicles(QSim qsim) {
				return new ParkCSVehicles(qsim, carsharingSupply);
			}

			@Provides
			@Singleton
			AgentFactory provideAgentFactory(Netsim netsim) {
				return new CSAgentFactory(netsim, carsharingManager);
			}
		});
	}

	public Map<String, Class<? extends AgentSource>> agentSources() {
		Map<String, Class<? extends AgentSource>> sources = new HashMap<>();
		sources.put(PopulationPlugin.POPULATION_SOURCE_NAME, PopulationAgentSource.class);
		sources.put(CARSHARING_PARKING_VEHICLES_SOURCE, ParkCSVehicles.class);
		return sources;
	}
}
