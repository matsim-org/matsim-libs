package playground.sergioo.passivePlanning2012.core.scenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import playground.sergioo.passivePlanning2012.core.network.ComposedLinkFactory;
import playground.sergioo.passivePlanning2012.core.network.ComposedNode;

public class ScenarioSimplerNetwork implements Scenario {
	
	private final Scenario delegate ;
	
	//Attributes
	private final Map<String, Network> simplerNetworks = new HashMap<String, Network>();
	
	//Methods
	public ScenarioSimplerNetwork(Config config) {
		delegate = ScenarioUtils.createScenario( config ) ;
		
		
		Set<String> modes = new HashSet<String>();
		modes.addAll(config.plansCalcRoute().getNetworkModes());
		for(String mode:modes)
			simplerNetworks.put(mode, NetworkUtils.createNetwork());
	}
	public Network getSimplerNetwork(String mode) {
		return simplerNetworks.get(mode);
	}
	public void createSimplerNetwork(String mode, Set<ComposedNode> mainNodes) {
		for(ComposedNode mainNode:mainNodes)
			simplerNetworks.get(mode).addNode(mainNode);
		LinkFactory linkFactory = new ComposedLinkFactory(this.getNetwork(), simplerNetworks.get(mode), mode);
		long linkId = mainNodes.size();
		for(ComposedNode nodeA:mainNodes)
			for(ComposedNode nodeB:mainNodes) {
				Link link = linkFactory.createLink(Id.createLinkId(++linkId), nodeA, nodeB, simplerNetworks.get(mode), 0, 0, 0, 0);
				if(link!=null)
					simplerNetworks.get(mode).addLink(link);
			}	
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
	public void addScenarioElement(String name, Object o) {
		delegate.addScenarioElement(name, o);
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

	@Override
	public Lanes getLanes() {
		return delegate.getLanes();
	}

}
