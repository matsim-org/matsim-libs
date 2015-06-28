package playground.sergioo.passivePlanning2012.core.scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import playground.sergioo.passivePlanning2012.core.population.socialNetwork.SocialNetwork;

public class ScenarioSocialNetwork implements Scenario {
	private final Scenario delegate ;
	
	//Attributes
	private SocialNetwork socialNetwork = new SocialNetwork();
	
	//Methods
	public ScenarioSocialNetwork(Config config) {
		delegate = ScenarioUtils.createScenario( config ) ;
	}
	public SocialNetwork getSocialNetwork() {
		return socialNetwork;
	}
	@Override
	public Network getNetwork() {
		return delegate.getNetwork();
	}
	@Override
	public Population getPopulation() {
		return delegate.getPopulation();
	}
	@Override
	public TransitSchedule getTransitSchedule() {
		return delegate.getTransitSchedule();
	}
	@Override
	public Config getConfig() {
		return delegate.getConfig();
	}
	@Override
	public Coord createCoord(double x, double y) {
		return delegate.createCoord(x, y);
	}
	@Override
	public void addScenarioElement(String name, Object o) {
		delegate.addScenarioElement(name, o);
	}
	@Override
	public Object removeScenarioElement(String name) {
		return delegate.removeScenarioElement(name);
	}
	@Override
	public Object getScenarioElement(String name) {
		return delegate.getScenarioElement(name);
	}
	@Override
	public ActivityFacilities getActivityFacilities() {
		return delegate.getActivityFacilities();
	}
	@Override
	public Vehicles getTransitVehicles() {
		return delegate.getTransitVehicles();
	}
	@Override
	public Vehicles getVehicles() {
		return delegate.getVehicles();
	}
	@Override
	public Households getHouseholds() {
		return delegate.getHouseholds();
	}

}
