/* *********************************************************************** *
 * project: org.matsim.*												   *
 * EdgeIntervals.java													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package playground.dressler.Interval;

//java imports
import java.util.ArrayList;

//playground imports
import playground.dressler.ea_flow.FlowCalculationSettings;

/**
 * class representing the flow of an edge in a Time Expanded Network
 * @author Manuel Schneider
 *
 */
public class EdgeIntervals {

//**********************************FIELDS*****************************************//
	
	/**
	 * internal binary search tree holding distinkt Intervals
	 */
	private AVLTree _tree;
	
	/**
	 * reference to the last Interval
	 */
	private EdgeInterval _last; 

	/**
	 * traveltime for easy access 
	 */
	public final int _traveltime;
	
	/**
	 * debug flag
	 */
	private static int _debug =0;
	
//********************************METHODS******************************************//
		
//------------------------------CONSTRUCTORS---------------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeInterval [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public EdgeIntervals(final int traveltime){
		EdgeInterval interval = new EdgeInterval(0,Integer.MAX_VALUE);
		_tree = new AVLTree();
		_tree.insert(interval);
		_last = interval;
		this._traveltime=traveltime;
	}

//-----------------------------------SPLITTING-------------------------------------//	
	
	/**
	 * Finds the EgdeInterval containing t and splits this at t 
	 * giving it the same flow as the flow as the original 
	 * it inserts the new EdgeInterval after the original
	 * @param t time point to split at
	 * @return the new EdgeInterval for further modification
 	 */
	public EdgeInterval splitAt(final int t){
		boolean found = false;
		EdgeInterval j = null;
		EdgeInterval i = getIntervalAt(t);
		if (i != null){
			found = true;
			//update last
			if(i == _last){
				j = i.splitAt(t);
				_last = j;
			}else {
				j = i.splitAt(t);
			}
		}	
		if (found){
			_tree.insert(j);
			return j;
		}
		else throw new IllegalArgumentException("there is no Interval that " +
				"can be split at "+t);
	}

//--------------------------------------FLOW---------------------------------------//	
	
	/**
	 * Gives the Flow on the Edge at time t
	 * @param t time
	 * @return flow at t
	 */
	public int getFlowAt(final int t){
		return getIntervalAt(t).getFlow();
	}
	
//-------------------------------------GETTER--------------------------------------//

	/**
	 * Finds the EdgeInterval containing t in the collection
	 * @param t time
	 * @return  EdgeInterval  containing t
	 */
	public EdgeInterval getIntervalAt(final int t){
		if(t<0){
			throw new IllegalArgumentException("negative time: "+ t);
		}
		EdgeInterval i = (EdgeInterval) _tree.contains(t);
		if(i==null)throw new IllegalArgumentException("there is no Interval " +
				"containing "+t);
		return i;
	}

	/**
	 * Gives a String representation of all stored EdgeIntervals
	 * @return String representation
	 */
	@Override
	public String toString(){
		String l,r;
		switch (Interval.getMode()){
		case 1: 
			l="[";
			r="]";
			break;
		case 2:
			l="(";
			r="]";
			break;
		case 3:
			l="[";
			r=")";
			break;
		case 4:
			l="(";
			r=")";
			break;
		default: 
			l="|";
			r="|";
			break;
		}
		StringBuilder str = new StringBuilder();
		for(_tree.reset();!_tree.isAtEnd();_tree.increment()){
			EdgeInterval i= (EdgeInterval) _tree._curr.obj;
			str.append(l+i.getLowBound()+";"+i.getHighBound()+r+" " +
					"f: "+i.getFlow()+" \n");
		}
		return str.toString();	
	}
	
	/**
	 * Returns the number of stored intervals
	 * @return the number of stored intervals
	 */
	public int getSize() {		
		return this._tree._size;
	}
	
	
	/**
	 * gives the last Stored EdgeInterval
	 * @return EdgeInterval with maximal lowbound
	 */
	public EdgeInterval getLast(){
		return _last;
	}
	
	
	/**
	 * checks weather last is referenced right
	 * @return true iff everything is OK
	 */
	public boolean checkLast(){
		return _last==_tree._getLast().obj;
	}
	
	/**
	 * Checks weather the given EdgeInterval is the last
	 * @param o EgeInterval which it test for 
	 * @return true if getLast.equals(o)
	 */
	public boolean isLast(final EdgeInterval o){
		return (_last.equals(o));
	}
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(final int debug){
		EdgeIntervals._debug=debug;
	}
	
	/**
	 * gives the next EdgeInterval with respect of the order contained 
	 * @param o schould be contained
	 * @return next EdgeInterval iff o is not last and contained. if o is last,
	 *  null is returned.
	 */
	public EdgeInterval getNext(final EdgeInterval o){
		_tree.goToNodeAt(o.getLowBound());
		EdgeInterval j = (EdgeInterval) _tree._curr.obj;
		if(j.equals(o)){
			_tree.increment();
			if(!_tree.isAtEnd()){
				EdgeInterval i = (EdgeInterval) _tree._curr.obj;
				_tree.reset();
				return i;
			}else{ 	
				return null;
			}
		}
		else throw new IllegalArgumentException("Interval was not contained");
	}

	
	
	/**
	 * finds the next EdgeInterval that has flow less than u after time t
	 * so that additional flow could be sent during the Interval
	 * @param earliestStartTimeAtFromNode time >=0 !!!
	 * @param capacity capacity
	 * @return EdgeInterval[a,b] with f<u  and a>=t
	 */
	public EdgeInterval minPossibleForwards(final int earliestStartTimeAtFromNode,
			final int capacity){
		if (earliestStartTimeAtFromNode<0){
			throw new IllegalArgumentException("time shold not be negative");
		}
		if (capacity<=0){
			throw new IllegalArgumentException("capacity shold be positive");
		}
		boolean wasAtEnd = false;
		//search for the next interval, at which flow can be send!
		for(_tree.goToNodeAt(earliestStartTimeAtFromNode); !wasAtEnd; _tree.increment()){
			if(_debug>0){
				System.out.println("f: " +
						((EdgeInterval)_tree._curr.obj).getFlow()+" on: "+
						((EdgeInterval)_tree._curr.obj));
			}
			EdgeInterval currentInterval = (EdgeInterval)_tree._curr.obj;
			if(currentInterval.getFlow()<capacity){
				if(_debug>0){
					System.out.println("capacity left: " +
							(capacity-currentInterval.getFlow()));
				}
				int earliestPossibleStart = Math.max(earliestStartTimeAtFromNode,
						currentInterval.getLowBound());
				return new EdgeInterval(earliestPossibleStart, 
						currentInterval.getHighBound(), currentInterval.getFlow());
			}
			//to iterate over the intervals
			if(_tree.isAtEnd())
			{
				wasAtEnd = true;
			}
		}
		return null;
	}
		
	/**
	 * Gives a list of intervals when the other end of the link can be reached.
	 * If forward, these are incoming times + length.
	 * Otherwise, these are incoming times - length.
	 * @param incoming Interval where we can start
	 * @param capacity Capacity of the Link
	 * @param forward indicates whether we use residual edge or not 
	 * @param TimeHorizon for easy reference
	 * @return plain old Interval
	 */
	public ArrayList<Interval> propagate(final Interval incoming,
			final int capacity ,final boolean forward, int timehorizon){

		ArrayList<Interval> result = new ArrayList<Interval>();

		EdgeInterval current;
		Interval toinsert;

		int low = -1;
		int high = -1;						
		boolean collecting = false;

		if(forward) {			
			current = this.getIntervalAt(incoming.getLowBound());
			while (current.getLowBound() < incoming.getHighBound()) {				
				if (current.getFlow() < capacity) {				
					if (collecting) {
						high = current.getHighBound();
					} else {
						collecting = true;
						low = current.getLowBound();					  
						high = current.getHighBound();
					}

				} else {
					if (collecting) { // finish the Interval
						low = Math.max(low, incoming.getLowBound());
						low += this._traveltime;
						high = Math.min(high, incoming.getHighBound());
						high += this._traveltime;
						high = Math.min(high, timehorizon);
						if (low < high) {
						  toinsert = new Interval(low, high);					  
						  result.add(toinsert);
						}
						collecting = false;
					}
				}
				
				if (this.isLast(current)) {
					break;
				} 
				current = this.getIntervalAt(current.getHighBound());
			    				
			}

			if (collecting) { // finish the Interval
				low = Math.max(low, incoming.getLowBound());
				low += this._traveltime;
				high = Math.min(high, incoming.getHighBound());
				high += this._traveltime;
				high = Math.min(high, timehorizon);
				if (low < high) {
					toinsert = new Interval(low, high);					  
					result.add(toinsert);
				}
				collecting = false;
			}
		} else { // not forward
			if (incoming.getLowBound() - this._traveltime < 0) {
				current = this.getIntervalAt(0);
			} else {
			  current = this.getIntervalAt(incoming.getLowBound() - this._traveltime);
			}
			
			while (current.getLowBound() < incoming.getHighBound() - this._traveltime) {				
				if (current.getFlow() > 0) {				
					if (collecting) {
						high = current.getHighBound();
					} else {
						collecting = true;
						low = current.getLowBound();					  
						high = current.getHighBound();
					}

				} else {
					if (collecting) { // finish the Interval
						low = Math.max(low, incoming.getLowBound() - this._traveltime);							
						high = Math.min(high, incoming.getHighBound() - this._traveltime);
						high = Math.min(high, timehorizon);
						if (low < high) {
						  toinsert = new Interval(low, high);					  
						  result.add(toinsert);
						}
						collecting = false;
					}
				}
				
				if (this.isLast(current)) {
					break;
				}
				current = this.getIntervalAt(current.getHighBound());				
			} 

			if (collecting) { // finish the Interval
				low = Math.max(low, incoming.getLowBound() - this._traveltime);					  
				high = Math.min(high, incoming.getHighBound() - this._traveltime);
				high = Math.min(high, timehorizon);
				if (low < high) {
					toinsert = new Interval(low, high);					  
					result.add(toinsert);
				}
				collecting = false;
			}

		}
		return result;
	}
	

//------------------------Clean Up--------------------------------//
	/**
	 * unifies adjacent EdgeIntervals, call only when you feel it is safe to do
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = this._last.getHighBound();
		EdgeInterval i, j;
		i = getIntervalAt(0);		
		while (i.getHighBound() < timestop) {		  
		  j = this.getIntervalAt(i.getHighBound());
		  if(i.getHighBound() != j.getLowBound())
			  throw new RuntimeException("error in cleanup!");  
		  if (i.getFlow() == j.getFlow()) {
			  _tree.remove(i);
			  _tree.remove(j);
   		      j = new EdgeInterval(i.getLowBound(), j.getHighBound(), i.getFlow()); 			  
			  _tree.insert(j);
			  gain++;
		  }
		  i = j;		  		 		
		}		
		this._last = i; // we might have to update it, just do it always
		return gain;
	}
	
//------------------------Augmentation--------------------------------//
	
	/**
	 * increeases the flow into an edge from time t to t+1 by f if capacity is obeyed
	 * @param t raising time
	 * @param f aumount of flow to augment (can be negative)
	 * @param u capcity of the edge
	 */
	public void augment(final int t,final int gamma,final int u){
		if (t<0){
			throw new IllegalArgumentException("negative time: "+ t);
		}
		EdgeInterval i = getIntervalAt(t);
		if (i.getFlow() + gamma > u){
			throw new IllegalArgumentException("too much flow! flow: " + i.getFlow() + " + " +
					gamma + " > " + u);
		}
		if (i.getFlow() + gamma < 0){
			throw new IllegalArgumentException("negative flow! flow: " + i.getFlow() + " + " +
					gamma + " < 0");
		}
		
		// FIXME one of these cases should not happen ... I think
		if(i.getLowBound() < t){
			i= splitAt(t);
		}
		if(i.getHighBound() > (t+1)){
			splitAt(t+1);
		}
		i.augment(gamma, u);
	}
	
	/**
	 * increeases the flow into an edge from time t to t+1 by f
	 * no checking is done
	 * @param t raising time
	 * @param gamma aumount of flow to augment (can be negative) 
	 */
	public void augmentUnsafe(final int t, final int gamma){
		EdgeInterval i = getIntervalAt(t);
		// FIXME one of these cases should not happen ... I think
		if(i.getLowBound() < t){
			i= splitAt(t);
		}
		if(i.getHighBound() > (t+1)){
			splitAt(t+1);
		}
		i.augmentUnsafe(gamma);
	}
	
	/**
	 * decreases the flow into an edge from time t to t+1 by f if flow remains nonnegative
	 * @param t raising time
	 * @param f amount of flow to reduce
	 * @deprecated
	 */
	public void augmentreverse(final int t,final int f){
		if (t<0){
			throw new IllegalArgumentException("negative time : "+ t);
		}
		EdgeInterval i= getIntervalAt(t);
		if(f<0){
			throw new IllegalArgumentException("cannot reduce flow by a negative amount");
		}
		int oldflow= i.getFlow();
		if(oldflow-f <0){
			throw new IllegalArgumentException("flow would get negative");
		}
		if(i.getLowBound() < t){
			i= splitAt(t);
		}
		if(i.getHighBound() > t+1){
			splitAt(t+1);
		}
		i.setFlow(oldflow-f);
	}
	


}
