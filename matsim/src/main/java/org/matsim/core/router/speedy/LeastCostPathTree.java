package org.matsim.core.router.speedy;

import com.google.common.base.Preconditions;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.turnRestrictions.TurnRestrictionsContext;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.vehicles.Vehicle;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implements a least-cost-path-tree upon a {@link SpeedyGraph} datastructure. Besides using the more efficient Graph datastructure, it also makes use of a custom priority-queue implementation (NodeMinHeap)
 * which operates directly on the least-cost-path-three data for additional performance gains.
 * <p>
 * In some limited tests, this resulted in a speed-up of at least a factor 2.5 compared to MATSim's default LeastCostPathTree.
 * <p>
 * The implementation does not allocate any memory in the {@link #calculate(Link, double, Person, Vehicle)} method. All required memory is pre-allocated in the constructor. This makes the
 * implementation NOT thread-safe.
 *
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 * @author hrewald, nkuehnel / MOIA turn restriction adjustments
 */
public class LeastCostPathTree {

    private final SpeedyGraph graph;
    private final TravelTime tt;
    private final TravelDisutility td;
    private final double[] data; // 3 entries per node: time, cost, distance
    private final int[] comingFrom;
    private final int[] fromLink;
    private final int[] comingFromLink;
    private final SpeedyGraph.LinkIterator outLI;
    private final SpeedyGraph.LinkIterator inLI;
    private final NodeMinHeap pq;

    public LeastCostPathTree(SpeedyGraph graph, TravelTime tt, TravelDisutility td) {
        this.graph = graph;
        this.tt = tt;
        this.td = td;
        this.data = new double[graph.nodeCount * 3];
        this.comingFrom = new int[graph.nodeCount];
        this.fromLink = new int[graph.nodeCount];
        this.comingFromLink = new int[graph.linkCount];
        this.pq = new NodeMinHeap(graph.nodeCount, this::getCost, this::setCost);
        this.outLI = graph.getOutLinkIterator();
        this.inLI = graph.getInLinkIterator();
    }

    /**
     * Please use the link based methods to also account for turn restrictions.
     */
    @Deprecated
    public void calculate(Node startNode, double startTime, Person person, Vehicle vehicle) {
        this.calculateImpl(startNode.getId().index(), startTime, person, vehicle, (node, arrTime, cost, distance, depTime) -> false);
    }

    /**
     * Please use the link based methods to also account for turn restrictions.
     */
    @Deprecated
    public void calculate(Node startNode, double startTime, Person person, Vehicle vehicle, StopCriterion stopCriterion) {
        this.calculateImpl(startNode.getId().index(), startTime, person, vehicle, stopCriterion);
    }

    public void calculate(Link startLink, double startTime, Person person, Vehicle vehicle) {
        this.calculate(startLink, startTime, person, vehicle, (node, arrTime, cost, distance, depTime) -> false);
    }

    public void calculate(Link startLink, double startTime, Person person, Vehicle vehicle, StopCriterion stopCriterion) {
        int startNode = startLink.getToNode().getId().index();
        if(graph.getTurnRestrictions().isPresent()) {
            TurnRestrictionsContext context = graph.getTurnRestrictions().get();
            if(context.replacedLinks.containsKey(startLink.getId())) {
                startNode = context.replacedLinks.get(startLink.getId()).toColoredNode.index();
            }
        }
        calculateImpl(startNode, startTime, person, vehicle, stopCriterion);
    }

    private void calculateImpl(int startNode, double startTime, Person person, Vehicle vehicle, StopCriterion stopCriterion) {
        Arrays.fill(this.data, Double.POSITIVE_INFINITY);
        Arrays.fill(this.comingFrom, -1);
        Arrays.fill(this.fromLink, -1);

        setData(startNode, 0, startTime, 0);

        this.pq.clear();
        this.pq.insert(startNode);

        while (!this.pq.isEmpty()) {
            final int nodeIdx = this.pq.poll();
            double currTime = getTimeRaw(nodeIdx);
            Preconditions.checkState(currTime != Double.POSITIVE_INFINITY, "Undefined Time");
            double currCost = getCost(nodeIdx);
            double currDistance = getDistance(nodeIdx);

            if (stopCriterion.stop( graph.getNode(nodeIdx).getId().index(), currTime, currCost, currDistance, startTime)) {
                break;
            }

            this.outLI.reset(nodeIdx);
            while (this.outLI.next()) {
                int linkIdx = this.outLI.getLinkIndex();
                Link link = this.graph.getLink(linkIdx);
                int toNode = this.outLI.getToNodeIndex();

                double travelTime = this.tt.getLinkTravelTime(link, currTime, person, vehicle);
                double newTime = currTime + travelTime;
                double newCost = currCost + this.td.getLinkTravelDisutility(link, currTime, person, vehicle);

                double oldCost = getCost(toNode);
                if (Double.isFinite(oldCost)) {
                    if (newCost < oldCost) {
                        this.pq.decreaseKey(toNode, newCost);
                        setData(toNode, newCost, newTime, currDistance + link.getLength());
                        this.comingFrom[toNode] = nodeIdx;
                        this.fromLink[toNode] = linkIdx;
                    }
                } else {
                    setData(toNode, newCost, newTime, currDistance + link.getLength());
                    this.pq.insert(toNode);
                    this.comingFrom[toNode] = nodeIdx;
                    this.fromLink[toNode] = linkIdx;
                }
            }
        }

        if (graph.getTurnRestrictions().isPresent()) {
            consolidateColoredNodes();
        }

        Arrays.fill(this.comingFromLink, -1);
        for (int i = 0; i < graph.nodeCount; i++) {
            Node node = graph.getNode(i);
            if(node != null) {
                this.outLI.reset(i);
                while (this.outLI.next()) {
                    int previousLinkIdx = fromLink[i];
                    this.comingFromLink[outLI.getLinkIndex()] = previousLinkIdx;
                }
            }
        }
    }


    public void calculateBackwards(Link arrivalLink, double arrivalTime, Person person, Vehicle vehicle) {
        this.calculateBackwards(arrivalLink, arrivalTime, person, vehicle, (node, arrTime, cost, distance, depTime) -> false);
    }

    public void calculateBackwards(Link arrivalLink, double arrivalTime, Person person, Vehicle vehicle, StopCriterion stopCriterion) {

        Arrays.fill(this.data, Double.POSITIVE_INFINITY);
        Arrays.fill(this.comingFrom, -1);
        Arrays.fill(this.fromLink, -1);

        this.pq.clear();

        int arrivalNode = arrivalLink.getFromNode().getId().index();
        setData(arrivalNode, 0, arrivalTime, 0);
        this.pq.insert(arrivalNode);

        if(graph.getTurnRestrictions().isPresent()) {
            TurnRestrictionsContext turnRestrictionsContext = graph.getTurnRestrictions().get();
            // it might be that the "real" node is not accessible in the colored graph, but only its colored
            // copies. Loop over all in links and add their colored to nodes to the queue. nkuehnel, May 2025
            for (Link inLink : arrivalLink.getFromNode().getInLinks().values()) {
                TurnRestrictionsContext.ColoredLink replacedLink = turnRestrictionsContext.replacedLinks
                        .get(inLink.getId());
                if (replacedLink != null && replacedLink.toColoredNode != null) {
                    int coloredArrivalNode = replacedLink.toColoredNode.index();
                    setData(coloredArrivalNode, 0, arrivalTime, 0);
                    this.pq.insert(coloredArrivalNode);
                }
                List<TurnRestrictionsContext.ColoredLink> coloredLinks = turnRestrictionsContext.coloredLinksPerLinkMap.get(inLink.getId());
                if (coloredLinks != null) {
                    for (TurnRestrictionsContext.ColoredLink coloredLink : coloredLinks) {
                        if (coloredLink.toColoredNode != null) {
                            int coloredArrivalNode = coloredLink.toColoredNode.index();
                            setData(coloredArrivalNode, 0, arrivalTime, 0);
                            this.pq.insert(coloredArrivalNode);
                        }
                    }
                }
            }
        }

        while (!this.pq.isEmpty()) {
            final int nodeIdx = this.pq.poll();
            double currTime = getTimeRaw(nodeIdx);
            Preconditions.checkState(currTime != Double.POSITIVE_INFINITY, "Undefined Time");
            double currCost = getCost(nodeIdx);
            double currDistance = getDistance(nodeIdx);

            if (stopCriterion.stop( graph.getNode(nodeIdx).getId().index(), arrivalTime, currCost, currDistance, currTime)) {
                break;
            }

            this.inLI.reset(nodeIdx);
            while (this.inLI.next()) {
                int linkIdx = this.inLI.getLinkIndex();
                Link link = this.graph.getLink(linkIdx);
                int fromNode = this.inLI.getFromNodeIndex();

                double travelTime = this.tt.getLinkTravelTime(link, currTime, person, vehicle);
                double newTime = currTime - travelTime;
                double newCost = currCost + this.td.getLinkTravelDisutility(link, currTime, person, vehicle);

                double oldCost = getCost(fromNode);
                if (Double.isFinite(oldCost)) {
                    if (newCost < oldCost) {
                        this.pq.decreaseKey(fromNode, newCost);
                        setData(fromNode, newCost, newTime, currDistance + link.getLength());
                        this.comingFrom[fromNode] = nodeIdx;
                        this.fromLink[fromNode] = linkIdx;
                    }
                } else {
                    setData(fromNode, newCost, newTime, currDistance + link.getLength());
                    this.pq.insert(fromNode);
                    this.comingFrom[fromNode] = nodeIdx;
                    this.fromLink[fromNode] = linkIdx;
                }
            }
        }

        if (graph.getTurnRestrictions().isPresent()) {
            consolidateColoredNodes();
        }

        Arrays.fill(this.comingFromLink, -1);
        for (int i = 0; i < graph.nodeCount; i++) {
            Node node = graph.getNode(i);
            if(node != null) {
                this.inLI.reset(i);
                while (this.inLI.next()) {
                    int previousLinkIdx = fromLink[i];
                    this.comingFromLink[inLI.getLinkIndex()] = previousLinkIdx;
                }
            }
        }
    }

    private void consolidateColoredNodes() {
        // update node values with the minimum of their colored copies, if any
        for (int i = 0; i < data.length / 3; i++) {
            Node uncoloredNode = graph.getNode(i);
            if (uncoloredNode != null) {

                // the index points to a node with a different index -> colored copy
                if (uncoloredNode.getId().index() != i) {
                    int uncoloredIndex = uncoloredNode.getId().index();
                    double uncoloredCost = getCost(uncoloredIndex);
                    double coloredCost = getCost(i);

                    if (coloredCost < uncoloredCost) {
                        setData(uncoloredIndex, coloredCost, getTimeRaw(i), getDistance(i));
                        this.comingFrom[uncoloredIndex] = this.comingFrom[i];
                        this.fromLink[uncoloredIndex] = this.fromLink[i];
                    }
                }
            }
        }
    }

    public double getCost(int nodeIndex) {
        return this.data[nodeIndex * 3];
    }

    private double getTimeRaw(int nodeIndex) {
        return this.data[nodeIndex * 3 + 1];
    }

    public OptionalTime getTime(int nodeIndex) {
        double time = getTimeRaw(nodeIndex);
        if (Double.isInfinite(time)) {
            return OptionalTime.undefined();
        }
        return OptionalTime.defined(time);
    }

    public double getDistance(int nodeIndex) {
        return this.data[nodeIndex * 3 + 2];
    }

    private void setCost(int nodeIndex, double cost) {
        this.data[nodeIndex * 3] = cost;
    }

    private void setData(int nodeIndex, double cost, double time, double distance) {
        int index = nodeIndex * 3;
        this.data[index] = cost;
        this.data[index + 1] = time;
        this.data[index + 2] = distance;
    }

    public PathIterator getNodePathIterator(Node node) {
        return new PathIterator(node);
    }

    public LinkPathIterator getLinkPathIterator(Node node) {
        return new LinkPathIterator(node);
    }

    public interface StopCriterion {

        boolean stop(int nodeIndex, double arrivalTime, double travelCost, double distance, double departureTime);
    }

    public static final class TravelTimeStopCriterion implements StopCriterion {

        private final double limit;

        public TravelTimeStopCriterion(double limit) {
            this.limit = limit;
        }

        @Override
        public boolean stop(int nodeIndex, double arrivalTime, double travelCost, double distance, double departureTime) {
            return Math.abs(arrivalTime - departureTime) >= this.limit; // use Math.abs() so it also works in backwards search
        }
    }

    public static final class TravelDistanceStopCriterion implements StopCriterion {

        private final double limit;

        public TravelDistanceStopCriterion(double limit) {
            this.limit = limit;
        }

        @Override
        public boolean stop(int nodeIndex, double arrivalTime, double travelCost, double distance, double departureTime) {
            return distance >= this.limit;
        }
    }

    // by not exposing internal indices to the outside we ensure that only uncolored nodes are returned. nkuehnel Feb'25
    public final class PathIterator implements Iterator<Node> {

        private int current;

        public PathIterator(Node startNode) {
            current = startNode.getId().index();
        }

        @Override
        public Node next() {
            current = comingFrom[current];
            if (current < 0) {
                throw new NoSuchElementException();
            }
            return graph.getNode(current);
        }

        @Override
        public boolean hasNext() {
            return comingFrom[current] >= 0;
        }
    }

    // by not exposing internal indices to the outside we ensure that only uncolored nodes are returned. nkuehnel Feb'25
    public final class LinkPathIterator implements Iterator<Link> {

        private boolean firstStep = true;

        private int current;

        public LinkPathIterator(Node startNode) {
            current = fromLink[startNode.getId().index()];
        }

        @Override
        public Link next() {
            if(firstStep) {
                firstStep = false;
                return graph.getLink(current);
            }
            current = comingFromLink[current];
            if (current < 0) {
                throw new NoSuchElementException();
            }
            return graph.getLink(current);
        }

        @Override
        public boolean hasNext() {
            return current >= 0 && (comingFromLink[current] >= 0 || firstStep);
        }
    }
}
