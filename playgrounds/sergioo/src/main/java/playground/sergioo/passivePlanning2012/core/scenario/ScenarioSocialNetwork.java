package playground.sergioo.passivePlanning2012.core.scenario;

import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;

import playground.sergioo.passivePlanning2012.core.population.socialNetwork.SocialNetwork;

public class ScenarioSocialNetwork extends ScenarioImpl {
	
	//Attributes
	private SocialNetwork socialNetwork = new SocialNetwork();
	
	//Methods
	public ScenarioSocialNetwork(Config config) {
		super(config);
	}
	public SocialNetwork getSocialNetwork() {
		return socialNetwork;
	}

}
