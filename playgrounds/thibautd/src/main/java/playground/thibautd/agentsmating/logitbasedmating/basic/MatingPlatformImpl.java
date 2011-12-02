/* *********************************************************************** *
 * project: org.matsim.*
 * MatingPlatformImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;

import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceModel;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;
import playground.thibautd.agentsmating.logitbasedmating.framework.MateProposer;
import playground.thibautd.agentsmating.logitbasedmating.framework.Mating;
import playground.thibautd.agentsmating.logitbasedmating.framework.MatingPlatform;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;
import playground.thibautd.agentsmating.logitbasedmating.framework.UnexistingAttributeException;

/**
 * Default implementation of a mating platform.
 * @author thibautd
 */
public class MatingPlatformImpl extends MatingPlatform {
	private static final Log log =
		LogFactory.getLog(MatingPlatformImpl.class);

	private final MateProposer proposer;
	private final ChoiceModel model;
	private final Graph graph = new Graph();

	public MatingPlatformImpl(
			final ChoiceModel model,
			final MateProposer proposer) {
		this.proposer = proposer;
		this.model = model;
	}

	@Override
	public void handleRequest(final TripRequest request) {
		graph.handleRequest( request );
	}

	/**
	 * Determines couples driver/passenger, using a minimum cost maximum matching algorithm.
	 * As many matings of two agents as possible are determined, maximising the
	 * gain in systematic utility when passing from no car poolin to car pooling
	 */
	@Override
	public List<Mating> getMatings() {
		graph.createEdges();
		successiveShortestPaths();

		List<Mating> matings = new ArrayList<Mating>();

		for ( Graph.Edge edge : graph.getEdges() ) {
			if ( edge.isSelectedMating() ) {
				matings.add( new MatingImpl(
							edge.getStart().getRequest(), // driver
							edge.getEnd().getRequest()) ); // passenger
			}
		}

		return matings;
	}

	// /////////////////////////////////////////////////////////////////////////
	// graph manipulating methods
	// /////////////////////////////////////////////////////////////////////////
	private void successiveShortestPaths() {
		log.info( "starting matching algorithm" );
		initializeGraph();

		int remainingFlowToAffect = graph.getCardinalSmallerSet();
		ResidualCapacityComparator comparator = new ResidualCapacityComparator();

		FlowCounter count = new FlowCounter( remainingFlowToAffect );
		while (remainingFlowToAffect > 0) {
			count.log( remainingFlowToAffect );
			List<Graph.Edge> augmentingPath = shortestSTPath();
			if (augmentingPath == null) break; //no further progress possible
			int augmentation =
				Collections.min( augmentingPath , comparator ).getResidualCapacity();
			augmentation = Math.min( augmentation , remainingFlowToAffect );
			remainingFlowToAffect -= augmentation;

			for (Graph.Edge edge : augmentingPath) {
				edge.augment( augmentation );
			}
		}
		count.logFinalState( remainingFlowToAffect );
	}

	private void initializeGraph() {}

	// Bellman-Ford algorithm
	private List<Graph.Edge> shortestSTPath() {
		List<Graph.Edge> edges = graph.getResidualGraphEdges();
		Map<Graph.Vertex, Double> potentials = new HashMap<Graph.Vertex, Double>();
		Map<Graph.Vertex, Graph.Edge> incomingEdges = new HashMap<Graph.Vertex, Graph.Edge>();

		Graph.Vertex source = graph.getSource();
		Graph.Vertex sink = graph.getSink();

		potentials.put( source , 0d );

		for (int i=0; i < edges.size() - 1; i++) {
			for (Graph.Edge edge : edges) {
				Graph.Vertex start = edge.getStart();
				Graph.Vertex end = edge.getEnd();

				Double pStart = potentials.get( start );
				Double pEnd = potentials.get( end );

				double newEndPotential =
					(pStart == null ? Double.POSITIVE_INFINITY : pStart)
					+ edge.getCost();
				double endPotential = (pEnd == null ? Double.POSITIVE_INFINITY : pEnd);

				if (endPotential > newEndPotential) {
					potentials.put( end , newEndPotential );
					incomingEdges.put( end , edge );
				}
			}
		}

		List<Graph.Edge> stPath = new ArrayList<Graph.Edge>();

		Graph.Edge currentEdge = incomingEdges.get( sink );
		if (currentEdge == null) return null; // no s-t path exists
		stPath.add( currentEdge );

		while (currentEdge.getStart() != source) {
			currentEdge = incomingEdges.get( currentEdge.getStart() );
			stPath.add( currentEdge );
		}

		// we do not really care of the order...
		// Collections.reverse( stPath );

		return stPath;
	}

	// /////////////////////////////////////////////////////////////////////////
	// nested classes
	// /////////////////////////////////////////////////////////////////////////
	private class Graph {
		// "tracking" data structures: modified at initialisation of new
		// edges/vertices
		private final List<Edge> edges = new ArrayList<Edge>();
		private final List<Vertex> driverVertices = new ArrayList<Vertex>();
		private final List<Vertex> passengerVertices = new ArrayList<Vertex>();
		private boolean stillModifiable = true;
		private final Vertex source = new Vertex();
		private final Vertex sink = new Vertex();

		// log/debug counters
		private int sxEdges = 0;
		private int xtEdges = 0;
		private int concreteEdges = 0;

		// /////////////////////////////////////////////////////////////////////
		// constructor
		// /////////////////////////////////////////////////////////////////////
		public Graph() {}

		// /////////////////////////////////////////////////////////////////////
		// public methods
		// /////////////////////////////////////////////////////////////////////
		public void handleRequest(final TripRequest request) {
			if (!stillModifiable) throw new IllegalStateException("cannot add requests once the graph is created");
			new Vertex( request );
		}

		public void createEdges() {
			// did we already create edges?
			if (!stillModifiable) return;
			stillModifiable = false;

			log.info( "beginning of graph creation" );
			try {
				for (Vertex passenger : passengerVertices) {
					List<Vertex> possibleDrivers = proposer.proposeMateList(
							passenger,
							driverVertices);
					
					for (Vertex driver : possibleDrivers) {
						new Edge( driver, passenger );
					}
				}
			} catch (MateProposer.UnhandledMatingDirectionException e) {
				try {
					for (Vertex driver : driverVertices) {
						List<Vertex> possiblePassengers = proposer.proposeMateList(
								driver,
								passengerVertices);
						
						for (Vertex passenger : possiblePassengers) {
							new Edge( driver, passenger );
						}
					}
				} catch (MateProposer.UnhandledMatingDirectionException e2) {
					throw new RuntimeException(e2);
				}
			}

			log.info( "graph created" );
			log.info( "graph has "+driverVertices.size()+" driver vertices" );
			log.info( "graph has "+passengerVertices.size()+" passenger vertices" );
			log.info( "graph has "+edges.size()+" edges in total (including s-x and x-t edges)" );
			log.info( "graph has "+concreteEdges+" \"concrete\" edges" );
			log.info( "graph has "+sxEdges+" s-x edges" );
			log.info( "graph has "+xtEdges+" x-t edges" );
		}

		public int getCardinalSmallerSet() {
			return Math.min( driverVertices.size(), passengerVertices.size() );
		}

		public List<Edge> getResidualGraphEdges() {
			List<Edge> out = new ArrayList<Edge>();

			for (Edge edge : edges) {
				if (edge.getResidualCapacity() > 0) {
					out.add( edge );
				}
			}

			return out;
		}

		public List<Edge> getEdges() {
			return edges;
		}

		public Vertex getSource() {
			return source;
		}

		public Vertex getSink() {
			return sink;
		}

		// /////////////////////////////////////////////////////////////////////
		// graph elements
		// /////////////////////////////////////////////////////////////////////
		public class Edge {
			// for "retro" edges in the residual graph
			private final Edge realEdge;
			private static final int capacity = 1;
			private int flow = 0;
			private final double cost;

			private final Vertex start, end;

			private Edge(
					final Vertex driver,
					final Vertex passenger) {
				realEdge = null;
				start = driver;
				end = passenger;

				// update debug counter
				concreteEdges++;

				// cost calculation
				double driverGain = computeGain(
						driver.getRequest(),
						passenger.getRequest());
				double passengerGain = computeGain(
						passenger.getRequest(),
						driver.getRequest());

				cost = - driverGain - passengerGain;
				//System.out.println("cost initilised to "+cost);

				updateTrackingLists();
				//create retro edge
				new Edge( this );
			}

			private Edge(
					final Edge retroEdge) {
				realEdge = retroEdge;
				start = retroEdge.getEnd();
				end = retroEdge.getStart();
				// cost linked to the one of the real edge in the getter
				cost = Double.NaN;

				updateTrackingLists();
			}

			/**
			 * creates edges from source or to sink
			 */
			private Edge(
					final boolean fromSource,
					final Vertex v) {
				if (fromSource) {
					start = source;
					end = v;
					// update the debug counter
					sxEdges++;
				}
				else {
					start = v;
					end = sink;
					// update the debug counter
					xtEdges++;
				}

				cost = 0;
				realEdge = null;
				updateTrackingLists();
				new Edge( this );
			}

			private double computeGain(
					final TripRequest request,
					final TripRequest proposedMate) {
				DecisionMaker decider = request.getDecisionMaker();
				TripRequest cpAlternative =
					model.changePerspective(
							proposedMate,
							request);

				double cpScore = model.getSystematicUtility(
						decider,
						cpAlternative);
				double bestScoreOtherAlternatives = Double.NEGATIVE_INFINITY;

				for (Alternative alt : cpAlternative.getAlternatives()) {
					bestScoreOtherAlternatives = Math.max(
							bestScoreOtherAlternatives,
							model.getSystematicUtility(
								decider,
								alt) );
				}

				return cpScore - bestScoreOtherAlternatives;
			}

			private void updateTrackingLists() {
				edges.add( this );
				start.addOutgoingEdge( this );
			}

			public double getCost() {
				if (realEdge != null) return -realEdge.getCost();
				return cost;
			}

			public int getFlow() {
				if (realEdge != null) return 0;
				return flow;
			}

			public int getResidualCapacity() {
				if (realEdge != null) return realEdge.getFlow();
				return capacity - flow;
			}

			public void augment(final int deltaFlow) {
				if (realEdge != null) {
					realEdge.augment(-deltaFlow);
				}
				else {
					int newFlow = flow + deltaFlow; 

					if (newFlow > capacity) {
						throw new IllegalArgumentException("flow exceeds capacity");
					}
					if (newFlow < 0) {
						throw new IllegalArgumentException("flow is negative");
					}

					flow = newFlow;
				}
			}

			public Vertex getStart() {
				return start;
			}

			public Vertex getEnd() {
				return end;
			}

			public boolean isSelectedMating() {
				return realEdge == null // is a real edge (not residual)
					&& (start != source && end != sink) // is not a "fake" transportation edge
					&& getFlow() > 0; //the flow is positive, ie is selected
			}
		}

		private class Vertex implements TripRequest {
			private final TripRequest request;
			private final List<Edge> outgoingEdges = new ArrayList<Edge>();

			private Vertex() {
				request = null;
			}

			private Vertex(
					final TripRequest request) {
				this.request = request;

				switch (request.getTripType()) {
					case DRIVER:
						driverVertices.add( this );
						// create edge from source
						new Edge( true, this );
						break;
					case PASSENGER:
						passengerVertices.add( this );
						// create edge to sink
						new Edge( false, this );
						break;
					default:
						throw new RuntimeException("unknown enum value");
				}
			}

			public TripRequest getRequest() {
				return request;
			}

			/**
			 * Returns the outgoing edges in the residual graph:
			 * edges of residual capacity 0 are not returned.
			 */
			public List<Edge> getOutgoingEdges() {
				List<Edge> out = new ArrayList<Edge>();

				for (Edge edge : outgoingEdges) {
					if (edge.getResidualCapacity() > 0) {
						out.add( edge );
					}
				}

				return out;
			}

			private void addOutgoingEdge(final Edge edge) {
				outgoingEdges.add( edge );
			}

			// /////////////////////////////////////////////////////////////////
			// delegate methods
			// /////////////////////////////////////////////////////////////////
			@Override
			public double getAttribute(final String attribute)
					throws UnexistingAttributeException {
				return request.getAttribute(attribute);
			}

			@Override
			public String getMode() {
				return request.getMode();
			}

			@Override
			public Type getTripType() {
				return request.getTripType();
			}

			@Override
			public List<Alternative> getAlternatives() {
				return request.getAlternatives();
			}

			@Override
			public Map<String, Object> getAttributes() {
				return request.getAttributes();
			}

			@Override
			public DecisionMaker getDecisionMaker() {
				return request.getDecisionMaker();
			}

			@Override
			public int getIndexInPlan() {
				return request.getIndexInPlan();
			}

			@Override
			public double getDepartureTime() {
				return request.getDepartureTime();
			}

			@Override
			public double getPlanArrivalTime() {
				return request.getPlanArrivalTime();
			}

			@Override
			public Activity getOrigin() {
				return request.getOrigin();
			}

			@Override
			public Activity getDestination() {
				return request.getDestination();
			}
		}
	}

	private static class ResidualCapacityComparator implements Comparator<Graph.Edge> {

		@Override
		public int compare(
				final Graph.Edge o1,
				final Graph.Edge o2) {
			return o1.getResidualCapacity() - o2.getResidualCapacity();
		}
	}

	private static class FlowCounter {
		private final int maxFlow;
		private int next;
		private int step = 1;

		public FlowCounter( final int maxFlow ) {
			this.maxFlow = maxFlow;
			next = maxFlow;
		}

		public void log( final int remainingFlow ) {
			if (remainingFlow <= next) {
				log.info( "successive shortest paths algorithm: "+
						"total demand "+maxFlow+
						", still to affect "+remainingFlow );
				next -= step;
				step *= 2;
			}
		}

		public void logFinalState( final int remainingFlow ) {
			log.info( "successive shortest paths algorithm finished. "+
					remainingFlow+" flow units left unaffected");
		}
	}
}

