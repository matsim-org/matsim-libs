/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.publicTransitMapping.mapping.pseudoPTRouter;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;

import java.util.*;

/**
 * A pseudo graph with PseudoRouteStops and PseudoRoutePaths
 * used to calculate the best path and thus link sequence
 * from the first stop to the last stop of a transit route.
 * <p/>
 *
 * @author polettif
 */
public class PseudoGraph {

	protected static Logger log = Logger.getLogger(PseudoGraph.class);

	public static final String SOURCE = "SOURCE";
	public static final String DESTINATION = "DESTINATION";
	public final Id<PseudoRouteStop> SOURCE_ID = Id.create(SOURCE, PseudoRouteStop.class);
	public final Id<PseudoRouteStop> DESTINATION_ID = Id.create(DESTINATION, PseudoRouteStop.class);

	public final PseudoRouteStop SOURCE_PSEUDO_STOP = new PseudoRouteStop(SOURCE);
	public final PseudoRouteStop DESTINATION_PSEUDO_STOP = new PseudoRouteStop(DESTINATION);

	private final PublicTransitMappingConfigGroup config;
	private final List<PseudoRoutePath> edges;
	private final Map<Id<PseudoRouteStop>, PseudoRouteStop> graph;

	public PseudoGraph(PublicTransitMappingConfigGroup configGroup) {
		this.config = configGroup;
		PseudoRoutePath.setConfig(configGroup);
		PseudoRouteStop.setConfig(configGroup);
		this.edges = new ArrayList<>();
		this.graph = new HashMap<>();
	}

	public static PseudoRouteStop createPseudoRouteStop(int order, TransitRouteStop routeStop, LinkCandidate linkCandidate) {
		return new PseudoRouteStop(order, routeStop, linkCandidate);
	}

	/**
	 * Add a path between two pseudoStops
	 */
	public void addPath(PseudoRouteStop fromPseudoStop, PseudoRouteStop toPseudoStop, double pathWeight) {
		if(!graph.containsKey(fromPseudoStop.getId())) {
			graph.put(fromPseudoStop.getId(), fromPseudoStop);
		}
		if(!graph.containsKey(toPseudoStop.getId())) {
			graph.put(toPseudoStop.getId(), toPseudoStop);
		}
		graph.get(fromPseudoStop.getId()).neighbours.put(graph.get(toPseudoStop.getId()), pathWeight);
	}

	/**
	 * Runs dijkstra using a specified source vertex
	 */
	public void runDijkstra() {
		if(!graph.containsKey(SOURCE_ID)) {
			System.err.printf("Graph doesn't contain dummy PseudoRouteStop \"%s\"\n", SOURCE_ID);
			return;
		}

		double incr = 0.001;

		NavigableSet<PseudoRouteStop> queue = new TreeSet<>();

		queue.add(graph.get(SOURCE_ID));

		PseudoRouteStop currentStop, neighbour;
		while(!queue.isEmpty()) {
			currentStop = queue.pollFirst(); // vertex with shortest distance (first iteration will return source)

			//look at distances to each neighbour
			for(Map.Entry<PseudoRouteStop, Double> n : currentStop.neighbours.entrySet()) {
				neighbour = n.getKey(); //the neighbour in this iteration

				final double alternateDist = currentStop.distToSource + n.getValue();
				if(alternateDist < neighbour.distToSource) { // shorter path to neighbour found
					queue.remove(neighbour);
					neighbour.distToSource = alternateDist;
					neighbour.previous = currentStop;
					while(!queue.add(neighbour)) {
						neighbour.distToSource -= incr;
					}
				}
			}
		}
	}

	/**
	 * Prints a path from the source to the specified vertex
	 */
	public LinkedList<PseudoRouteStop> getShortestPseudoPath() {
		if(!graph.containsKey(DESTINATION_ID)) {
			System.err.printf("Graph doesn't contain end PseudoRouteStop \"%s\"\n", DESTINATION_ID);
			return null;
		}

		PseudoRouteStop step = graph.get(DESTINATION_ID);
		LinkedList<PseudoRouteStop> path = new LinkedList<>();

		// check if a path exists
		if(step.previous == null) {
			return null;
		}
		path.add(step);
		while(!step.getId().equals(SOURCE_ID)) {
			step = step.previous;
			path.add(step);
		}

		// Put it into the correct order
		Collections.reverse(path);

		// remove dummies
		path.removeFirst();
		path.removeLast();

		return path;
	}

	public void addSourceDummyPaths(int order, TransitRouteStop routeStop, Set<LinkCandidate> linkCandidates) {
		for(LinkCandidate lc : linkCandidates) {
			edges.add(new PseudoRoutePath(SOURCE_PSEUDO_STOP, new PseudoRouteStop(order, routeStop, lc), 1.0, true));
			addPath(SOURCE_PSEUDO_STOP, new PseudoRouteStop(order, routeStop, lc), 1.0);
		}
	}

	public void addDestinationDummyPaths(int order, TransitRouteStop routeStop, Set<LinkCandidate> linkCandidates) {
		for(LinkCandidate lc : linkCandidates) {
			edges.add(new PseudoRoutePath(new PseudoRouteStop(order, routeStop, lc), DESTINATION_PSEUDO_STOP, 1.0, true));
			addPath(new PseudoRouteStop(order, routeStop, lc), DESTINATION_PSEUDO_STOP, 1.0);
		}
	}

	public List<PseudoRoutePath> getEdges() {
		return edges;
	}
}


