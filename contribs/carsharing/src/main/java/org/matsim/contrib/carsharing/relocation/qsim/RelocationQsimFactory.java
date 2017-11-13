package org.matsim.contrib.carsharing.relocation.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.qsim.CSAgentFactory;
import org.matsim.contrib.carsharing.qsim.CarsharingPersonDriverAgentImpl;
import org.matsim.contrib.carsharing.qsim.ParkCSVehicles;
import org.matsim.contrib.carsharing.relocation.demand.CarsharingVehicleRelocationContainer;
import org.matsim.contrib.carsharing.relocation.events.MobismBeforeSimStepRelocationListener;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 *
 *
 *
 */

public class RelocationQsimFactory implements Provider<Mobsim>{


	@Inject private Scenario scenario;
	@Inject private EventsManager eventsManager;
	
	@Inject private CarsharingSupplyInterface carsharingSupply;
	@Inject private CarsharingManagerInterface carsharingManager;

	@Inject private CarsharingVehicleRelocationContainer carsharingVehicleRelocation;
	@Inject private Provider<TripRouter> routerProvider;
	
	@Inject private MobismBeforeSimStepRelocationListener mobismBeforeSimStepRelocationListener;

	@Override
	public Mobsim get() {
		QSim qSim = new QSim(this.scenario, this.eventsManager);

		ActivityEngine activityEngine = new ActivityEngine(this.eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);
		qSim.addDepartureHandler(teleportationEngine);
		
		AgentFactory agentFactory = new CSAgentFactory(qSim, this.carsharingManager);

		if (this.scenario.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());
		}

		PopulationAgentSource agentSource = new PopulationAgentSource(this.scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		ParkCSVehicles parkSource = new ParkCSVehicles(qSim, this.carsharingSupply);
		qSim.addAgentSource(parkSource);

		RelocationAgentSource relocationAgentSource = new RelocationAgentSource(this.scenario, qSim, this.carsharingVehicleRelocation, this.routerProvider, this.carsharingSupply);
		qSim.addAgentSource(relocationAgentSource);

		
		qSim.addQueueSimulationListeners(mobismBeforeSimStepRelocationListener);
		return qSim;
	}
		
}
