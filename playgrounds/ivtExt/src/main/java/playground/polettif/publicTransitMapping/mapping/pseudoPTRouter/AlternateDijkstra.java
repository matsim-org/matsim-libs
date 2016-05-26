package playground.polettif.publicTransitMapping.mapping.pseudoPTRouter;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import playground.polettif.publicTransitMapping.workbench.PseudoRouteExport;

import java.util.*;

public class AlternateDijkstra {

	protected static Logger log = Logger.getLogger(AlternateDijkstra.class);

	public final String SOURCE = "SOURCE";
	public final Id<PseudoRouteStop> SOURCE_ID = Id.create(SOURCE, PseudoRouteStop.class);
	public final String DESTINATION = "DESTINATION";
	public final Id<PseudoRouteStop> DESTINATION_ID = Id.create(DESTINATION, PseudoRouteStop.class);

	private final Map<Id<PseudoRouteStop>, PseudoRouteStop> graph; // mapping of vertex names to Vertex objects, built from a set of Edges

	/**
	 * One edge of the graph (only used by Graph constructor)
	 */
	public static class Edge {
		public final String v1, v2;
		public final int dist;

		public Edge(String v1, String v2, int dist) {
			this.v1 = v1;
			this.v2 = v2;
			this.dist = dist;
		}
	}

	/**
	 * Builds a graph from a set of edges
	 */
	public AlternateDijkstra(PseudoGraph pseudoGraph) {
		List<PseudoRoutePath> edges = pseudoGraph.getEdges();

		graph = new HashMap<>(edges.size());

		//one pass to find all vertices
		for(PseudoRoutePath e : edges) {
			if(!graph.containsKey(e.getFromPseudoStop().getId())) {
				graph.put(e.getFromPseudoStop().getId(), e.getFromPseudoStop());
			}
			if(!graph.containsKey(e.getToPseudoStop().getId())) {
				graph.put(e.getToPseudoStop().getId(), e.getToPseudoStop());
			}
		}

		//another pass to set neighbouring vertices
		for(PseudoRoutePath e : edges) {
			graph.get(e.getFromPseudoStop().getId()).neighbours.put(graph.get(e.getToPseudoStop().getId()), e.getWeight());
			//graph.get(e.v2).neighbours.put(graph.get(e.v1), e.dist); // also do this for an undirected graph
		}
	}

	/**
	 * Runs dijkstra using a specified source vertex
	 */
	public void run() {
		if(!graph.containsKey(SOURCE_ID)) {
			System.err.printf("Graph doesn't contain dummy PseudoRouteStop \"%s\"\n", SOURCE_ID);
			return;
		}

		final PseudoRouteStop source = graph.get(SOURCE_ID);
		NavigableSet<PseudoRouteStop> queue = new TreeSet<>(graph.values());

/*		// set-up vertices
		for(PseudoRouteStop v : graph.values()) {
			v.previous = v == source ? source : null;
			v.distToSource = v == source ? 0 : Integer.MAX_VALUE;
			queue.add(v);
		}
		*/

		PseudoRouteStop currentStop, neighbour;
		while(!queue.isEmpty()) {
			currentStop = queue.pollFirst(); // vertex with shortest distance (first iteration will return source)

			if(currentStop.distToSource == Integer.MAX_VALUE) {
				break; // we can ignore u (and any other remaining vertices) since they are unreachable
			}

			//look at distances to each neighbour
			for(Map.Entry<PseudoRouteStop, Double> n : currentStop.neighbours.entrySet()) {
				neighbour = n.getKey(); //the neighbour in this iteration

				final double alternateDist = currentStop.distToSource + n.getValue();
				if(alternateDist < neighbour.distToSource) { // shorter path to neighbour found
					queue.remove(neighbour);
					neighbour.distToSource = alternateDist;
					neighbour.previous = currentStop;
					queue.add(neighbour);
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
			log.error("Destination cannot be reached");
			for(PseudoRouteStop psr : graph.values()) {
				if(psr.previous == null) {
					log.info(psr);
				}
			}
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


}
