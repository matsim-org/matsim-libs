package org.matsim.contrib.carsharing.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CarsharingModule extends AbstractModule {

	@Provides
	@Singleton
	public PopulationAgentSource provideAgentSource(QSim qsim, Scenario scenario, AgentFactory agentFactory) {
		return new PopulationAgentSource(scenario.getPopulation(), agentFactory, qsim);
	}

	@Provides
	@Singleton
	public ParkCSVehicles provideAgentSource(QSim qsim, CarsharingSupplyInterface carsharingSupply) {
		return new ParkCSVehicles(qsim, carsharingSupply);
	}

	@Override
	protected void configure() {

	}

}
