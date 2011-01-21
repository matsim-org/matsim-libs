/* *********************************************************************** *
 * project: org.matsim.*												   *
 * VertexIntervals.java												   *
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

//playground imports
package playground.dressler.Interval;

//java imports
import java.util.ArrayList;
import java.util.Iterator;

import playground.dressler.ea_flow.PathStep;

/**
 * class representing the flow of an edge in a Time Expanded Network
 * @author Manuel Schneider
 *
 */
public class VertexIntervals extends Intervals<VertexInterval> {

//------------------------FIELDS----------------------------------//
	
	
	
	/**
	 * flag for debug mode
	 */
	@SuppressWarnings("unused")
	private static int _debug = 0;
	
	
//-----------------------METHODS----------------------------------//
//****************************************************************//
	
	 
//----------------------CONSTRUCTORS------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * the given interval
	 */
	public VertexIntervals(VertexInterval interval){
		super(interval);
	}

	//------------------------------METHODS-----------------------//
	
	/**
	 * Gives the predecessor Link on the Vertex at time t
	 * @param t time
	 * @return flow at t
	 */
	public PathStep getPred(int t){
		return getIntervalAt(t).getPredecessor().copyShiftedToArrival(t);
	}
	
	/**
	 * Gives the successor Link on the Vertex at time t
	 * @param t time
	 * @return flow at t
	 */
	public PathStep getSucc(int t){
		return getIntervalAt(t).getSuccessor().copyShiftedToStart(t);
	}
	

	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(int debug){
		VertexIntervals._debug=debug;
	}
	
	/**
	 * finds the first VertexInterval within which
	 *  the node is reachable from the source
	 * @return specified VertexInterval or null if none exist
	 */
	public VertexInterval getFirstPossibleForward(){
		VertexInterval result = this.getIntervalAt(0);
		while(!this.isLast(result)){
			if (result.getReachable() && result.getPredecessor() != null){
				return result;
			}else{
				result=this.getNext(result);
			}
		}
		
		if (result.getReachable() && result.getPredecessor() != null){
			return result;
		}
		
		return null;
	}
	
	/**
	 * calculates the first time where it is reachable 
	 * @return minimal time or Integer.MAX_VALUE if it is not reachable at all
	 */
	public int firstPossibleTime(){
		VertexInterval test =this.getFirstPossibleForward();
		if(test!=null){
			return test.getLowBound();
		}else{
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Sets arrival true for all intervals in arrive and sets predecessor to link for each time t
	 * where it was null beforehand
	 * @param arrive Intervals at which node is reachable
	 * @param pred PathStep to set as breadcrumb (predecessor or successor)
	 *         It will always be shifted to the beginning of the interval, according to reverse
	 * @param reverse Is this for the reverse search?
	 * @return (possibly empty) list of changed intervals
	 * @deprecated
	 *//*
    public ArrayList<VertexInterval> setTrueList(ArrayList<Interval> arrive, PathStep pred, final boolean reverse) {
		
    	ArrayList<VertexInterval> changed = new ArrayList<VertexInterval>();
    	
		if (arrive == null || arrive.isEmpty()) { return changed; }
		
		// there used to be condensing here ...
		// but propagate already condenses these days
		
		Iterator<Interval> iterator = arrive.iterator();
		Interval i;
								
		while(iterator.hasNext()) {
			i = iterator.next();	        
		    changed.addAll(setTrueList(i, pred, reverse));
		}
				
		return changed;
	}

    *//**
	 * Sets arrival true for all time steps in arrive and sets predecessor to link for each time t
	 * where it was null beforehand
	 * @param arrive Interval at which node is reachable
	 * @param pred PathStep to set as breadcrumb (predecessor or successor)
	 *         It will always be shifted to the beginning of the interval, according to reverse
	 * @param reverse Is this for the reverse search?  
	 * @return null or list of changed intervals iff anything was changed
	 * @ deprecated
	 *//*
	public ArrayList<VertexInterval> setTrueList(Interval arrive, PathStep pred, final boolean reverse){
		ArrayList<VertexInterval> changed = new ArrayList<VertexInterval>();		
		VertexInterval current = this.getIntervalAt(arrive.getLowBound());
				
		while(current.getLowBound() < arrive.getHighBound()){

			// only do something if current was not reachable yet
			if(!current.getReachable()) {
				
				// let's modify the interval to look jus right
				if(current.getLowBound() < arrive.getLowBound()) {
					current = this.splitAt(arrive.getLowBound());
				}
				
				if(current.getHighBound() > arrive.getHighBound())
				{
					this.splitAt(arrive.getHighBound());
					// pick the interval again to be on the safe side
					current = this.getIntervalAt(arrive.getHighBound()-1);
				}
				
				// and set the attributes
				if (!reverse) {
					//current.setArrivalAttributesForward(pred.copyShiftedToArrival(current.getLowBound()));
					current.setArrivalAttributesForward(pred);
				} else {
					//current.setArrivalAttributesReverse(pred.copyShiftedToStart(current.getLowBound()));
					current.setArrivalAttributesReverse(pred);
				}
				changed.add(current);
			}


			if (isLast(current)) {
				break;
			}			
			current = this.getIntervalAt(current.getHighBound());
		}	
		return changed;
	}*/
	
	public ArrayList<VertexInterval> setTrueList(ArrayList<VertexInterval> arrive) {
			
	    	ArrayList<VertexInterval> changed = new ArrayList<VertexInterval>();
	    	
			if (arrive == null || arrive.isEmpty()) { return changed; }
			
			// there used to be condensing here ...
			// but propagate already condenses these days
			
			Iterator<VertexInterval> iterator = arrive.iterator();
			VertexInterval i;
									
			while(iterator.hasNext()) {
				i = iterator.next();	        
			    changed.addAll(setTrueList(i));
			}
					
			return changed;
		}
	 
	 public ArrayList<VertexInterval> setTrueList(ArrayList<Interval> arrive, VertexInterval arriveProperties) {
			
	    	ArrayList<VertexInterval> changed = new ArrayList<VertexInterval>();
	    	
			if (arrive == null || arrive.isEmpty()) { return changed; }
			
			// there used to be condensing here ...
			// but propagate already condenses these days
			
			Iterator<Interval> iterator = arrive.iterator();
			Interval i;
			//VertexInterval temp = new VertexInterval(arriveProperties);
									
			while(iterator.hasNext()) {
				i = iterator.next();
				arriveProperties._l = i._l;
				arriveProperties._r = i._r;
			    changed.addAll(setTrueList(arriveProperties));
			}
					
			return changed;
		}
	
	/**
	 * Sets arrival true for all time steps where arrive is better than the existing interval
	 * @param arrive VertexInterval suitable to be copied to this vertex  
	 * @return null or list of changed intervals iff anything was changed
	 */
	public ArrayList<VertexInterval> setTrueList(VertexInterval arrive){
		
		ArrayList<VertexInterval> changed = new ArrayList<VertexInterval>();	
		VertexInterval current = this.getIntervalAt(arrive.getLowBound());
		
		while(current.getLowBound() < arrive.getHighBound()){
			
			// only do something if current was not reachable or can be improved
			Interval improvement = arrive.isBetterThan(current);	
		
			if (improvement != null) {
				
				// let's modify the interval to look just right
				if (current.getLowBound() < improvement.getLowBound()) {
					current = this.splitAt(improvement.getLowBound());
				}
				
				if(current.getHighBound() > improvement.getHighBound())
				{
					this.splitAt(improvement.getHighBound());
					// pick the interval again to be on the safe side
					current = this.getIntervalAt(improvement.getHighBound()-1);
				}
				
				
				// if it was not yet reachable, we definitely have to scan.
				boolean needsScanning = !current.getReachable();
				
				// set the attributes and maybe there is a reason to scan again
				// note: this is not flexible enough for a full mixed search ...
				// but that could probably be done quicker in two passes anyway
				
				if (current.setArrivalAttributes(arrive)) {
					needsScanning = true;
				}

				if (needsScanning) {
					current.setScanned(false);
					changed.add(current);
				}
			}


			if (isLast(current)) {
				break;
			}			
			current = this.getIntervalAt(current.getHighBound());
		}	
				
		
		return changed;
	}
	


	/**
	 * unifies adjacent intervals, call only when you feel it is safe to do
	 * @return number of unified VertexIntervals
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = getLast().getHighBound();		
	    
		//System.out.println("VertexIntervals.cleanup()");
		//System.out.println(this.toString());
		
		VertexInterval i, j;
		i = getIntervalAt(0);		
		while (i.getHighBound() < timestop) {		  
		  j = this.getIntervalAt(i.getHighBound());
		  if(i.getHighBound() != j.getLowBound())
			  throw new RuntimeException("error in cleanup!");
		  //if (i.getReachable() == j.getReachable() 
		  //		  && i.isScanned() == j.isScanned()) {
		  
		  if (i.continuedBy(j)) {			  
			  // FIXME use a safer method for removing things!
			  _tree.remove(i);
			  _tree.remove(j);
			  j = new VertexInterval(i.getLowBound(), j.getHighBound(), i);
			  _tree.insert(j);			  
			  gain++;

		  } 
		  i = j;
		}
		this._last = i; // we might have to update it, just do it always
		
		return gain;
	}

	
	// primitive TEST
	public static void main (String[] args) {
		System.out.println("Running test for VertexIntervals");
		VertexIntervals VI = new VertexIntervals(new VertexInterval(0, 1000));
		System.out.println(VI);
		System.out.flush();
		VI.splitAt(5);
		System.out.println(VI);
		VI.splitAt(7);
		System.out.println(VI);
		VI.splitAt(10);
		System.out.println(VI);
		VI.splitAt(1);
		System.out.println(VI);
	}
	
}
