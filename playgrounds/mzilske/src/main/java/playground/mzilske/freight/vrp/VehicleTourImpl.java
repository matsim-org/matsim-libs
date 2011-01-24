/**
 * 
 */
package playground.mzilske.freight.vrp;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author schroeder
 * 
 */
public class VehicleTourImpl implements VehicleTour  {
	
	private ArrayList<Node> nodes;
	
	private double tourCost;
	
	public VehicleTourImpl(ArrayList<Node> nodes){
		this.nodes = nodes;
	}
	
	
	/* (non-Javadoc)
	 * @see core.basic.VehicleTour#getNodes()
	 */
	public ArrayList<Node> getNodes() {
		return nodes;
	}

	/* (non-Javadoc)
	 * @see core.basic.VehicleTour#getCostValue()
	 */
	public double getCostValue() {
		return tourCost;
	}

	/* (non-Javadoc)
	 * @see core.basic.VehicleTour#setCost(double)
	 */
	public void setCost(double cost) {
		tourCost = cost;
		
	}

	/* (non-Javadoc)
	 * @see core.basic.VehicleTour#getFirst()
	 */
	public Node getFirst(){
		if(nodes.size()>0){
			return nodes.get(0);
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see core.basic.VehicleTour#getLast()
	 */
	public Node getLast(){
		return nodes.get(nodes.size()-1);
	}
	
	/* (non-Javadoc)
	 * @see core.basic.VehicleTour#nodeIsAtEnd(core.basic.Node)
	 */
	public boolean nodeIsAtEnd(Node node){
		if(nodes.get(nodes.size()-2).equals(node)){
			return true;
		}
		else{
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see core.basic.VehicleTour#nodeIsAtBeginning(core.basic.Node)
	 */
	public boolean nodeIsAtBeginning(Node node){
		if(nodes.get(1).equals(node)){
			return true;
		}
		else{
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see core.basic.VehicleTour#nodeIsInterior(core.basic.Node)
	 */
	public boolean nodeIsInterior(Node node) {
		if(nodeIsAtBeginning(node) || nodeIsAtEnd(node)){
			return false;
		}
		else {
			return true;
		}	
	}
	
	/* (non-Javadoc)
	 * @see core.basic.VehicleTour#toString()
	 */
	public String toString(){
		String nodeString = "";
		for(Node n : nodes){
			nodeString += n + " ";
		}
		return nodeString;
	}


	@Override
	public Iterator<Node> tourIterator() {
		// TODO Auto-generated method stub
		return null;
	}
}
