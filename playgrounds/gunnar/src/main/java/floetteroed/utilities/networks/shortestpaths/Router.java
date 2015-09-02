/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.networks.shortestpaths;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.networks.basic.BasicLink;
import floetteroed.utilities.networks.basic.BasicNetwork;
import floetteroed.utilities.networks.basic.BasicNode;


/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Router {

	// -------------------- CONSTANTS --------------------

	protected enum Direction {
		FWD, BWD
	};

	protected final BasicNetwork network;

	protected final LinkCost linkCost;

	// -------------------- CONSTRUCTION --------------------

	public Router(final BasicNetwork network, final LinkCost linkCost) {
		if (network == null) {
			throw new IllegalArgumentException("network is null");
		}
		if (linkCost == null) {
			throw new IllegalArgumentException("link cost is null");
		}
		this.network = network;
		this.linkCost = linkCost;
	}

	// -------------------- GETTERS --------------------

	// TODO check if needed -- network is only required internally!
	public BasicNetwork getNetwork() {
		return this.network;
	}

	// -------------------- INTERNALS --------------------

	static double treeCost(final BasicNode node, final Map<BasicNode, Double> treeCost) {
		final Double result = treeCost.get(node);
		if (result == null) {
			return Double.POSITIVE_INFINITY;
		} else {
			return result;
		}
	}

	private void expand(final BasicNode currentNode, final BasicNode candidateNode,
			final BasicLink connectingLink, final UnsettledNodes unsettled) {
		final double oldCost = unsettled.cost(candidateNode);
		final double newCost = this.linkCost.getCost(connectingLink)
				+ unsettled.cost(currentNode);
		unsettled.update(candidateNode, Math.min(oldCost, newCost));
	}

	protected Map<BasicNode, Double> treeCost(final BasicNode root,
			final Set<BasicNode> targets, final Direction direction,
			UnsettledNodes unsettled, Set<BasicNode> settled) {
		/*
		 * (1) define set of feasible targets
		 */
		final Set<BasicNode> targetsLeft;
		if (targets == null) {
			targetsLeft = new HashSet<BasicNode>();
			// TODO NEW >>>>>
			targetsLeft.add(new BasicNode(">>>>> NON-EXISTING NODE <<<<<"));
			// targetsLeft.add(new Node(new Object()));
			// TODO NEW <<<<<
		} else {
			targetsLeft = new HashSet<BasicNode>(targets);
		}
		/*
		 * (2) initialize data structures and search loop
		 */
		BasicNode currentNode;
		if (unsettled == null) {
			unsettled = new UnsettledNodes();
			currentNode = root;
			unsettled.update(root, 0.0);
		} else {
			currentNode = unsettled.first();
		}
		if (settled == null) {
			settled = new HashSet<BasicNode>();
		}
		/*
		 * (3) search until all reachable targets are found
		 */
		while (currentNode != null && !targetsLeft.isEmpty()) {
			if (Direction.FWD.equals(direction)) {
				/*
				 * (3-A) expand forwards
				 */
				for (BasicLink outLink : currentNode.getOutLinks()) {
					final BasicNode outNode = outLink.getToNode();
					if (!settled.contains(outNode)) {
						this.expand(currentNode, outNode, outLink, unsettled);
					}
				}
			} else {
				/*
				 * (3-B) expand backwards
				 */
				for (BasicLink inLink : currentNode.getInLinks()) {
					final BasicNode inNode = inLink.getFromNode();
					if (!settled.contains(inNode)) {
						this.expand(currentNode, inNode, inLink, unsettled);
					}
				}
			}
			settled.add(currentNode);
			unsettled.remove(currentNode);
			targetsLeft.remove(currentNode);
			if (unsettled.isEmpty()) {
				currentNode = null;
			} else {
				currentNode = unsettled.first();
			}
		}
		return unsettled.cost();
	}

	private Map<BasicNode, Double> treeCost(final BasicNode root,
			final Set<BasicNode> targets, final Direction direction) {
		return this.treeCost(root, targets, direction, null, null);
	}

	// -------------------- ROUTING IMPLEMENTATIONS --------------------

	public Map<BasicNode, Double> fwdCost(final BasicNode origin,
			final Set<BasicNode> destinations) {
		return this.treeCost(origin, destinations, Direction.FWD);
	}

	public Map<BasicNode, Double> bwdCost(final Set<BasicNode> origins,
			final BasicNode destination) {
		return this.treeCost(destination, origins, Direction.BWD);
	}

	public Map<BasicNode, Double> fwdCost(final BasicNode origin, final BasicNode destination) {
		final Set<BasicNode> destinations = new HashSet<BasicNode>();
		destinations.add(destination);
		return this.fwdCost(origin, destinations);
	}

	public Map<BasicNode, Double> fwdCost(final BasicNode origin) {
		return this.fwdCost(origin, new HashSet<BasicNode>(this.network.getNodes()));
	}

	public Map<BasicNode, Double> bwdCost(final BasicNode destination) {
		return this.bwdCost(new HashSet<BasicNode>(this.network.getNodes()),
				destination);
	}

	public Map<BasicNode, Double> bwdCost(final BasicNode origin, final BasicNode destination) {
		final Set<BasicNode> origins = new HashSet<BasicNode>();
		origins.add(origin);
		return this.bwdCost(origins, destination);
	}

	private Map<BasicNode, LinkedList<BasicNode>> bestRoutes(final Set<BasicNode> origins,
			final BasicNode destination, final Map<BasicNode, Double> bwdCostTree) {
		final Map<BasicNode, LinkedList<BasicNode>> result = new HashMap<BasicNode, LinkedList<BasicNode>>();
		for (BasicNode origin : origins) {
			LinkedList<BasicNode> route = new LinkedList<BasicNode>();
			route.addFirst(origin);
			while (route != null && !route.getLast().equals(destination)) {
				BasicNode bestNode = null;
				double bestCost = Double.POSITIVE_INFINITY;
				for (BasicLink candLink : route.getLast().getOutLinks()) {
					final BasicNode candNode = candLink.getToNode();
					final double bwdCost = treeCost(candNode, bwdCostTree);
					double candCost = this.linkCost.getCost(candLink) + bwdCost;
					if (candCost < bestCost) {
						bestNode = candNode;
						bestCost = candCost;
					}
				}
				if (Double.isInfinite(bestCost)) {
					route = null;
				} else {
					route.addLast(bestNode);
				}
			}
			result.put(origin, route);
		}
		return result;
	}

	private Map<BasicNode, LinkedList<BasicNode>> bestRoutes(final BasicNode origin,
			final Set<BasicNode> destinations, final Map<BasicNode, Double> fwdCostTree) {
		final Map<BasicNode, LinkedList<BasicNode>> result = new HashMap<BasicNode, LinkedList<BasicNode>>();
		for (BasicNode destination : destinations) {
			LinkedList<BasicNode> route = new LinkedList<BasicNode>();
			route.addLast(destination);
			while (route != null && !route.getFirst().equals(origin)) {
				BasicNode bestNode = null;
				double bestCost = Double.POSITIVE_INFINITY;
				for (BasicLink candLink : route.getFirst().getInLinks()) {
					final BasicNode candNode = candLink.getFromNode();
					final double fwdCost = treeCost(candNode, fwdCostTree);
					double candCost = this.linkCost.getCost(candLink) + fwdCost;
					if (candCost < bestCost) {
						bestNode = candNode;
						bestCost = candCost;
					}
				}
				if (Double.isInfinite(bestCost)) {
					route = null;
				} else {
					route.addFirst(bestNode);
				}
			}
			result.put(destination, route);
		}
		return result;
	}

	public LinkedList<BasicNode> bestRouteBwd(final BasicNode origin,
			final BasicNode destination, final Map<BasicNode, Double> bwdCost) {
		final Set<BasicNode> origins = new HashSet<BasicNode>();
		origins.add(origin);
		return this.bestRoutes(origins, destination, bwdCost).get(origin);
	}

	public LinkedList<BasicNode> bestRouteFwd(final BasicNode origin,
			final BasicNode destination, final Map<BasicNode, Double> fwdCost) {
		final Set<BasicNode> destinations = new HashSet<BasicNode>();
		destinations.add(destination);
		return this.bestRoutes(origin, destinations, fwdCost).get(destination);
	}

	public Map<BasicNode, LinkedList<BasicNode>> bestRoutes(final BasicNode origin,
			final Set<BasicNode> destinations) {
		final Map<BasicNode, Double> fwdCost = this.fwdCost(origin, destinations);
		return this.bestRoutes(origin, destinations, fwdCost);
	}

	public Map<BasicNode, LinkedList<BasicNode>> bestRoutes(final Set<BasicNode> origins,
			final BasicNode destination) {
		final Map<BasicNode, Double> bwdCost = this.bwdCost(origins, destination);
		return this.bestRoutes(origins, destination, bwdCost);
	}

	public LinkedList<BasicNode> bestRoute(final BasicNode origin, final BasicNode destination) {
		final Set<BasicNode> destinations = new HashSet<BasicNode>();
		destinations.add(destination);
		return this.bestRoutes(origin, destinations).get(destination);
	}

	// public double bestRouteCost(final Node origin, final Node destination) {
	//
	//
	// }

	// -------------------- SUPPLEMENTARY IMPLEMENTATIONS --------------------

	/**
	 * TODO End parameter is inclusive!
	 * 
	 * TODO made this static
	 */
	public static boolean equals(final List<BasicNode> route1, final int start1,
			final int end1, List<BasicNode> route2, final int start2, final int end2) {
		if (end1 - start1 != end2 - start2) {
			return false;
		} else {
			for (int i = 0; i <= end1 - start1; i++) {
				if (!route1.get(start1 + i).equals(route2.get(start2 + i))) {
					return false;
				}
			}
		}
		return true;
	}

	private static BasicLink connectingLink(final BasicNode from, final BasicNode to) {
		for (BasicLink link : from.getOutLinks()) {
			if (link.getToNode().equals(to)) {
				return link;
			}
		}
		return null;
	}

	public static List<BasicLink> toLinkRoute(final List<BasicNode> nodeRoute) {
		final List<BasicLink> linkRoute = new LinkedList<BasicLink>();
		for (int i = 0; i < nodeRoute.size() - 1; i++) {
			final BasicLink link = Router.connectingLink(nodeRoute.get(i),
					nodeRoute.get(i + 1));
			if (link == null) {
				return null;
			} else {
				linkRoute.add(link);
			}
		}
		return linkRoute;
	}

	public double cost(final List<BasicLink> linkRoute) {
		double result = 0;
		for (BasicLink link : linkRoute) {
			result += this.linkCost.getCost(link);
		}
		return result;
	}

	// -------------------- TODO NEW --------------------

	public Map<BasicNode, Double> costWithoutExcludedNodes(final BasicNode root,
			final BasicNode target, final Collection<BasicNode> allNodes,
			final Collection<BasicNode> excludedNodes,
			final Map<BasicNode, Double> treeCost, final Direction direction) {
		/*
		 * (1) simple case: the root node is excluded
		 */
		if (excludedNodes.contains(root)) {
			throw new IllegalArgumentException(
					"trying to compute SP cost tree with forbidden root");
		}
		if (excludedNodes.contains(target)) {
			throw new IllegalArgumentException(
					"trying to compute SP cost tree to forbidden target");
		}
		/*
		 * (2) compute completely new tree cost
		 */
		final Set<BasicNode> targets = new LinkedHashSet<BasicNode>();
		targets.add(target);
		final LinkCost myLinkCost = new LinkCostExcludingNodes(this.linkCost,
				excludedNodes);
		final Router myRouter = new Router(this.getNetwork(), myLinkCost);
		final Map<BasicNode, Double> myTreeCost = myRouter.treeCost(root, targets,
				direction, null, null);
		/*
		 * (3) simply return the result
		 */
		return myTreeCost;
	}

	public Map<BasicNode, Double> fwdCostWithoutExcludedNodes(final BasicNode origin,
			final BasicNode destination, final Collection<BasicNode> allNodes,
			final Collection<BasicNode> excludedNodes,
			final Map<BasicNode, Double> fwdCost) {
		return this.costWithoutExcludedNodes(origin, destination, allNodes,
				excludedNodes, fwdCost, Direction.FWD);
	}

	public Map<BasicNode, Double> bwdCostWithoutExcludedNodes(
			final BasicNode destination, final BasicNode origin,
			final Collection<BasicNode> allNodes,
			final Collection<BasicNode> excludedNodes,
			final Map<BasicNode, Double> bwdCost) {
		return this.costWithoutExcludedNodes(destination, origin, allNodes,
				excludedNodes, bwdCost, Direction.BWD);
	}
}
