package vrp.basics;

import org.matsim.api.core.v01.Id;

import vrp.api.Node;


/**
 * 
 * @author stefan schroeder
 *
 */

public class NodeFactory {
	public Node createNode(Id id, int matrixId){
		Node node = new NodeImpl(id);
		node.setMatrixId(matrixId);
		return node;
	}
}
