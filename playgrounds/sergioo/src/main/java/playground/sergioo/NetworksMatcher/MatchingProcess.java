package playground.sergioo.NetworksMatcher;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;


public class MatchingProcess {
	
	//Attributes
	private final List<Step> steps;
	
	//Methods
	public MatchingProcess() {
		steps = new ArrayList<Step>();
	}
	public void execute(Network networkA, Network networkB) {
		for(Step step:steps) {
			Network[] networks = step.execute(networkA, networkB);
			networkA = networks[0];
			networkB = networks[1];
		}
	}
	
}
