package org.matsim.contrib.carsharing.qsim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.utils.timing.TimeInterpretation;

import com.google.inject.Inject;

public class CSAgentFactory implements AgentFactory{

	private final Netsim simulation;
	private CarsharingManagerInterface carsharingManager;
	private final TimeInterpretation timeInterpretation;
	
	@Inject
	public CSAgentFactory(Netsim simulation, CarsharingManagerInterface carsharingManager, TimeInterpretation timeInterpretation) {

		this.simulation = simulation;
		this.carsharingManager = carsharingManager;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {

		return new CarsharingPersonDriverAgentImpl(p.getSelectedPlan(), this.simulation, this.carsharingManager, this.timeInterpretation);
	}

}
