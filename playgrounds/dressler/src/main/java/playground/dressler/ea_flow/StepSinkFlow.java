package playground.dressler.ea_flow;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;


public class StepSinkFlow implements PathStep {	
	
	/**
	 * Sink node
	 */
	private final Node node;
	

	/**
	 * time when the forward flow enters the sink
	 */
	private final int time;
	
	
	/**
	 * reminder if this is a forward step or not
	 */
	private final boolean forward;	
	
	/**
	 * Constructor for setting the arguments when entering a sink 
	 * @param edge Link used
	 * @param startTime starting time
	 * @param arrivalTime arrival time
	 * @param forward flag if edge is forward or backward
	 */
	public StepSinkFlow(Node node, int leaveTime, boolean forward){
		if (node == null) {
			throw new IllegalArgumentException("StepSinkFlow, Node node may not be null");
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
		s = "Sinkflow into ";
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
			return new VirtualNormalNode(this.node, this.time);			
		} else {
			return new VirtualSink(this.node);					  
		}
	}
	
	@Override
	public VirtualNode getArrivalNode() {
		if (this.forward) {			
			return new VirtualSink(this.node);				
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
			return time;
		} else {
			return 0;
		}
	}
	
	public int getArrivalTime()
	{
		if (!this.forward) {
			return time;
		} else {
			return 0; // Maybe time as well, but the sink is not really located at any time.
		}
	}
	
	/**
	 * Checks if two PathEdges are "identical" up to direction
	 * @param other another PathStep 
	 * @return true iff identical up 
	 */
	@Override
	public boolean equalsNoCheckForward(PathStep other) {
		if (!(other instanceof StepSinkFlow)) return false;
		StepSinkFlow o = (StepSinkFlow) other;

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
		if (!(other instanceof StepSinkFlow)) return false;
		StepSinkFlow o = (StepSinkFlow) other;

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
		
		if (!(other instanceof StepSinkFlow)) return false;
		StepSinkFlow o = (StepSinkFlow) other;

		if (this.time != o.time) return false;
				
		return (this.node.equals(o.node));				 
	}


	@Override
	public PathStep copyShiftedToStart(int newStart) {
		// forward, adjust when sink is entered
		if (this.forward) {
		  return new StepSinkFlow(this.node, newStart, true);
		}
		
		// residual ... always starts at 0/TimeHorizon/whenever the sink is
		return new StepSinkFlow(this.node, this.time, false);
	}

	@Override
	public PathStep copyShiftedToArrival(int newArrival) {
		// forward ... always arrives at 0/TimeHorizon/whenever the sink is
		if (this.forward) {
			return new StepSinkFlow(this.node, this.time, true);
		}
		
		// residual ...  update when the reverse flow exits the sink
		return new StepSinkFlow(this.node, newArrival, false);
	}
	
	@Override
	public PathStep copyShifted(int shift) {
		return new StepSinkFlow(this.node, this.time + shift, this.forward);
	}
	
	@Override
	public boolean continuedBy(PathStep other) {
		if (!(other instanceof StepSinkFlow)) return false;
		
		StepSinkFlow o = (StepSinkFlow) other;
		
		return (this.forward == o.forward);			
	}


	@Override
	public String print() {
		String str = "sink:"+node.getId().toString()+":"+time+":"+forward;
		return str;
	}
		
}

