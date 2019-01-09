package org.matsim.contrib.carsharing.relocation.qsim;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.relocation.demand.CarsharingVehicleRelocationContainer;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RelocationAgentSource implements AgentSource {

	private static final Logger log = Logger.getLogger("dummy");

	private QSim qSim;

	private CarsharingVehicleRelocationContainer carsharingVehicleRelocation;

	private Provider<TripRouter> routerProvider;

	private CarsharingSupplyInterface carsharingSupply;
	
	private LeastCostPathCalculator lcpc;

	@Inject
	public RelocationAgentSource(Scenario scenario, QSim qSim, 
			CarsharingVehicleRelocationContainer carsharingVehicleRelocation, 
			Provider<TripRouter> routerProvider, CarsharingSupplyInterface carsharingSupply,
			LeastCostPathCalculator lcpc) {
		this.qSim = qSim;
		this.carsharingVehicleRelocation = carsharingVehicleRelocation;
		this.routerProvider = routerProvider;
		this.carsharingSupply = carsharingSupply;
		this.lcpc = lcpc;
	}

	@Override
	public void insertAgentsIntoMobsim() {
		int counter = 0;
		Guidance guidance = new Guidance (this.routerProvider.get(), lcpc);
		for (Entry<String, Map<Id<Person>, RelocationAgent>> companyEntry : this.carsharingVehicleRelocation.getRelocationAgents().entrySet()) {
			String companyId = companyEntry.getKey();
			List<Double> relocationTimes = this.carsharingVehicleRelocation.getRelocationTimes(companyId);
			counter =+ companyEntry.getValue().size();

			for (Entry<Id<Person>, RelocationAgent> agentEntry : companyEntry.getValue().entrySet()) {
				RelocationAgent agent = agentEntry.getValue();
				agent.setGuidance(guidance);
				agent.setMobsimTimer(this.qSim.getSimTimer());
				agent.setCarsharingSupplyContainer(this.carsharingSupply);
				agent.setRelocationTimes(relocationTimes);

				this.qSim.insertAgentIntoMobsim(agent);
			}
		}

		log.info("inserted " + counter + " relocation agents into qSim");
	}

}
