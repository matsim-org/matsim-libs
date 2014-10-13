package playground.pieter.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

public class StochasticRouter implements LeastCostPathCalculator {
	private final Network network;
	private final TravelDisutility travelCosts;
	private final TravelTime travelTimes;
	// sensitivity parameter
    private final double beta;

	public StochasticRouter(Network network, TravelDisutility travelCosts,
			TravelTime travelTimes, double beta) {
		super();
		this.network = network;
		this.travelCosts = travelCosts;
		this.travelTimes = travelTimes;
		this.beta = beta;
	}

	@Override
	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime,
			Person person, Vehicle vehicle) {

		// we are going to tag attributes to links and nodes
		HashMap<Link, Double> linkCosts = new HashMap<>();
		HashMap<Node, Double> nodeCosts = new HashMap<>();
		ArrayList<Node> nodesToVisit = new ArrayList<>();
		int nodeIndex = -1;

		// iterate through the list of nodes
		double nodeCost = 0;
		Node currentNode = toNode;
		while (nodeIndex < nodesToVisit.size()) {
			if (nodeIndex >= 0) {
				currentNode = nodesToVisit.get(nodeIndex);
				nodeCost = nodeCosts.get(currentNode);
			}
			Collection<? extends Link> inLinks = currentNode.getInLinks()
					.values();
			// post the cost to the node on the other side of the link
			for (Link link : inLinks) {
				if (!linkCosts.containsKey(link)) {
					double linkCost = travelCosts.getLinkTravelDisutility(link,
							0, person, vehicle);
					linkCosts.put(link, nodeCost + linkCost);

					Node nextNode = link.getFromNode();
					if (nodeCosts.containsKey(nextNode)) {
						if (nodeCosts.get(nextNode) > nodeCost + linkCost) {
							nodeCosts.put(nextNode, nodeCost + linkCost);
							nodesToVisit.add(nextNode);
						}
					} else {
						nodeCosts.put(nextNode, nodeCost + linkCost);
						nodesToVisit.add(nextNode);
					}
				}
			}
			nodeIndex++;
		}

		// do the actual routing
		List<Node> nodes = new ArrayList<>();
		List<Link> links = new ArrayList<>();
		double travelTime = starttime;
		double travelCost = 0;
		currentNode = fromNode;
		double linkCost = 0;
		while (currentNode != toNode) {
			List<Link> outLinks = new ArrayList<>();
			outLinks.addAll(currentNode.getOutLinks().values());
			double[] utils = new double[outLinks.size()];
			// double[] probs = new double[outLinks.size()];
			double total = 0;
			nodeCost = nodeCosts.get(currentNode);
			for (int i = 0; i < outLinks.size(); i++) {
				utils[i] = -1;
				// probs[i] = -1;
				Link link = outLinks.get(i);
				// never go upstream
				linkCost = linkCosts.get(link);
				double contribution = linkCost <= nodeCost?Math.pow(linkCost,-1*beta):Math.pow(linkCost,-2*beta);
//				if (linkCost <= nodeCost) {
////					double contribution = Math.exp(-1 * beta * linkCost);
////					total += contribution;
////					utils[i] = contribution;
//					double contribution = Math.pow(linkCost,beta);
//				}else{
//					
//				}
				total += contribution;
				utils[i] = contribution;
			}
			// convert utils>0 to probabilities
			double sampleProb = MatsimRandom.getRandom().nextDouble() * total;
			double currentCumulativeProb = 0;
			Link selection = null;
			for (int i = 0; i < outLinks.size(); i++) {
				if (utils[i] > 0) {
					currentCumulativeProb += utils[i];
					if (currentCumulativeProb >= sampleProb) {
						selection = outLinks.get(i);
						links.add(selection);
						nodes.add(currentNode);
						travelCost += travelCosts.getLinkTravelDisutility(
								selection, travelTime, person, vehicle);
						travelTime += travelTimes.getLinkTravelTime(selection,
								travelTime, person, vehicle);
						break;

						// ((Link)network.getLinks().get(selection.getId())).setCapacity(1000000000);
					}
				}
			}
			currentNode = selection.getToNode();
		}
		// new NetworkWriter(network).write("data/testRouter.xml");
		return new Path(nodes, links, travelTime, travelCost);
		// return null;
	}

	public static void main(String[] args) {
		Scenario scenario;
		MatsimRandom.reset(123);
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario)
				.readFile("data/zurich/altnet_CLEAN.xml");
		Network network = scenario.getNetwork();
		StochasticRouter stochasticRouter = new StochasticRouter(network,
				new TravelDisutility() {

					@Override
					public double getLinkTravelDisutility(Link link,
							double time, Person person, Vehicle vehicle) {
						return link.getLength() / link.getFreespeed();
					}

					@Override
					public double getLinkMinimumTravelDisutility(Link link) {
						return link.getLength() / link.getFreespeed();
					}
				}, new TravelTime() {

					@Override
					public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
						return link.getLength() / link.getFreespeed();
					}
				}, 0.0001);



	}
}
