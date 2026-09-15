package org.matsim.dsim.simulation.net;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.Steppable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implement the move node logic in the engine. The intersection update touches in and out links
 * of nodes. I find it not fitting to put that behaviour into the node. This way, the node holds
 * the graph information, but the engine, which holds nodes and links performs the mutations on the
 * links.
 */
public class ActiveNodes implements Steppable {

	private final Set<SimNode> activeNodes = new HashSet<>();
	private final EventsManager em;

	@Inject
	public ActiveNodes(EventsManager em) {
		this.em = em;
	}

	int size() {
		return activeNodes.size();
	}

	void activate(SimNode node) {
		activeNodes.add(node);
	}

	@Override
	public void doSimStep(double now) {
		// use iterator, so that we can directly remove inactive nodes
		var it = activeNodes.iterator();
		while (it.hasNext()) {
			var node = it.next();
			var keepActive = moveNode(node, now);
			if (!keepActive) {
				it.remove();
			}
		}
	}

	/**
	 * Implement the move node logic in the engine. The intersection update touches in and out links
	 * of nodes. I find it not fitting to put that behaviour into the node. This way, the node holds
	 * the graph information, but the engine, which holds nodes and links performs the mutations on the
	 * links.
	 */
	private boolean moveNode(SimNode node, double now) {

		var availableCapacity = node.calculateAvailableCapacity();
		var exhaustedLinks = node.createExhaustedLinks();
		var selectedCapacity = 0.;

		// do the actual update
		while (availableCapacity > 1e-10) {
			for (var i = 0; i < node.getInLinks().size(); i++) {
				if (exhaustedLinks[i]) continue;

				var inLink = node.getInLinks().get(i);

				if (shouldVehicleMove(inLink, node.getOutLinks(), now)) {
					selectedCapacity += inLink.getMaxFlowCapacity();
					var rndNum = node.nextDouble() * availableCapacity;
					// this test ensures that links with higher capacities have a higher probability of being served first.
					if (selectedCapacity >= rndNum) {
						var vehicle = inLink.popVehicle();
						move(inLink, vehicle, node.getOutLinks(), now);
					}
				} else {
					exhaustedLinks[i] = true;
					availableCapacity -= inLink.getMaxFlowCapacity();
				}
			}
		}
		return node.isActiveInNextTimestep();
	}

	private boolean shouldVehicleMove(SimLink inLink, Map<Id<Link>, SimLink> outLinks, double now) {

		// first check if the inLink has a vehicle which can leave
		if (!inLink.isOffering())
			return false;

		// check if next link has space, or if the vehicle is stuck.
		// move the vehicle if either the next link has sufficient space, or if the vehicle
		// has reached its stuck time, move it regardless
		DistributedMobsimVehicle vehicle = inLink.peekFirstVehicle();
		Id<Link> nextLinkId = vehicle.getDriver().chooseNextLinkId();
		assert nextLinkId != null : "Vehicle %s has no next route element".formatted(vehicle.getId());

		SimLink nextLink = outLinks.get(nextLinkId);
		assert nextLink != null : "Next link %s not found in outLinks".formatted(nextLinkId);

		return nextLink.isAccepting(SimLink.LinkPosition.QStart, now) || inLink.isStuck(now);
	}

	private void move(SimLink inLink, DistributedMobsimVehicle vehicle, Map<Id<Link>, SimLink> outLinks, double now) {

		em.processEvent(new LinkLeaveEvent(now, vehicle.getId(), inLink.getId()));
		var nextLinkId = vehicle.getDriver().chooseNextLinkId();
		vehicle.getDriver().notifyMoveOverNode(nextLinkId);
		var nextLink = outLinks.get(nextLinkId);

		em.processEvent(new LinkEnterEvent(now, vehicle.getId(), nextLinkId));
		nextLink.pushVehicle(vehicle, SimLink.LinkPosition.QStart, now);
	}

	@Override
	public String toString() {
		return "#nodes=" + activeNodes.size();
	}
}
