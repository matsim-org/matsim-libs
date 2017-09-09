/**
 * 
 */
package playground.fseccamo.dispatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/** @author Claudio Ruch */
public class MPCAuxiliary {

    /** @param min
     * @param requests */
    /* package */ static int cellMatchingMPCOption1( //
            int min, List<AVRequest> requests, double[] networkBounds, List<RoboTaxi> cars, MPCDispatcher mpcDispatcher, //
            BiConsumer<RoboTaxi, AVRequest> biConsumer) {

        int totalPickupEffectiveAdd = 0;
        for (int count = 0; count < min; ++count) {

            // take a request
            final AVRequest avRequest = requests.get(count);

            // build tree with robotaxis and get robotaxi closest to request
            final QuadTree<RoboTaxi> unassignedVehiclesTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
            for (RoboTaxi robotaxi : cars) {
                unassignedVehiclesTree.put(robotaxi.getDivertableLocation().getCoord().getX(), robotaxi.getDivertableLocation().getCoord().getY(), robotaxi);
            }
            RoboTaxi robotaxi = unassignedVehiclesTree.getClosest( //
                    avRequest.getFromLink().getCoord().getX(), //
                    avRequest.getFromLink().getCoord().getY());

            GlobalAssert.that(!mpcDispatcher.getRoboTaxiSubset(AVStatus.DRIVETOCUSTMER).contains(robotaxi));
            {
                boolean removed = cars.remove(robotaxi);
                GlobalAssert.that(removed);
            }

            // dispatch the car and bookkeeping
            biConsumer.accept(robotaxi, avRequest);
            ++totalPickupEffectiveAdd;
        }

        GlobalAssert.that(totalPickupEffectiveAdd == min);
        return totalPickupEffectiveAdd;

    }

    /* package */ static int cellMatchingMPCOption2(int min, List<AVRequest> requests, List<RoboTaxi> cars, MPCDispatcher mpcDispatcher, //
            BiConsumer<RoboTaxi,AVRequest> biConsumer,  AbstractVehicleDestMatcher vehicleDestMatcher) {
        int totalPickupEffectiveAdd = 0;

        // feed to matcher
        Map<RoboTaxi, AVRequest> matching = vehicleDestMatcher.matchAVRequest(cars, requests);

        // execute the computed matching
        for (Entry<RoboTaxi, AVRequest> entry : matching.entrySet()) {
            if (totalPickupEffectiveAdd < min) {
                biConsumer.accept(entry.getKey(), entry.getValue());
                ++totalPickupEffectiveAdd;
            }
        }
        return totalPickupEffectiveAdd;
    }

    /* package */ static Map<VirtualNode<Link>, Link> computeCenterLinks(VirtualNetwork<Link> virtualNetwork, double[] networkBounds) {
        Map<VirtualNode<Link>, Link> centerLink = new HashMap<>();
        for (VirtualNode<Link> virtualNode : virtualNetwork.getVirtualNodes()) {
            final QuadTree<Link> quadTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
            for (Link link : virtualNode.getLinks())
                quadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);
            Link center = quadTree.getClosest(virtualNode.getCoord().Get(0).number().doubleValue(), //
                    virtualNode.getCoord().Get(1).number().doubleValue());
            centerLink.put(virtualNode, center);
        }
        return centerLink;

    }

}
