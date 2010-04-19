package playground.dressler.Interval;

import java.util.ArrayList;
/**
 * An interface to capture the usual aspects of the flow on a single edge.
 * Provides methods to get and set the flow and to propagate. 
 */
public interface EdgeFlowI {
	
	/**
	 * increeases the flow into an edge from time t to t+1 by f if capacity is obeyed
	 * @param t raising time
	 * @param f aumount of flow to augment (can be negative)
	 */
	public void augment(final int t, final int gamma);
	
	/**
	 * increeases the flow into an edge from time t to t+1 by f
	 * no checking is done
	 * @param t raising time
	 * @param gamma aumount of flow to augment (can be negative) 
	 */
	public void augmentUnsafe(final int t, final int gamma);
	
	/**
	 * Gives the Flow on the Edge at time t
	 * @param t time
	 * @return flow at t
	 */
	public int getFlowAt(final int t);
	
	/**
	 * Checks the Flow on the Edge at time t
	 * @param t time
	 * @return true iff capacity is obeyed
	 */
	public boolean checkFlowAt(final int t, final int cap);
	
	/**
	 * Gives a list of intervals when the other end of the link can be reached.
	 * This is supposed to work for forward or reverse search.
	 * @param incoming Interval where we can start
	 * @param primal indicates whether we use an original or residual edge
	 * @param reverse indicates whether we want to search forward or backward 
	 * @param TimeHorizon for easy reference
	 * @return plain old Interval
	 */
	public ArrayList<Interval> propagate(final Interval incoming,
			final boolean primal, final boolean reverse, int timehorizon);
	
	/**
	 * unifies adjacent EdgeIntervals, call only when you feel it is safe to do
	 */
	public int cleanup();
	
	/**
	 * Checks whether the given Interval is the last
	 * @param o Interval which it test for 
	 * @return true if getLast.equals(o)
	 */
	public boolean isLast(Interval o);
	
	/**
	 * An upper limit on the last interesting point in time.
	 */
	public int getLastTime();
	
	/**
	 * returns the number of intervals used to represent the flow
	 * @return the number of intervals used
	 */
	public int getMeasure();
	
	public String toString();
}
