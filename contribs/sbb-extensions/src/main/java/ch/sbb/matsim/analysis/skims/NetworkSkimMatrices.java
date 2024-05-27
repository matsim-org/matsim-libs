/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.analysis.skims;

import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.speedy.LeastCostPathTree;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * Calculates zone-to-zone matrices containing a number of performance indicators related to modes routed on a network.
 * <p>
 * Inspired by https://github.com/moeckel/silo/blob/siloMatsim/silo/src/main/java/edu/umd/ncsg/transportModel/Zone2ZoneTravelTimeListener.java.
 * <p>
 * Idea of the algorithm: - given n points per zone - find the nearest link and thereof the to-node for each point - this results in n nodes per zone (where some nodes can appear multiple times, this
 * is wanted as it acts as a weight/probability) - for each zone-to-zone combination, calculate the travel times for each node to node combination. - this results in n x n travel times per
 * zone-to-zone combination. - average the n x n travel times and store this value as the zone-to-zone travel time.
 *
 * @author mrieser / SBB
 */
public final class NetworkSkimMatrices {

    private NetworkSkimMatrices() {
    }

    public static <T> NetworkIndicators<T> calculateSkimMatrices(Network xy2lNetwork, Network routingNetwork, Map<T, Coord[]> coordsPerZone, double departureTime, TravelTime travelTime,
            TravelDisutility travelDisutility, int numberOfThreads) {
        SpeedyGraph routingGraph = SpeedyGraphBuilder.build(routingNetwork);
        Map<T, Node[]> nodesPerZone = new HashMap<>();
        for (Map.Entry<T, Coord[]> e : coordsPerZone.entrySet()) {
            T zoneId = e.getKey();
            Coord[] coords = e.getValue();
            Node[] nodes = new Node[coords.length];
            nodesPerZone.put(zoneId, nodes);
            for (int i = 0; i < coords.length; i++) {
                Coord coord = coords[i];
                Node node = NetworkUtils.getNearestLink(xy2lNetwork, coord).getToNode();
                nodes[i] = routingNetwork.getNodes().get(node.getId());
            }
        }

        // prepare calculation
        NetworkIndicators<T> networkIndicators = new NetworkIndicators<>(coordsPerZone.keySet());

        int numberOfPointsPerZone = coordsPerZone.values().iterator().next().length;
        float avgFactor = (float) (1.0 / numberOfPointsPerZone / numberOfPointsPerZone);

        // do calculation
        ConcurrentLinkedQueue<T> originZones = new ConcurrentLinkedQueue<>(coordsPerZone.keySet());

        Counter counter = new Counter("CAR-TravelTimeMatrix-" + Time.writeTime(departureTime) + " zone ", " / " + coordsPerZone.size());
        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            RowWorker<T> worker = new RowWorker<>(originZones, coordsPerZone.keySet(), routingGraph, nodesPerZone, networkIndicators, departureTime, travelTime, travelDisutility, counter);
            threads[i] = new Thread(worker, "CAR-TravelTimeMatrix-" + Time.writeTime(departureTime) + "-" + i);
            threads[i].start();
        }

        // wait until all threads have finished
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        networkIndicators.travelTimeMatrix.multiply(avgFactor);
        networkIndicators.distanceMatrix.multiply(avgFactor);

        return networkIndicators;
    }

    private static class RowWorker<T> implements Runnable {

        private final static Vehicle VEHICLE = VehicleUtils.getFactory().createVehicle(Id.create("theVehicle", Vehicle.class), VehicleUtils.createDefaultVehicleType());
        private final static Person PERSON = PopulationUtils.getFactory().createPerson(Id.create("thePerson", Person.class));
        private final ConcurrentLinkedQueue<T> originZones;
        private final Set<T> destinationZones;
        private final SpeedyGraph graph;
        private final Map<T, Node[]> nodesPerZone;
        private final NetworkIndicators<T> networkIndicators;
        private final TravelTime travelTime;
        private final TravelDisutility travelDisutility;
        private final double departureTime;
        private final Counter counter;

        RowWorker(ConcurrentLinkedQueue<T> originZones, Set<T> destinationZones, SpeedyGraph graph, Map<T, Node[]> nodesPerZone, NetworkIndicators<T> networkIndicators, double departureTime,
                  TravelTime travelTime, TravelDisutility travelDisutility, Counter counter) {
            this.originZones = originZones;
            this.destinationZones = destinationZones;
            this.graph = graph;
            this.nodesPerZone = nodesPerZone;
            this.networkIndicators = networkIndicators;
            this.departureTime = departureTime;
            this.travelTime = travelTime;
            this.travelDisutility = travelDisutility;
            this.counter = counter;
        }

        @Override
        public void run() {
            LeastCostPathTree lcpTree = new LeastCostPathTree(this.graph, this.travelTime, this.travelDisutility);
            while (true) {
                T fromZoneId = this.originZones.poll();
                if (fromZoneId == null) {
                    return;
                }

                this.counter.incCounter();
                Node[] fromNodes = this.nodesPerZone.get(fromZoneId);
                if (fromNodes != null) {
                    for (Node fromNode : fromNodes) {
                        lcpTree.calculate(fromNode.getId().index(), this.departureTime, PERSON, VEHICLE);

                        for (T toZoneId : this.destinationZones) {
                            Node[] toNodes = this.nodesPerZone.get(toZoneId);
                            if (toNodes != null) {
                                for (Node toNode : toNodes) {
                                    int nodeIndex = toNode.getId().index();
                                    OptionalTime currOptionalTime = lcpTree.getTime(nodeIndex);
                                    double currTime = currOptionalTime.orElseThrow(() -> new RuntimeException("Undefined Time"));
                                    double tt = currTime - this.departureTime;
                                    double dist = lcpTree.getDistance(nodeIndex);
                                    this.networkIndicators.travelTimeMatrix.add(fromZoneId, toZoneId, (float) tt);
                                    this.networkIndicators.distanceMatrix.add(fromZoneId, toZoneId, (float) dist);
                                }
                            } else {
                                // this might happen if a zone has no geometry, for whatever reason...
                                this.networkIndicators.travelTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                                this.networkIndicators.distanceMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                            }
                        }
                    }
                } else {
                    // this might happen if a zone has no geometry, for whatever reason...
                    for (T toZoneId : this.destinationZones) {
                        this.networkIndicators.travelTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                        this.networkIndicators.distanceMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    }
                }
            }
        }
    }

    static class NetworkIndicators<T> {

        final FloatMatrix<T> travelTimeMatrix;
        final FloatMatrix<T> distanceMatrix;

        NetworkIndicators(Set<T> zones) {
            this.travelTimeMatrix = new FloatMatrix<>(zones, 0);
            this.distanceMatrix = new FloatMatrix<>(zones, 0);
        }
    }

}
