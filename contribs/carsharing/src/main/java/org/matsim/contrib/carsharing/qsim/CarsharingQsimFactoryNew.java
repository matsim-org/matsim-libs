package org.matsim.contrib.carsharing.qsim;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActiveQSimBridge;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.AgentCounterImpl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

import com.google.inject.Provider;

/** 
 * 
 * @author balac
 */
public class CarsharingQsimFactoryNew implements Provider<Mobsim>{

	@Inject Scenario scenario;
	@Inject EventsManager eventsManager;
	@Inject CarsharingSupplyInterface carsharingSupply;
	@Inject private CarsharingManagerInterface carsharingManager;
	@Inject ActiveQSimBridge activeQSimBridge;

	@Override
	public Mobsim get() {
		MobsimTimer mobsimTimer = new MobsimTimer();
		AgentCounter agentCounter = new AgentCounterImpl();
		
		final QSim qsim = new QSim(scenario, eventsManager, agentCounter, mobsimTimer, activeQSimBridge);
		//QSimUtils.createDefaultQSim(scenario, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, agentCounter, mobsimTimer);
		qsim.addMobsimEngine(activityEngine);
		qsim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = new QNetsimEngine(scenario.getConfig(), scenario, eventsManager, mobsimTimer, agentCounter);
		qsim.addMobsimEngine(netsimEngine);
		qsim.addDepartureHandler(netsimEngine.getDepartureHandler());
		DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(scenario, eventsManager, mobsimTimer);
		qsim.addMobsimEngine(teleportationEngine);
		qsim.addDepartureHandler(teleportationEngine) ;
		AgentFactory agentFactory = new CSAgentFactory(carsharingManager, scenario, eventsManager, mobsimTimer);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, scenario.getConfig(), scenario, qsim);
        qsim.addAgentSource(agentSource);		
	
		ParkCSVehicles parkSource = new ParkCSVehicles( qsim, scenario,
				carsharingSupply);
		qsim.addAgentSource(parkSource);
		return qsim;
	}

}
