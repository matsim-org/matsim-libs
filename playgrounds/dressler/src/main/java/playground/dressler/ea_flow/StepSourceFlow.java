package playground.dressler.ea_flow;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;


public class StepSourceFlow implements PathStep {	
	
	/**
	 * Source node
	 */
	private final Node node;
	

	/**
	 * time when the forward flow leaves the source
	 */
	private final int time;
	
	
	/**
	 * reminder if this is a forward step or not
	 */
	private final boolean forward;	
	
	/**
	 * default Constructor setting the arguments when using a Link 
	 * @param edge Link used
	 * @param startTime starting time
	 * @param arrivalTime arrival time
	 * @param forward flag if edge is forward or backward
	 */
	public StepSourceFlow(Node node, int leaveTime, boolean forward){
		if (node == null) {
			throw new IllegalArgumentException("StepSourceFlow, Node node may not be null");
		}
		this.node = node;
		this.time = leaveTime;		
		this.forward = forward;		
	}
		
	
	/**
	 * Method returning a String representation of the PathEdge
	 */
	@Override
	public String toString(){
		String s;
		s = "Sourceoutflow from ";
		s += this.node.getId().toString() + " at "+ this.time;		
		if(!this.forward){
				s += " backwards";
		}
		return s;
	}

	/**
	 * checks whether the link is used in forward direction
	 * @return the forward
	 */
	@Override
	public boolean getForward() {
		return forward;
	}

	/**
	 * returns startNode
	 * @return startNode
	 */
	@Override
	public VirtualNode getStartNode() {
		if (this.forward) {			
		  return new VirtualSource(this.node);
		} else {
		  return new VirtualNormalNode(this.node, this.time);	
		}
	}
	
	@Override
	public VirtualNode getArrivalNode() {
		if (!this.forward) {			
			return new VirtualSource(this.node);
		} else {
			return new VirtualNormalNode(this.node, this.time);	
		}
	}
	
	/**
	 * getter for the time at which an edge is entered
	 * @return the time
	 */
	public int getStartTime() {
		if (this.forward) {
			return 0;
		} else {
			return time;
		}
	}
	
	public int getArrivalTime()
	{
		if (!this.forward) {
			return 0;
		} else {
			return time;
		}
	}
	
	@Override
	public int getCost() {		
		if (forward) 
			return this.time;
		else 
			return 	-this.time;
	}
	
	/**
	 * Checks if two PathEdges are "identical" up to direction
	 * @param other another PathStep 
	 * @return true iff identical up 
	 */
	@Override
	public boolean equalsNoCheckForward(PathStep other) {
		if (!(other instanceof StepSourceFlow)) return false;
		StepSourceFlow o = (StepSourceFlow) other;

		if(this.time == o.time) {
			return (this.node.equals(o.node));
		} else {
			return false;
		}

	}
	
	/**
	 * checks if two PathEdges are identical in all fields
	 * @return true iff identical
	 */
	@Override
	public boolean equals(PathStep other)
	{
		if (!(other instanceof StepSourceFlow)) return false;
		StepSourceFlow o = (StepSourceFlow) other;

		if(this.time == o.time 
				&& this.forward == o.forward) {
			return (this.node.equals(o.node));
		} else {
			return false;
		}

	}
	
	/**
	 * Checks if this is the residual of other
	 * @param other a forward PathEdge 
	 * @return true iff this is the residual of other
	 */
	@Override
	public boolean isResidualVersionOf(PathStep other)
	{
		if (this.forward)
			return false;
		
		if (!other.getForward())
			return false;
		
		if (!(other instanceof StepSourceFlow)) return false;
		StepSourceFlow o = (StepSourceFlow) other;

		if (this.time != o.time) return false;
				
		return (this.node.equals(o.node));				 
	}


	@Override
	public PathStep copyShiftedToStart(int newStart) {
		// forward ... will always have starttime 0
		if (this.forward) {
		  return new StepSourceFlow(this.node, this.time, true);
		}
		
		// residual ... when the source is entered
		return new StepSourceFlow(this.node, newStart, false);
	}

	@Override
	public PathStep copyShiftedToArrival(int newArrival) {
		// forward ... shift arrival time
		if (this.forward) {
		  return new StepSourceFlow(this.node, newArrival, true);
		}
		
		// residual ... will always have arrival 0
		return new StepSourceFlow(this.node, this.time, false);
	}
	
	@Override
	public PathStep copyShifted(int shift) {
		return new StepSourceFlow(this.node, this.time + shift, this.forward);
	}
	
	/*@Override
	public boolean haveSameStart(PathStep other) {		
		if (this.getStartTime() != other.getStartTime()) return false;
		if (!this.node.equals(other.getStartNode())) return false;

		// they seem to leave from the same time/node-pair
		// but one might really leave from a virtual source node
		if (this.forward) {
			if (other instanceof StepSourceFlow && other.getForward()) {
				return true; 
			}
			return false;
		} else {
			if (!(other instanceof StepSourceFlow && other.getForward())) {
				return true;
			}
			return false;
		}
					
	}*/
	
	@Override
	public boolean continuedBy(PathStep other) {
		if (!(other instanceof StepSourceFlow)) return false;
		
		StepSourceFlow o = (StepSourceFlow) other;
		
		return (this.forward == o.forward);			
	}


	@Override
	public String print() {
		String str = "source:"+node.getId().toString()+":"+time+":"+forward;
		return str;
	}
	
}

