package playground.dressler.ea_flow;

import org.matsim.api.core.v01.network.Node;

public class StepHold implements PathStep {
	
	private final Node node;
	/**
	 * time upon which the flow enters the edge
	 */
	private final int startTime;
	
	/**
	 * time upon the flow arrivs at the toNode
	 */
	private final int arrivalTime;
	
	
	/**
	 * reminder if this is a forward edge or not
	 */
	private final boolean forward;
	
	/**
	 * default Constructor setting the arguments when using a Link 
	 * @param edge Link used
	 * @param startTime starting time
	 * @param arrivalTime arrival time (usually < starttime for residual links)
	 * @param forward flag if edge is forward or backward
	 */
	public StepHold(Node node, int startTime, int arrivalTime, boolean forward){
		if (node == null) {
			throw new IllegalArgumentException("StepHold, holdover Node may not be null");
		}
		this.startTime = startTime;
		this.arrivalTime = arrivalTime;
		this.node = node;
		this.forward = forward;		
	}
	
	@Override
	public boolean continuedBy(PathStep other) {
		if (!(other instanceof StepHold)) return false;
		StepHold o = (StepHold) other;
		if (this.forward != o.forward) return false;
		return (this.node.equals(o.node));	
	}

	@Override
	public PathStep copyShiftedToStart(int newStart) {
	    int shift = newStart - this.startTime;
		return new StepHold(this.node, newStart, this.arrivalTime + shift, this.forward); 
	}
	
	@Override
	public PathStep copyShiftedToArrival(int newArrival) {
		int shift = newArrival - this.arrivalTime;
		return new StepHold(this.node, this.startTime + shift, newArrival, this.forward);
	}
	
	@Override
	public PathStep copyShifted(int shift) {
		return new StepHold(this.node, this.startTime + shift, this.arrivalTime + shift, this.forward);
	}
	

	@Override
	public boolean equals(PathStep other) {
		if (!(other instanceof StepHold)) return false;
		StepHold o = (StepHold) other;
		if(this.startTime == o.startTime
				&& this.arrivalTime == o.arrivalTime
				&& this.forward == o.forward)
		{			
			return (this.node.equals(o.node));				 
		}
		return false;
		
	}

	@Override
	public boolean equalsNoCheckForward(PathStep other) {
		if (!(other instanceof StepHold)) return false;
		StepHold o = (StepHold) other;

		if (this.forward == o.forward) {
			if (this.startTime == o.startTime && this.arrivalTime == o.arrivalTime) {
				return (this.node.equals(o.node));
			} else {
				return false;
			}
			
		} else {
			if (this.startTime == o.arrivalTime && this.arrivalTime == o.startTime) {
				return (this.node.equals(o.node));
			} else {
				return false;
			}
		}
	}

	@Override
	public VirtualNode getArrivalNode() {
		return new VirtualNormalNode(node, this.arrivalTime);
		
	}
	@Override
	public int getArrivalTime() {
		return arrivalTime;
	}

	@Override
	public int getCost() {
		return this.arrivalTime - this.startTime;		
	}

	@Override
	public boolean getForward() {
		return forward;
	}

	@Override
	public VirtualNode getStartNode() {
		return new VirtualNormalNode(node, this.startTime);
	}

	@Override
	public int getStartTime() {
		
		return startTime;
	}

	@Override
	public boolean isResidualVersionOf(PathStep other) {
		{
			if (this.forward)
				return false;
			
			if (!other.getForward())
				return false;
			
			if (!(other instanceof StepHold)) return false;
			StepHold o = (StepHold) other;

			if(this.startTime == o.arrivalTime
					&& this.arrivalTime == o.startTime) {
				return (this.node.equals(o.node));
			}
			return false;						 
		}
	}

	@Override
	public String print() {
		String str = "hold:"+node.getId().toString()+":"+startTime+":"+arrivalTime+":"+forward;
		return str;
	}

}
