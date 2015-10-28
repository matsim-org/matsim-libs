package playground.andreas.bln.pop;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

/**
 * Provides a real scenario, but exchanges the population.
 * Still, network and facilities can be reused that way.
 *
 * @author mrieser
 */
public class SharedNetScenario implements Scenario {

	private final MutableScenario scenario;
	private Population myPopulation;

	public SharedNetScenario(final MutableScenario scenario, final Population population) {
		this.scenario = scenario;
		this.myPopulation = population;
	}

	@Override
	public Population getPopulation() {
		return this.myPopulation;
	}
	
	@Override
	public TransitSchedule getTransitSchedule() {
		return this.scenario.getTransitSchedule();
	}
	
	@Override
	public ActivityFacilities getActivityFacilities() {
		return this.scenario.getActivityFacilities();
	}

	@Override
	public Config getConfig() {
		return this.scenario.getConfig();
	}

	@Override
	public Network getNetwork() {
		return this.scenario.getNetwork();
	}

	@Override
	public void addScenarioElement(String name, Object o) {
		this.scenario.addScenarioElement(name , o);
	}

	@Override
	public Object getScenarioElement(String name) {
		return this.scenario.getScenarioElement(name);
	}

	@Override
	public Vehicles getTransitVehicles() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Households getHouseholds() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public LaneDefinitions20 getLanes() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Vehicles getVehicles() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}
