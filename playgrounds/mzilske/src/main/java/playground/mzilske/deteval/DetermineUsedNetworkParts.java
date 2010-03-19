/**
 * 
 */
package playground.mzilske.deteval;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public final class DetermineUsedNetworkParts extends AbstractPersonAlgorithm {
	
	private final Network network;
	
	private Collection<Node> usedNodes = new HashSet<Node>();

	public Collection<Node> getUsedNodes() {
		return usedNodes;
	}

	public DetermineUsedNetworkParts(Network networkLayer) {
		this.network = networkLayer;
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if (leg.getRoute() instanceof NetworkRoute) {
						NetworkRoute route = (NetworkRoute) leg.getRoute();
						for (Id linkId : route.getLinkIds()) {
							Link link = network.getLinks().get(linkId);
							usedNodes.add(link.getFromNode());
							usedNodes.add(link.getToNode());
						}
					}
				} else if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					Id linkId = activity.getLinkId();
					Link link = network.getLinks().get(linkId);
					usedNodes.add(link.getFromNode());
					usedNodes.add(link.getToNode());
				}
			}
		}
	}
}