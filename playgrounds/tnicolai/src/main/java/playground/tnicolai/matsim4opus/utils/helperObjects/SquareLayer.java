package playground.tnicolai.matsim4opus.utils.helperObjects;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.Id;

public class SquareLayer {
	
	private static Map<Id, AccessibilityNode> accessibilityNodeMap;

	/** fields regarding square centroid (Layer1) */
	private Coord squareCentroid = null;
	private Node squareCentroidNode = null;
	private double squareCentroidAccessibilityValue = 0.;
	
	/** fields regarding square interpolation (Layer2) */
	private ArrayList<Node> squareInterpolationNodeList = null;
	private double squareInterpolationAccessibilityValue = 0.;
	
	/** fields regarding derivation between Layer 1 and 2 (Layer3) */
	private double squareDerivation = 0.;
	private double targetTolerance = 0.;
	
	public void setSquareCentroid(Coord centroidCoord, Node nearestNode){
		this.squareCentroid = centroidCoord;
		this.squareCentroidNode = nearestNode;
	}
	
	public void addNode(Node node){
		if(this.squareInterpolationNodeList == null)
			this.squareInterpolationNodeList = new ArrayList<Node>();
		this.squareInterpolationNodeList.add( node );
	}
	
	class AccessibilityNode{
		
		Node node = null;
		double accessibilityValue = 0.;
		
	}
}