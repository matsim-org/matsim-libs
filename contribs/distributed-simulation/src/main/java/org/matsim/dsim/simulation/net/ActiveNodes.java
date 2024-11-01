package org.matsim.dsim.simulation.net;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Steppable;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Implement the move node logic in the engine. The intersection update touches in and out links
 * of nodes. I find it not fitting to put that behaviour into the node. This way, the node holds
 * the graph information, but the engine, which holds nodes and links performs the mutations on the
 * links.
 */
@Log4j2
@RequiredArgsConstructor
public class ActiveNodes implements Steppable {

    private final Set<SimNode> activeNodes = new HashSet<>();
    private final EventsManager em;
    private final Random rnd = new Random();
    @Setter
    private Consumer<SimLink> activateLink;

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
            var rndNum = rnd.nextDouble() * availableCapacity;
            for (var i = 0; i < node.inLinks().size(); i++) {
                if (exhaustedLinks[i]) continue;

                var inLink = node.inLinks().get(i);

                if (shouldVehicleMove(inLink, node.outLinks(), now)) {
                    selectedCapacity += inLink.getMaxFlowCapacity();
                    if (selectedCapacity >= rndNum) {
                        var vehicle = inLink.popVehicle();
                        move(inLink, vehicle, node.outLinks(), now);
                    }
                } else {
                    exhaustedLinks[i] = true;
                    availableCapacity -= inLink.getMaxFlowCapacity();
                }
            }
        }
        return node.isActiveInNextTimestep(now);
    }

    private boolean shouldVehicleMove(SimLink inLink, Map<Id<Link>, SimLink> outLinks, double now) {

        // first check if the inLink has a vehicle which can leave
        if (!inLink.isOffering())
            return false;

        // check if next link has space, or if the vehicle is stuck.
        // move the vehicle if either the next link has sufficient space, or if the vehicle
        // has reached its stuck time, move it regardless
        SimVehicle vehicle = inLink.peekFirstVehicle();
        Id<Link> nextLinkId = vehicle.getNextRouteElement();
        assert nextLinkId != null : "Vehicle %s has no next route element".formatted(vehicle.getId());

        SimLink nextLink = outLinks.get(nextLinkId);
        assert nextLink != null : "Next link %s not found in outLinks".formatted(nextLinkId);

		return nextLink.isAccepting(SimLink.LinkPosition.QStart, now) || vehicle.isStuck(now);
    }

    private void move(SimLink inLink, SimVehicle vehicle, Map<Id<Link>, SimLink> outLinks, double now) {

        em.processEvent(new LinkLeaveEvent(now, vehicle.getId(), inLink.getId()));

        vehicle.advanceRoute();
        var nextLinkId = vehicle.getCurrentRouteElement();
        var nextLink = outLinks.get(nextLinkId);
        em.processEvent(new LinkEnterEvent(now, vehicle.getId(), nextLinkId));

        nextLink.pushVehicle(vehicle, SimLink.LinkPosition.QStart, now);
        activateLink.accept(nextLink);
    }
}
