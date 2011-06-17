package vrp.basics;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import vrp.api.Node;


/**
 * 
 * @author stefan schroeder
 *
 */

public class Nodes {

	private Map<Integer,Node> matrixId2NodeMap = new HashMap<Integer, Node>(); 
	
	private Map<Id,Node> nodes = new HashMap<Id,Node>();
	
	public Map<Id, Node> getNodes() {
		return nodes;
	}

	public Map<Integer, Node> getMatrixIdNodeMap() {
		return matrixId2NodeMap;
	}
}
