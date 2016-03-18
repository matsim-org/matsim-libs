package org.matsim.contrib.carsharing.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class CarsharingAgentFactory implements AgentFactory{
	private final Netsim simulation;
	private CarSharingVehicles carSharingVehicles;

	private LeastCostPathCalculator pathCalculator;
	
	
	public CarsharingAgentFactory(final Netsim simulation, final Scenario scenario, 
			CarSharingVehicles carSharingVehicles, LeastCostPathCalculator pathCalculator) {
		this.simulation = simulation;
		this.carSharingVehicles = carSharingVehicles;
		this.pathCalculator = pathCalculator;
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {
		
		
		//if we want to simulate all agents then we have a normal AllCSModesPersonDriverAgentImpl agent 
		//if we want to simulate only carsharing members we would have PersonDriverAgentOnlyMembersImpl agent here

		MobsimDriverAgent agent ;
//		agent = new CarsharingPersonDriverAgentImplOLD(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()),
//				this.simulation, this.scenario, this.carSharingVehicles, this.tripRouter); 
		agent = new CarsharingPersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation,
				this.carSharingVehicles, pathCalculator); 
		return agent;
	}
}
