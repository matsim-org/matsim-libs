package org.matsim.contrib.carsharing.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.Inject;

public class CSAgentFactory implements AgentFactory{

	private CarsharingManagerInterface carsharingManager;
	final private Scenario scenario;
	final private EventsManager eventsManager;
	final private MobsimTimer mobsimTimer;
	
	@Inject
	public CSAgentFactory(CarsharingManagerInterface carsharingManager, Scenario scenario, EventsManager eventsManager, MobsimTimer mobsimTimer) {
		this.carsharingManager = carsharingManager;
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;
	}

	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {

		return new CarsharingPersonDriverAgentImpl(p.getSelectedPlan(), scenario, eventsManager, mobsimTimer, this.carsharingManager);
	}

}
