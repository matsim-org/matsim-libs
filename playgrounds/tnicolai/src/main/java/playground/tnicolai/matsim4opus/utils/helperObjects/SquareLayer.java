package playground.tnicolai.matsim4opus.utils.helperObjects;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class SquareLayer {

	/** fields regarding square centroid (Layer1) */
	private Id squareCentroidNodeID = null;
	private double squareCentroidAccessibility = 0.;
	
	/** fields regarding square interpolation (Layer2) */
	private ArrayList<Id> squareInterpolationNodeList = null;
	private double squareInterpolationAccessibility = 0.;
	
	/** fields regarding derivation between Layer 1 and 2 (Layer3) */
	private double squareAccessibilityDerivation = 0.;
	
	public void setSquareCentroid(Id nearestNodeID){
		this.squareCentroidNodeID = nearestNodeID;
	}
	
	public void addNode(Node node){
		if(this.squareInterpolationNodeList == null)
			this.squareInterpolationNodeList = new ArrayList<Id>();
		this.squareInterpolationNodeList.add( node.getId() );
	}
	
	public void computeDerivation(final Map<Id, Double> resultMap){
		
		assert(resultMap != null);
		
		// Step 1: get accessibility value for square centroid
		if(this.squareCentroidNodeID == null)
			return;
		this.squareCentroidAccessibility = resultMap.get( this.squareCentroidNodeID );
		
		// Step 2: get accessibility values for remaining nodes within this square (if available)
		if(this.squareInterpolationNodeList != null){
			
			// Step 2.1: sum over all accessibility values
			int numberOfNodes = this.squareInterpolationNodeList.size();
			for(int index = 0; index < numberOfNodes; index++){
				Id nodeId = this.squareInterpolationNodeList.get( index );
				this.squareInterpolationAccessibility =+ resultMap.get( nodeId );
			}
			// Step 2.2: get mean accessibility value
			this.squareInterpolationAccessibility = (this.squareInterpolationAccessibility / numberOfNodes);
			
			// Step 3: determine accessibility derivation
			this.squareAccessibilityDerivation = Math.abs( this.squareInterpolationAccessibility - this.squareCentroidAccessibility );
		}
	}
	
	public double getCentroidAccessibility(){
		return this.squareCentroidAccessibility;
	}
	public double getInterpolationAccessibility(){
		return this.squareInterpolationAccessibility;
	}
	public double getAccessibilityDerivation(){
		return this.squareAccessibilityDerivation;
	}
}