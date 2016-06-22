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


package playground.polettif.publicTransitMapping.mapping.pseudoRouter;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

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
public class PseudoGraphImpl implements PseudoGraph {

	protected static Logger log = Logger.getLogger(PseudoGraphImpl.class);

	/*package*/ static final String SOURCE = "SOURCE";
	/*package*/ static final String DESTINATION = "DESTINATION";
	private final Id<PseudoRouteStop> SOURCE_ID = Id.create(SOURCE, PseudoRouteStop.class);
	private final PseudoRouteStop SOURCE_PSEUDO_STOP = new PseudoRouteStopImpl(SOURCE);
	private final Id<PseudoRouteStop> DESTINATION_ID = Id.create(DESTINATION, PseudoRouteStop.class);
	private final PseudoRouteStop DESTINATION_PSEUDO_STOP = new PseudoRouteStopImpl(DESTINATION);

	private final Map<Id<PseudoRouteStop>, PseudoRouteStop> graph;

	public PseudoGraphImpl() {
		this.graph = new HashMap<>();
	}

	/**
	 * Runs dijkstra using a specified source vertex
	 * todo make private
	 */
	public void runDijkstra() {
		if(!graph.containsKey(SOURCE_ID)) {
			System.err.printf("Graph doesn't contain dummy PseudoRouteStop \"%s\"\n", SOURCE_ID);
			return;
		}

		NavigableSet<PseudoRouteStop> queue = new TreeSet<>();

		queue.add(graph.get(SOURCE_ID));

		PseudoRouteStop currentStop, neighbour;
		while(!queue.isEmpty()) {
			currentStop = queue.pollFirst(); // vertex with shortest distance (first iteration will return source)

			//look at distances to each neighbour
			for(Map.Entry<PseudoRouteStop, Double> n : currentStop.getNeighbours().entrySet()) {
				neighbour = n.getKey(); //the neighbour in this iteration

				final double alternateDist = currentStop.getTravelCostToSource() + n.getValue();
				if(alternateDist < neighbour.getTravelCostToSource()) { // shorter path to neighbour found
					queue.remove(neighbour);
					neighbour.setTravelCostToSource(alternateDist);
					neighbour.setClosestPrecedingRouteSTop(currentStop);
					queue.add(neighbour);
				}
			}
		}
	}

	/**
	 * returns a path from the source to the destionation
	 */
	public List<PseudoRouteStop> getShortestPseudoPath() {
		if(!graph.containsKey(DESTINATION_ID)) {
			System.err.printf("Graph doesn't contain end PseudoRouteStop \"%s\"\n", DESTINATION_ID);
			return null;
		}

		PseudoRouteStop step = graph.get(DESTINATION_ID);
		LinkedList<PseudoRouteStop> path = new LinkedList<>();

		// check if a path exists
		if(step.getClosestPrecedingRouteStop() == null) {
			return null;
		}
		path.add(step);
		while(!step.getId().equals(SOURCE_ID)) {
			step = step.getClosestPrecedingRouteStop();
			path.add(step);
		}

		// Put it into the correct order
		Collections.reverse(path);
		// remove dummies
		path.removeFirst();
		path.removeLast();

		return path;
	}

	@Override
	public void addDummyEdges(List<TransitRouteStop> transitRouteStops, Collection<LinkCandidate> firstStopLinkCandidates, Collection<LinkCandidate> lastStopLinkCandidates) {
		for(LinkCandidate lc : firstStopLinkCandidates) {
			addEdge(SOURCE_PSEUDO_STOP, new PseudoRouteStopImpl(0, transitRouteStops.get(0), lc), 1.0);
		}
		int last = transitRouteStops.size() - 1;
		for(LinkCandidate lc : lastStopLinkCandidates) {
			addEdge(new PseudoRouteStopImpl(last, transitRouteStops.get(last), lc), DESTINATION_PSEUDO_STOP, 1.0);
		}
	}

	@Override
	public List<PseudoRouteStop> getLeastCostStopSequence() {
		runDijkstra();
		return getShortestPseudoPath();
	}

	@Override
	public void addEdge(int orderOfFromStop, TransitRouteStop fromTransitRouteStop, LinkCandidate fromLinkCandidate, TransitRouteStop toTransitRouteStop, LinkCandidate toLinkCandidate, double pathTravelCost) {
		PseudoRouteStop fromPseudoStop = new PseudoRouteStopImpl(orderOfFromStop, fromTransitRouteStop, fromLinkCandidate);
		PseudoRouteStop toPseudoStop = new PseudoRouteStopImpl(orderOfFromStop+1, toTransitRouteStop, toLinkCandidate);
		addEdge(fromPseudoStop, toPseudoStop, pathTravelCost);
	}

	private void addEdge(PseudoRouteStop from, PseudoRouteStop to, double pathTravelCost) {
		if(!graph.containsKey(from.getId())) {
			graph.put(from.getId(), from);
		}
		if(!graph.containsKey(to.getId())) {
			graph.put(to.getId(), to);
		}
		double weight = pathTravelCost + 0.5 * from.getLinkCandidate().getLinkTravelCost() + 0.5 * to.getLinkCandidate().getLinkTravelCost();
		graph.get(from.getId()).getNeighbours().put(graph.get(to.getId()), weight);
	}


	/**
	 * DEPRECATED
	 */
	@Deprecated
	public void addSourceDummyPaths(int order, TransitRouteStop routeStop, Set<LinkCandidateImpl> linkCandidates) {
		for(LinkCandidateImpl lc : linkCandidates) {
			addPath((PseudoRouteStopImpl) SOURCE_PSEUDO_STOP, new PseudoRouteStopImpl(order, routeStop, (LinkCandidate) lc), 1.0);
		}
	}

	@Deprecated
	public void addDestinationDummyPaths(int order, TransitRouteStop routeStop, Set<LinkCandidateImpl> linkCandidates) {
		for(LinkCandidateImpl lc : linkCandidates) {
			addPath(new PseudoRouteStopImpl(order, routeStop, (LinkCandidate) lc), (PseudoRouteStopImpl) DESTINATION_PSEUDO_STOP, 1.0);
		}
	}

	/**
	 * Add a path between two pseudoStops
	 */
	@Deprecated
	public void addPath(PseudoRouteStopImpl fromPseudoStopImpl, PseudoRouteStopImpl toPseudoStopImpl, double pathWeight) {
		PseudoRouteStop fromPseudoStop = (PseudoRouteStop) fromPseudoStopImpl;
		PseudoRouteStop toPseudoStop = (PseudoRouteStop) toPseudoStopImpl;

		if(!graph.containsKey(fromPseudoStop.getId())) {
			graph.put(fromPseudoStop.getId(), fromPseudoStop);
		}
		if(!graph.containsKey(toPseudoStop.getId())) {
			graph.put(toPseudoStop.getId(), toPseudoStop);
		}

		double weight = pathWeight + 0.5 * fromPseudoStop.getLinkTravelCost() + 0.5 * toPseudoStop.getLinkTravelCost();
		graph.get(fromPseudoStop.getId()).getNeighbours().put(graph.get(toPseudoStop.getId()), weight);
	}

	@Deprecated
	public static PseudoRouteStopImpl createPseudoRouteStop(int order, TransitRouteStop routeStop, LinkCandidateImpl linkCandidate) {
		return new PseudoRouteStopImpl(order, routeStop, linkCandidate);
	}
}


