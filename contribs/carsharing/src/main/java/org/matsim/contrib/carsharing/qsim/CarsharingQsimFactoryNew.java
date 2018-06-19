package org.matsim.contrib.carsharing.qsim;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
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

	@Override
	public Mobsim get() {
		final QSim qsim = new QSim(scenario, eventsManager);
		//QSimUtils.createDefaultQSim(scenario, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qsim.getAgentCounter());
		qsim.addMobsimEngine(activityEngine);
		qsim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = new QNetsimEngine(qsim);
		qsim.addMobsimEngine(netsimEngine);
		qsim.addDepartureHandler(netsimEngine.getDepartureHandler());
		DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(scenario, eventsManager);
		qsim.addMobsimEngine(teleportationEngine);
		qsim.addDepartureHandler(teleportationEngine) ;
		AgentFactory agentFactory = new CSAgentFactory(qsim, carsharingManager);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qsim);
        qsim.addAgentSource(agentSource);		
	
		ParkCSVehicles parkSource = new ParkCSVehicles( qsim,
				carsharingSupply);
		qsim.addAgentSource(parkSource);
		return qsim;
	}

}
