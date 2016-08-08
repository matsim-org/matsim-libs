package org.matsim.contrib.carsharing.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class CarsharingAgentFactory implements AgentFactory{
	private final Netsim simulation;
	private CarSharingVehiclesNew carSharingVehicles;

	private LeastCostPathCalculator pathCalculator;
	private KeepingTheCarModel keepCarModel;
	
	
	public CarsharingAgentFactory(final Netsim simulation, final Scenario scenario, 
			CarSharingVehiclesNew carSharingVehicles, LeastCostPathCalculator pathCalculator, KeepingTheCarModel keepCarModel) {
		this.simulation = simulation;
		this.carSharingVehicles = carSharingVehicles;
		this.pathCalculator = pathCalculator;
		this.keepCarModel = keepCarModel;
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {	

		MobsimDriverAgent agent ;

		agent = new CarsharingPersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation,
				this.carSharingVehicles, pathCalculator, keepCarModel); 
		return agent;
	}
}
