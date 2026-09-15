/* *********************************************************************** *
 * project: org.matsim.*
 * ShortestPathTree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.vehicles.Vehicle;

import java.util.Iterator;

/**
 * Interface for shortest-path tree computations.  Both the Dijkstra-based
 * {@link LeastCostPathTree} and the CH-based {@link CHLeastCostPathTree}
 * implement this interface, enabling DRT and other modules to benefit from
 * CH-accelerated one-to-many path queries.
 *
 * @author Steffen Axer
 */
public interface ShortestPathTree {

    /**
     * Compute shortest paths from the given start link to all reachable nodes.
     */
    void calculate(Link startLink, double startTime, Person person, Vehicle vehicle);

    /**
     * Compute shortest paths from the given start link, stopping when the
     * criterion is satisfied.
     */
    void calculate(Link startLink, double startTime, Person person, Vehicle vehicle,
                   LeastCostPathTree.StopCriterion stopCriterion);

    /**
     * Compute shortest paths to the given arrival link from all reachable nodes
     * (backward tree).
     */
    void calculateBackwards(Link arrivalLink, double arrivalTime, Person person, Vehicle vehicle);

    /**
     * Compute shortest paths to the given arrival link, stopping when the
     * criterion is satisfied.
     */
    void calculateBackwards(Link arrivalLink, double arrivalTime, Person person, Vehicle vehicle,
                            LeastCostPathTree.StopCriterion stopCriterion);

    /**
     * Returns the internal node index used by this tree for the given MATSim node.
     * Use this instead of {@code node.getId().index()} when querying tree results via
     * {@link #getTime(int)}, {@link #getCost(int)}, or {@link #getDistance(int)}.
     */
    int getNodeIndex(Node node);

    /** Cost to reach the given node from the source (forward) or target (backward). */
    double getCost(int nodeIndex);

    /** Arrival/departure time at the given node, or undefined if not reached. */
    OptionalTime getTime(int nodeIndex);

    /** Accumulated distance to the given node. */
    double getDistance(int nodeIndex);

    /**
     * Returns an iterator that walks the parent chain from the given node
     * back to the search root, yielding nodes along the way.
     */
    Iterator<Node> getNodePathIterator(Node node);

    /**
     * Returns an iterator that walks the parent chain from the given node
     * back to the search root, yielding links along the way.
     */
    Iterator<Link> getLinkPathIterator(Node node);
}
