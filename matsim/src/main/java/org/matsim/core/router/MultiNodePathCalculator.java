/**
 * 
 */
package org.matsim.core.router;

import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.vehicles.Vehicle;

/**
 * Marker interface so that one can program against an interface rather than against a very specific implementation.
 * 
 * @author nagel
 */
public interface MultiNodePathCalculator extends LeastCostPathCalculator {

	void setSearchAllEndNodes(boolean b);

	/**
	 * This method should be called after {@link LeastCostPathCalculator#calcLeastCostPath(Node, Node, double, Person, Vehicle)} has been called, with so-called {@link
	 * ImaginaryNode} as input(s).  It is expected that the algorithm memorizes the Dijkstra tree.  This means that queries for the same starting node but different end nodes
	 * should be fast.  This also implies that fromNode canNOT be freely selected; rather, it will be an output of the algo.  If also the starting point is multiple nodes,
	 * then the path will start at the most convenient of those to reach the given end point.
	 * <br/>
	 * I was thinking of removing the fromNode argument.  However, there are "backwards Dijkstra" cases, and it may thus be easier to just accept the current interface, and
	 * possibly throw runtime exceptions.
	 */
	Path constructPath(Node fromNode, Node node, double startTime);

}
