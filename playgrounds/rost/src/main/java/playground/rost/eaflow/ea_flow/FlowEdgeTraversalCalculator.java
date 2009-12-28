package playground.rost.eaflow.ea_flow;

/**
 * @author Matthias Rost (rost@mi.fu-berlin.de)
 *
 */
/**
 * @author Matthias Rost (rost@mi.fu-berlin.de)
 *
 */
/**
 * @author Matthias Rost (rost@mi.fu-berlin.de)
 *
 */
public interface FlowEdgeTraversalCalculator {
	
	/**
	 * 
	 * @param currentFlow
	 * @return how many units can be send over some edge according to the currentFlow
	 */
	public int getRemainingForwardCapacityWithThisTravelTime(int currentFlow);
	
	/**
	 *
	 * @param currentFlow
	 * @return the amount of flow which can be negated according to the currentFlow
	 */
	public int getRemainingBackwardCapacityWithThisTravelTime(int currentFlow);
	
	/**
	 * 
	 * @return the minimal possible travel time over this edge
	 */
	public int getMinimalTravelTime();
	
	
	/**
	 * @return the maximal possible travel time over this edge
	 */
	public int getMaximalTravelTime();
	
	
	/**
	 * @param currentFlow
	 * @return the travel time for additional flow according to the current flow on this edge
	 */
	public Integer getTravelTimeForAdditionalFlow(int currentFlow);

	/**
	 * @param currentFlow
	 * @return the travel time, that the upper bound of the flow induces 
	 */
	public Integer getTravelTimeForFlow(int currentFlow);
	
}
