package playground.dressler.ea_flow;

import org.matsim.api.core.v01.network.Node;

public interface PathStep {	

	/**
	 * Returns a shifted copy of a PathStep.
	 * The time the flow enters the PathStep is changed to newStart
	 * and the ArrivalTime is changed accordingly. 
	 * @param newStart
	 * @return a new PathStep
	 */
	PathStep copyShiftedToStart(int newStart);
	
	/**
	 * Returns a shifted copy of a PathStep.
	 * The time the flow leaves the PathStep is changed to newArrival
	 * and the StartTime is changed accordingly. 
	 * @param newArrival
	 * @return a new PathStep
	 */
	PathStep copyShiftedToArrival(int newArrival);
	
	/**
	 * Is the the forward or residual version of the step?
	 * @return true iff forward
	 */
	boolean getForward();

	/**
	 * Returns the node that the flow uses first.
	 * This is adjusted according to isForward().
	 * @return The first visited node.
	 */
	Node getStartNode();
	
	/**
	 * Returns the node that the flow uses second.
	 * This is adjusted according to isForward().
	 * @return The second visited node.
	 */
	Node getArrivalNode();

	/**
	 * Returns the time point when the flow enters.
	 * This is adjusted according to isForward().
	 * @return The second visited node.
	 */
	int getStartTime();

	/**
	 * Returns the time point when the flow leaves.
	 * This is adjusted according to isForward().
	 * @return The second visited node.
	 */
	int getArrivalTime();

	/**
	 * Checks if two PathEdges are identical in all fields and of the same type.
	 * @param other another PathStep 
	 * @return true iff identical
	 */
	boolean equals(PathStep other);
	
	/**
	 * Checks if two PathEdges are "identical" up to direction
	 * @param other another PathStep 
	 * @return true iff identical up 
	 */
	boolean equalsNoCheckForward(PathStep other);
	
	/**
	 * Checks if this is the residual of other.
	 * In particular, this must be a residual step and other a forward step for this to return true.
	 * @param other a forward PathEdge 
	 * @return true iff this is the Residual of other
	 */
	boolean isResidualVersionOf(PathStep other);
	
	String toString();
}