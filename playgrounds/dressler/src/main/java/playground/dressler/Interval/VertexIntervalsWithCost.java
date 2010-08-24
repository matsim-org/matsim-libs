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
import playground.dressler.ea_flow.PathStep;

/**
 * class representing the flow of an edge in a Time Expanded Network
 * @author Manuel Schneider
 *
 */
/* extneds Intervals<VertexIntervalWithCost> ? */ 
public class VertexIntervalsWithCost extends VertexIntervals {

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
	public VertexIntervalsWithCost(VertexIntervalWithCost interval){
		super(interval);
	}
	

	
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
	

//------------------------------GETTER-----------------------//


	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(int debug){
		VertexIntervalsWithCost._debug=debug;
	}
	
	/**
	 * finds the first VertexInterval within which
	 *  the node is reachable from the source
	 * @return specified VertexInterval or null if none exist
	 */
	public VertexIntervalWithCost getFirstPossibleForward(){
		VertexIntervalWithCost result = (VertexIntervalWithCost) this.getIntervalAt(0);
		while(!this.isLast(result)){
			if (result.getReachable() && result.getPredecessor() != null){
				return result;
			}else{
				result = (VertexIntervalWithCost) this.getNext(result);
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
		VertexIntervalWithCost test =this.getFirstPossibleForward();
		if(test!=null){
			return test.getLowBound();
		}else{
			return Integer.MAX_VALUE;
		}
	}

   /* @Override
	public ArrayList<VertexInterval> setTrueList(ArrayList<VertexInterval> arrive) {
			
	    	ArrayList<VertexInterval> changed = new ArrayList<VertexInterval>();
	    	
			if (arrive == null || arrive.isEmpty()) { return changed; }
			
			// there used to be condensing here ...
			// but propagate already condenses these days
			
			Iterator<VertexIntervalWithCost> iterator = arrive.iterator();
			VertexIntervalWithCost i;
									
			while(iterator.hasNext()) {
				i = iterator.next();	        
			    changed.addAll(setTrueList(i));
			}
					
			return changed;
		}
		
	 */
	
	
	/* public ArrayList<VertexIntervalWithCost> setTrueList(ArrayList<Interval> arrive, VertexIntervalWithCost arriveProperties) {
			
	    	ArrayList<VertexIntervalWithCost> changed = new ArrayList<VertexIntervalWithCost>();
	    	
			if (arrive == null || arrive.isEmpty()) { return changed; }
			
			// there used to be condensing here ...
			// but propagate already condenses these days
			
			Iterator<Interval> iterator = arrive.iterator();
			Interval i;
			VertexIntervalWithCost temp = new VertexIntervalWithCost(arriveProperties);
									
			while(iterator.hasNext()) {
				i = iterator.next();
				temp._l = i._l;
				temp._r = i._r;
			    changed.addAll(setTrueList(temp));
			}
					
			return changed;
		}*/
	
	/**
	 * Sets arrival true for all time steps where arrive is better than the existing interval
	 * @param arrive VertexInterval suitable to be copied to this vertex  
	 * @return null or list of changed intervals iff anything was changed
	 *//*
	public ArrayList<VertexInterval> setTrueList(VertexInterval arrive){
		
		ArrayList<VertexInterval> changed = new ArrayList<VertexInterval>();	
		VertexIntervalWithCost current = (VertexIntervalWithCost) this.getIntervalAt(arrive.getLowBound());
				
		while(current.getLowBound() < arrive.getHighBound()){

			// only do something if current was not reachable or can be improved			
			Interval improvement = arrive.isBetterThan(current); 
			if (improvement != null) {
				
				// let's modify the interval to look just right
				if(current.getLowBound() < improvement.getLowBound()) {
					current =(VertexIntervalWithCost) this.splitAt(improvement.getLowBound());
				}
				
				if(current.getHighBound() > improvement.getHighBound())
				{
					this.splitAt(improvement.getHighBound());
					// pick the interval again to be on the safe side
					current = (VertexIntervalWithCost) this.getIntervalAt(improvement.getHighBound()-1);
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
	*/
	
	
//------------------------Clean Up--------------------------------//

	/**
	 * unifies adjacent intervals, call only when you feel it is safe to do
	 * @return number of unified VertexIntervals
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = getLast().getHighBound();		
	    
		//System.out.println("VertexIntervals.cleanup()");
		//System.out.println(this.toString());
		
		VertexIntervalWithCost i, j;
		i = (VertexIntervalWithCost) getIntervalAt(0);		
		while (i.getHighBound() < timestop) {		  
		  j = (VertexIntervalWithCost) this.getIntervalAt(i.getHighBound());
		  if(i.getHighBound() != j.getLowBound())
			  throw new RuntimeException("error in cleanup!");
		  //if (i.getReachable() == j.getReachable() 
		  //		  && i.isScanned() == j.isScanned()) {
		  
		  if (i.continuedBy(j)) {			  
			  // FIXME use a safer method for removing things!
			  _tree.remove(i);
			  _tree.remove(j);
			  j = new VertexIntervalWithCost(i.getLowBound(), j.getHighBound(), i);
			  _tree.insert(j);			  
			  gain++;

		  } 
		  i = j;
		}
		this._last = i; // we might have to update it, just do it always
		
		return gain;
	}

}
