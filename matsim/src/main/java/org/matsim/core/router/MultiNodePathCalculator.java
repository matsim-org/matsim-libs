/**
 * 
 */
package org.matsim.core.router;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;

/**
 * Marker interface so that one can program against an interface rather than against a very specific implementation.
 * 
 * @author nagel
 */
public interface MultiNodePathCalculator extends LeastCostPathCalculator {

	void setSearchAllEndNodes(boolean b);

	Path constructPath(Node fromNode, Node node, double startTime);

}
