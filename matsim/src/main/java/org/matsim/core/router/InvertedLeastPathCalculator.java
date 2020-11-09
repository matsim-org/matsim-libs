package org.matsim.core.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * A least cost path calculator supposed to work on an inverted network (created with {@link org.matsim.core.network.algorithms.NetworkInverter}).
 * The returned path of this calculator will be inverted again so it is valid on the actual non-inverted network.
 * <p>
 * The nodes in {@link #calcLeastCostPath(Node, Node, double, Person, Vehicle)} have to be pseudo nodes from the inverted network, i.e. link ids.
 */
class InvertedLeastPathCalculator implements LeastCostPathCalculator {

    private final Network network;
    private final LeastCostPathCalculator leastCostPathCalculator;

    InvertedLeastPathCalculator(Network network, LeastCostPathCalculator leastCostPathCalculator) {
        this.network = network;
        this.leastCostPathCalculator = leastCostPathCalculator;
    }


    /**
     * Create a new {@link InvertedLeastPathCalculator}.s
     */
    static InvertedLeastPathCalculator create(LeastCostPathCalculatorFactory costFactory, TravelDisutilityFactory travelTimeFactory,
                                              Network network, Network invertedNetwork, LinkToLinkTravelTime l2ltravelTimes) {

        // convert l2ltravelTimes into something that can be used by the inverted network router:
        TravelTimesInvertedNetworkProxy invertedTravelTimes = new TravelTimesInvertedNetworkProxy(network, l2ltravelTimes);
        // (method that takes a getLinkTravelTime( link , ...) with a link from the inverted network, converts it into links on the
        // original network, and looks up the link2link tttime in the l2ltravelTimes data structure)

        TravelDisutility travelCost = travelTimeFactory.createTravelDisutility(invertedTravelTimes);

        return new InvertedLeastPathCalculator(network, costFactory.createPathCalculator(invertedNetwork, travelCost, invertedTravelTimes));
    }

    @Override
    public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person, Vehicle vehicle) {
        Path path = leastCostPathCalculator.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle);
        if (path == null)
            return null;

        return invertPath(path);
    }


    private Path invertPath(Path invPath) {
        int invLinkCount = invPath.links.size();//==> normal node count

        //path search is called only if fromLinkId != toLinkId
        //see: org.matsim.core.router.NetworkRoutingModule.routeLeg()
        //implies: fromInvNode != toInvNode
        if (invLinkCount == 0) {
            throw new RuntimeException(
                    "The path in the inverted network should consist of at least one link.");
        }

        List<Link> links = new ArrayList<>(invLinkCount - 1);
        for (int i = 1; i < invLinkCount; i++) {
            Id<Link> linkId = Id.create(invPath.nodes.get(i).getId(), Link.class);
            links.add(network.getLinks().get(linkId));
        }

        List<Node> nodes = new ArrayList<>(invLinkCount);
//        nodes.add(links.get(0).getFromNode());
        /* use the first link of the inverted path instead of the first node of the just created link list. also works for invLinkCount 1. theresa, jan'17 */
        nodes.add(network.getNodes().get(Id.create(invPath.links.get(0).getId(), Node.class)));
        for (Link l : links) {
            nodes.add(l.getToNode());
        }

        return new Path(nodes, links, invPath.travelTime, invPath.travelCost);
    }

    private static class TravelTimesInvertedNetworkProxy implements TravelTime {

        private final Network network;
        private final LinkToLinkTravelTime linkToLinkTravelTime;

        private TravelTimesInvertedNetworkProxy(Network network, LinkToLinkTravelTime l2ltt) {
            this.linkToLinkTravelTime = l2ltt;
            this.network = network;
        }

        /**
         * In this case the link given as parameter is a link from the inverted network.
         *
         * @see org.matsim.core.router.util.TravelTime#getLinkTravelTime(Link, double, Person,
         * Vehicle)
         */
        @Override
        public double getLinkTravelTime(Link invLink, double time, Person person, Vehicle vehicle) {
            Link fromLink = network.getLinks()
                    .get(Id.create(invLink.getFromNode().getId(), Link.class));
            Link toLink = network.getLinks()
                    .get(Id.create(invLink.getToNode().getId(), Link.class));
            return linkToLinkTravelTime.getLinkToLinkTravelTime(fromLink, toLink, time, person, vehicle);
        }
    }
}
