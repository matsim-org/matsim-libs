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
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;

import java.util.*;

/**
 * A pseudo graph with PseudoRouteStops as nodes  used to
 * calculate the best path and thus link sequence from the
 * first stop to the last stop of a transit route.
 * <p/>
 * The
 *
 * @author polettif
 */
public class PseudoGraphImpl {

	protected static Logger log = Logger.getLogger(PseudoGraphImpl.class);

	/*package*/ static final String SOURCE = "SOURCE";
	/*package*/ static final String DESTINATION = "DESTINATION";
	private final Id<PseudoRouteStopImpl> SOURCE_ID = Id.create(SOURCE, PseudoRouteStopImpl.class);
	private final PseudoRouteStopImpl SOURCE_PSEUDO_STOP = new PseudoRouteStopImpl(SOURCE);
	private final Id<PseudoRouteStopImpl> DESTINATION_ID = Id.create(DESTINATION, PseudoRouteStopImpl.class);
	private final PseudoRouteStopImpl DESTINATION_PSEUDO_STOP = new PseudoRouteStopImpl(DESTINATION);

	private final PublicTransitMappingConfigGroup config;

	private final Map<Id<PseudoRouteStopImpl>, PseudoRouteStopImpl> graph;

	public PseudoGraphImpl(PublicTransitMappingConfigGroup configGroup) {
		this.config = configGroup;
		PseudoRouteStopImpl.setConfig(configGroup);
		this.graph = new HashMap<>();
	}

	public static PseudoRouteStopImpl createPseudoRouteStop(int order, TransitRouteStop routeStop, LinkCandidateImpl linkCandidate) {
		return new PseudoRouteStopImpl(order, routeStop, linkCandidate);
	}

	/**
	 * Add a path between two pseudoStops
	 */
	public void addPath(PseudoRouteStopImpl fromPseudoStop, PseudoRouteStopImpl toPseudoStop, double pathWeight) {
		if(!graph.containsKey(fromPseudoStop.getId())) {
			graph.put(fromPseudoStop.getId(), fromPseudoStop);
		}
		if(!graph.containsKey(toPseudoStop.getId())) {
			graph.put(toPseudoStop.getId(), toPseudoStop);
		}

		double weight = pathWeight + 0.5 * fromPseudoStop.getLinkWeight() + 0.5 * toPseudoStop.getLinkWeight();

		graph.get(fromPseudoStop.getId()).neighbours.put(graph.get(toPseudoStop.getId()), weight);
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

		NavigableSet<PseudoRouteStopImpl> queue = new TreeSet<>();

		queue.add(graph.get(SOURCE_ID));

		PseudoRouteStopImpl currentStop, neighbour;
		while(!queue.isEmpty()) {
			currentStop = queue.pollFirst(); // vertex with shortest distance (first iteration will return source)

			//look at distances to each neighbour
			for(Map.Entry<PseudoRouteStopImpl, Double> n : currentStop.neighbours.entrySet()) {
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
	public LinkedList<PseudoRouteStopImpl> getShortestPseudoPath() {
		if(!graph.containsKey(DESTINATION_ID)) {
			System.err.printf("Graph doesn't contain end PseudoRouteStop \"%s\"\n", DESTINATION_ID);
			return null;
		}

		PseudoRouteStopImpl step = graph.get(DESTINATION_ID);
		LinkedList<PseudoRouteStopImpl> path = new LinkedList<>();

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

	public void addSourceDummyPaths(int order, TransitRouteStop routeStop, Set<LinkCandidateImpl> linkCandidates) {
		for(LinkCandidateImpl lc : linkCandidates) {
			addPath(SOURCE_PSEUDO_STOP, new PseudoRouteStopImpl(order, routeStop, lc), 1.0);
		}
	}

	public void addDestinationDummyPaths(int order, TransitRouteStop routeStop, Set<LinkCandidateImpl> linkCandidates) {
		for(LinkCandidateImpl lc : linkCandidates) {
			addPath(new PseudoRouteStopImpl(order, routeStop, lc), DESTINATION_PSEUDO_STOP, 1.0);
		}
	}
}


