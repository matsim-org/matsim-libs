package playground.dressler.ea_flow;

// java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

// other imports
import playground.dressler.Intervall.src.Intervalls.*;

// matsim imports
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

// import org.apache.log4j.Logger;
// import org.matsim.utils.identifiers.IdI;
// import org.matsim.basic.v01.Id;

/**
 * Implementation of the Moore-Bellman-Ford Algorithm for a static network! i =
 * 1 .. n for all e = (v,w) if l(w) > l(v) + c(e) then l(w) = l(v) + c(e), p(w) =
 * v.
 * 
 */


public class BellmanFordVertexIntervalls {
	// private final static Logger log =
	// Logger.getLogger(MooreBellmanFord.class);

	/* avoid numerical problems when doing comparisons ... */
	// private double ACCURACY = 0.001;
	/**
	 * The network on which we find routes. We expect the network to change
	 * between runs!
	 */
	final NetworkLayer network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	final TravelCost costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and
	 * time step. This is ignored.
	 */
	final TravelTime timeFunction;
	
	
	
	/**
	 * Datastructure to to represent the flow on a network  
	 */
	private HashMap<Link, EdgeIntervalls> _flow;
	
	
	private HashMap<Node, VertexIntervalls> _labels;

	
	private LinkedList<Link> _pathToRoute;

	private LinkedList<Link> _sources;
	private int _timeHorizon;

	private int _gamma;

	final FakeTravelTimeCost length = new FakeTravelTimeCost();

	
	/**
	 * Default constructor.
	 * 
	 * @param network
	 *            The network on which to route.
	 * @param costFunction
	 *            Determines the link cost defining the cheapest route. Note,
	 *            comparisons are only made with accuraracy 0.001 due to
	 *            numerical problems otherwise.
	 * @param timeFunction
	 *            Determines the travel time on links. This is ignored!
	 */
	public BellmanFordVertexIntervalls(final NetworkLayer network,
			final TravelCost costFunction, final TravelTime timeFunction,
			HashMap<Link, EdgeIntervalls> flow) {

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this._flow = flow;
		_timeHorizon = Integer.MAX_VALUE;

		_pathToRoute = new LinkedList<Link>();
		_gamma = Integer.MAX_VALUE;
		

		/**
		for (Node node : network.getNodes().values()) {
			pred.put(node, null);
			waited.put(node, 0);
		}**/
	}

	/**
	 * Default constructor.
	 * 
	 * @param network
	 *            The network on which to route.
	 * @param costFunction
	 *            Determines the link cost defining the cheapest route. Note,
	 *            comparisons are only made with accuraracy 0.001 due to
	 *            numerical problems otherwise.
	 * @param timeFunction
	 *            Determines the travel time on links. This is ignored!
	 */
	public BellmanFordVertexIntervalls(final NetworkLayer network,
			final TravelCost costFunction, final TravelTime timeFunction,
			HashMap<Link, EdgeIntervalls> flow, int timeHorizon) {

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this._flow = flow;
		this._timeHorizon = timeHorizon;

		_pathToRoute = new LinkedList<Link>();
		_gamma = Integer.MAX_VALUE;
		/**
		for (Node node : network.getNodes().values()) {
			pred.put(node, null);
			waited.put(node, 0);
		}**/

	}
	
	
	private void refreshLabels(){
		
	}
	
	private void augmentFlow(){
		
	}
	
	private void constructRoute(){
		
	}
	private void findGamma(){
		
	} 
	
	
	/**
	 * method for updating the labels of Node to
	 * @param from Node from wich we start
	 * @param to Node to which we want to go 
	 * @param over Link upon which we travel
	 * @param forward indicates, weather we use a forwar or backwards edge
	 * @return
	 */
	private boolean relabel(Node from, Node to, Link over,boolean forward){
		VertexIntervalls labelfrom = _labels.get(from);
		VertexIntervalls labelto = _labels.get(to);
		EdgeIntervalls	flowover = _flow.get(over);
		boolean changed=false;
		int t=0;
		VertexIntervall i;
		do{
			i = labelfrom.getIntervallAt(t);
			t=i.getHighBound();
			if(i.getDist()){
				ArrayList<Intervall> arrive = flowover.propagate(i, (int)over.getCapacity(1.),forward);
				if(!arrive.isEmpty()){
					boolean temp = labelto.setTrue( arrive , over );
					if(temp){
						changed = true;
					}
				}
			}
		}while(!labelfrom.isLast(i));
		return changed;
		
	}
		
		private boolean doCalculations(final Node supersource, final Node toNode,
				final double startTime, final HashMap<Link, EdgeIntervalls> flow) {
		// outprints
		/*
		 * for (Link link : network.getLinks().values()) {
		 * System.out.println("(" + link.getFromNode().getId() + ", " +
		 * link.getToNode().getId() + ") hat Laenge " +
		 * length.getLinkTravelCost(link, 0.)); }
		 */

		// set the start distances Dists of the vertices
		//TODO implement init(fromNode);

		// queue to save nodes we have to scan
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(supersource);

		// v is first vertex in the queue
		// w is the vertex we probably want to insert to the queue or to
		// decrease the distance
		Node v, w;
		// dist is the distance from the source to w over v

		// mainloop
		while (!queue.isEmpty()) {
			// gets the first vertex in the queue
			v = queue.poll();

			// visit neighbors
			// link is outgoing edge of v => forward edge
			for (Link link : v.getOutLinks().values()) {
				w=link.getToNode();
				boolean changed = relabel(v,w,link,true);
				if (changed && !queue.contains(w)) {
					queue.add(w);
				}
			}
			// link is incomming edge of v => backward edge
			for (Link link : v.getInLinks().values()) {
				w=link.getFromNode();
				boolean changed = relabel(v,w,link,false);
				if (changed && !queue.contains(w)) {
					queue.add(w);
				}
				
			}

		}
		//printAll();
		// calculate shortest path with back-tracking and send flow in find
		// ShortestPath
		/**
		if (Dists.getDistance(toNode) == Integer.MAX_VALUE) {
			pathToRoute = null;
			System.out.println("No path found!");
			return false;
		} else if (Dists.getDistance(toNode) > timeHorizon) {
			pathToRoute = null;
			System.out.println("Out of time horizon!");
			System.out.println(timeHorizon + " < " + Dists.getDistance(toNode));
			return false;
		} else {
			pathToRoute = findPath(fromNode, toNode);
			return true;
		}**/
		return false; //TODO remove
	}
	

}