package org.matsim.core.router.speedy;

import java.util.Arrays;
import java.util.NoSuchElementException;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.vehicles.Vehicle;

/**
 * Implements a least-cost-path-tree upon a {@link SpeedyGraph} datastructure. Besides using the more efficient Graph datastructure, it also makes use of a custom priority-queue implementation (NodeMinHeap)
 * which operates directly on the least-cost-path-three data for additional performance gains.
 * <p>
 * In some limited tests, this resulted in a speed-up of at least a factor 2.5 compared to MATSim's default LeastCostPathTree.
 * <p>
 * The implementation does not allocate any memory in the {@link #calculate(int, double, Person, Vehicle)} method. All required memory is pre-allocated in the constructor. This makes the
 * implementation NOT thread-safe.
 */
public class LeastCostPathTree {

    private final SpeedyGraph graph;
    private final TravelTime tt;
    private final TravelDisutility td;
    private final double[] data; // 3 entries per node: time, cost, distance
    private final int[] comingFrom;
    private final SpeedyGraph.LinkIterator outLI;
    private final SpeedyGraph.LinkIterator inLI;
    private final NodeMinHeap pq;

    public LeastCostPathTree(SpeedyGraph graph, TravelTime tt, TravelDisutility td) {
        this.graph = graph;
        this.tt = tt;
        this.td = td;
        this.data = new double[graph.nodeCount * 3];
        this.comingFrom = new int[graph.nodeCount];
        this.pq = new NodeMinHeap();
        this.outLI = graph.getOutLinkIterator();
        this.inLI = graph.getInLinkIterator();
    }

    public void calculate(int startNode, double startTime, Person person, Vehicle vehicle) {
        this.calculate(startNode, startTime, person, vehicle, (node, arrTime, cost, distance, depTime) -> false);
    }

    public void calculate(int startNode, double startTime, Person person, Vehicle vehicle, StopCriterion stopCriterion) {
        Arrays.fill(this.data, Double.POSITIVE_INFINITY);
        Arrays.fill(this.comingFrom, -1);

        setData(startNode, 0, startTime, 0);

        this.pq.clear();
        this.pq.insert(startNode);

        while (!pq.isEmpty()) {
            final int nodeIdx = pq.poll();
            OptionalTime currOptionalTime = getTime(nodeIdx);
            double currTime = currOptionalTime.orElseThrow(() -> new RuntimeException("Undefined Time"));
            double currCost = getCost(nodeIdx);
            double currDistance = getDistance(nodeIdx);

            if (stopCriterion.stop(nodeIdx, currTime, currCost, currDistance, startTime)) {
                break;
            }

            outLI.reset(nodeIdx);
            while (outLI.next()) {
                int linkIdx = outLI.getLinkIndex();
                Link link = this.graph.getLink(linkIdx);
                int toNode = outLI.getToNodeIndex();

                double travelTime = this.tt.getLinkTravelTime(link, currTime, person, vehicle);
                double newTime = currTime + travelTime;
                double newCost = currCost + this.td.getLinkTravelDisutility(link, currTime, person, vehicle);

                double oldCost = getCost(toNode);
                if (Double.isFinite(oldCost)) {
                    if (newCost < oldCost) {
                        pq.decreaseKey(toNode, newCost);
                        setData(toNode, newCost, newTime, currDistance + link.getLength());
                        this.comingFrom[toNode] = nodeIdx;
                    }
                } else {
                    setData(toNode, newCost, newTime, currDistance + link.getLength());
                    pq.insert(toNode);
                    this.comingFrom[toNode] = nodeIdx;
                }
            }
        }
    }

    public void calculateBackwards(int arrivalNode, double arrivalTime, Person person, Vehicle vehicle) {
        this.calculate(arrivalNode, arrivalTime, person, vehicle, (node, arrTime, cost, distance, depTime) -> false);
    }

    public void calculateBackwards(int arrivalNode, double arrivalTime, Person person, Vehicle vehicle, StopCriterion stopCriterion) {
        Arrays.fill(this.data, Double.POSITIVE_INFINITY);
        Arrays.fill(this.comingFrom, -1);

        setData(arrivalNode, 0, arrivalTime, 0);

        this.pq.clear();
        this.pq.insert(arrivalNode);

        while (!pq.isEmpty()) {
            final int nodeIdx = pq.poll();
            OptionalTime currOptionalTime = getTime(nodeIdx);
            double currTime = currOptionalTime.orElseThrow(() -> new RuntimeException("Undefined Time"));
            double currCost = getCost(nodeIdx);
            double currDistance = getDistance(nodeIdx);

            if (stopCriterion.stop(nodeIdx, arrivalTime, currCost, currDistance, currTime)) {
                break;
            }

            inLI.reset(nodeIdx);
            while (inLI.next()) {
                int linkIdx = inLI.getLinkIndex();
                Link link = this.graph.getLink(linkIdx);
                int fromNode = inLI.getFromNodeIndex();

                double travelTime = this.tt.getLinkTravelTime(link, currTime, person, vehicle);
                double newTime = currTime - travelTime;
                double newCost = currCost + this.td.getLinkTravelDisutility(link, currTime, person, vehicle);

                double oldCost = getCost(fromNode);
                if (Double.isFinite(oldCost)) {
                    if (newCost < oldCost) {
                        pq.decreaseKey(fromNode, newCost);
                        setData(fromNode, newCost, newTime, currDistance + link.getLength());
                        this.comingFrom[fromNode] = nodeIdx;
                    }
                } else {
                    setData(fromNode, newCost, newTime, currDistance + link.getLength());
                    pq.insert(fromNode);
                    this.comingFrom[fromNode] = nodeIdx;
                }
            }
        }
    }

    public double getCost(int nodeIndex) {
        return this.data[nodeIndex * 3];
    }

    public OptionalTime getTime(int nodeIndex) {
        double time = this.data[nodeIndex * 3 + 1];
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

    public int getComingFrom(int nodeIndex) {
        return this.comingFrom[nodeIndex];
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

    private class NodeMinHeap {

        private final int[] heap;
        private int size = 0;

        NodeMinHeap() {
            this.heap = new int[graph.nodeCount]; // worst case: every node is part of the heap
        }

        void insert(int node) {
            int i = this.size;
            heap[i] = node;
            this.size++;

            int parent = parent(i);

            while (parent != i && getCost(heap[i]) < getCost(heap[parent])) {
                swap(i, parent);
                i = parent;
                parent = parent(i);
            }
        }

        void decreaseKey(int node, double cost) {
            int i;
            for (i = 0; i < size; i++) {
                if (this.heap[i] == node) {
                    break;
                }
            }
            if (getCost(heap[i]) < cost) {
                throw new IllegalArgumentException("existing cost is already smaller than new cost.");
            }

            setCost(node, cost);
            int parent = parent(i);

            // sift up
            while (i > 0 && getCost(heap[parent]) > getCost(heap[i])) {
                swap(i, parent);
                i = parent;
                parent = parent(parent);
            }
        }

        int poll() {
            if (this.size == 0) {
                throw new NoSuchElementException("heap is empty");
            }
            if (this.size == 1) {
                this.size--;
                return this.heap[0];
            }

            int root = this.heap[0];

            // remove the last item, set it as new root
            int lastNode = this.heap[this.size - 1];
            this.size--;
            this.heap[0] = lastNode;

            // sift down
            minHeapify(0);

            return root;
        }

        int peek() {
            if (this.size == 0) {
                throw new NoSuchElementException("heap is empty");
            }
            return this.heap[0];
        }

        int size() {
            return this.size;
        }

        boolean isEmpty() {
            return this.size == 0;
        }

        void clear() {
            this.size = 0;
        }

        private void minHeapify(int i) {
            int left = left(i);
            int right = right(i);
            int smallest = i;

            if (left <= (size - 1) && getCost(heap[left]) < getCost(heap[i])) {
                smallest = left;
            }
            if (right <= (size - 1) && getCost(heap[right]) < getCost(heap[smallest])) {
                smallest = right;
            }
            if (smallest != i) {
                swap(i, smallest);
                minHeapify(smallest);
            }
        }

        private int right(int i) {
            return 2 * i + 2;
        }

        private int left(int i) {
            return 2 * i + 1;
        }

        private int parent(int i) {
            return (i - 1) / 2;
        }

        private void swap(int i, int parent) {
            int tmp = this.heap[parent];
            this.heap[parent] = this.heap[i];
            this.heap[i] = tmp;
        }
    }

}
