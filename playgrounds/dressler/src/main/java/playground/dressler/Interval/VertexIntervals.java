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

import org.matsim.api.core.v01.network.Link;

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
	

	
	/**
	 * Gives the predecessor Link on the Vertex at time t
	 * @param t time
	 * @return flow at t
	 */
	public PathStep getPred(int t){
		return getIntervalAt(t).getPredecessor().copyShiftedToArrival(t);
	}
	

//------------------------------GETTER-----------------------//


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
	public VertexInterval getFirstPossible(){
		VertexInterval result = this.getIntervalAt(0);
		while(!this.isLast(result)){
			if (result.getReachable()){
				return result;
			}else{
				result=this.getNext(result);
			}
		}
		if (result.getReachable()){
			return result;
		}	
		return null;
	}
	
	/**
	 * calculates the first time where it is reachable 
	 * @return minimal time or Integer.MAX_VALUE if it is not reachable at all
	 */
	public int firstPossibleTime(){
		VertexInterval test =this.getFirstPossible();
		if(test!=null){
			return test.getLowBound();
		}else{
			return Integer.MAX_VALUE;
		}
	}
	
	/**
	 * Sets arrival true for all time steps in arrive and sets predecessor to link for each time t
	 * where it was null beforehand
	 * @deprecated setTrueList does the same and better
	 * @param arrive VertexIntervals at which node is reachable
	 * @param pred Predecessor PathStep. It will always be shifted to the beginning of the interval
	 * @return true iff anything was changed
	 */
	public boolean setTrue(ArrayList<Interval> arrive, PathStep pred) {
		boolean changed = false;
		boolean temp;
		// there used to be condensing here ...
		// but propagate already condenses these days
		for(int i=0; i< arrive.size(); i++){
		  temp = setTrue(arrive.get(i), pred);
		  changed = changed || temp;
		}
		return changed;
	}
	

	
	/**
	 * Sets arrival true for all time steps in arrive that were not reachable and sets the predecessor to pred
	 * @deprecated setTrueList does the same and better  
	 * @param arrive Interval at which node is reachable
	 * @param pred Predecessor PathStep. It will always be shifted to the beginning of the interval
	 * @return true iff anything was changed
	 */
	public boolean setTrue(Interval arrive, PathStep pred){
		// slightly slower, but easier to manage if this just calls the new setTrueList
		ArrayList<VertexInterval> temp = setTrueList(arrive, pred);
		return (temp != null && !temp.isEmpty());
	}
	
	/**
	 * Sets arrival true for all intervals in arrive and sets predecessor to link for each time t
	 * where it was null beforehand
	 * @param arrive Intervals at which node is reachable
	 * @param pred Predecessor PathStep. It will always be shifted to the beginning of the interval
	 * @return null or list of changed intervals iff anything was changed
	 */
    public ArrayList<VertexInterval> setTrueList(ArrayList<Interval> arrive, PathStep pred) {
		
		if (arrive == null || arrive.isEmpty()) { return null; }
				
		ArrayList<VertexInterval> changed = new ArrayList<VertexInterval>();
		
		// there used to be condensing here ...
		// but propagate already condenses these days
		
		Iterator<Interval> iterator = arrive.iterator();
		Interval i;
								
		while(iterator.hasNext()) {
			i = iterator.next();	        
		    changed.addAll(setTrueList(i, pred));
		}
				
		return changed;
	}

    /**
	 * Sets arrival true for all time steps in arrive and sets predecessor to link for each time t
	 * where it was null beforehand
	 * @param arrive Interval at which node is reachable
	 * @return null or list of changed intervals iff anything was changed
	 */
	public ArrayList<VertexInterval> setTrueList(Interval arrive, PathStep pred){
		// TODO Test !
		ArrayList<VertexInterval> changed = new ArrayList<VertexInterval>();		
		VertexInterval current = this.getIntervalAt(arrive.getLowBound());
		int t = current.getHighBound();
		while(current.getLowBound() < arrive.getHighBound()){
			//current was never reachable before and is not scanned
			if(!current.getReachable() && !current.isScanned())
			{
				//test if the intervals intersect at all (using the condition in while head above)
				if(arrive.getLowBound() >= current.getLowBound()
						|| arrive.getHighBound() > current.getLowBound())
				{
					//if arrive contains current, we relabel it completely
					if(arrive.contains(current))
					{
						current.setArrivalAttributes(pred);						
						changed.add(current);
					}
					else if(current.contains(arrive))
					{
						//if arrive is contained..
						//we adapt current, so that our lowbound equals
						//the low bound of the arrive interval..
						if(current.getLowBound() < arrive.getLowBound())
						{
							current = this.splitAt(arrive.getLowBound());
						}
						//or we set our highbound to the highbound of arrival
						if(current.getHighBound() > arrive.getHighBound())
						{
							this.splitAt(arrive.getHighBound());
							current = this.getIntervalAt(arrive.getHighBound()-1);
						}
						//current has exactly the same bounds as arrive
						//so relabel it completely
						current.setArrivalAttributes(pred);						
						changed.add(current);
					}
					else
					{
						//ourInterval intersects arrive, but is neither contained nor does it contain
						//arrive. thus they overlap somewhere
						//if the lowerBound of arrive, is greater than our lower bound
						//we set our lower bound to the bound of arrive
						if(arrive.getLowBound() > current.getLowBound() && arrive.getLowBound() < current.getHighBound())
						{
							current = this.splitAt(arrive.getLowBound());
						}
						//we adapt our highbound, so that they are the same
						if(arrive.getHighBound() < current.getHighBound())
						{
							this.splitAt(arrive.getHighBound());
							current = this.getIntervalAt(arrive.getHighBound()-1);
						}
						//we set the attributes
						current.setArrivalAttributes(pred);
						changed.add(current);
					}
				}
			}
			
			
			if (isLast(current)) {
				break;
			}
			t = current.getHighBound();
			current= this.getIntervalAt(t);
		}	
		return changed;
	}
	
	
	
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
		
		VertexInterval i, j;
		i = getIntervalAt(0);		
		while (i.getHighBound() < timestop) {		  
		  j = this.getIntervalAt(i.getHighBound());
		  if(i.getHighBound() != j.getLowBound())
			  throw new RuntimeException("error in cleanup!");
		  if (i.getReachable() == j.getReachable() 
				  && i.isScanned() == j.isScanned()
				  && i.getPredecessor().equals(j.getPredecessor())) {
			  _tree.remove(i);
			  _tree.remove(j);
			  j = new VertexInterval(i.getLowBound(), j.getHighBound(), i);
			  _tree.insert(j);			  
			  gain++;
		  } 
		  i = j;
		}
		this._last = i;
		
		return gain;
	}
	
	/**
	 * Gives the first reachable but unscanned VertexInterval 
	 * @return the VertexInterval or null if it does not exist
	 */
	public VertexInterval getFirstUnscannedInterval()
	{
		int lowBound = 0;
		VertexInterval vI;
		do
		{
			vI = this.getIntervalAt(lowBound);
			if(vI.getReachable() &&  !vI.isScanned())
				return vI;
			lowBound = vI.getHighBound();
			if (isLast(vI)) {
				break;
			}
		} while (!isLast(vI));
		return null;
	}
	
	/**
	 * Returns the lowbound of the first unscanned but reachable VertexInterval
	 * @return the Value of the lowbound or null if it does not exist
	 */
	public Integer getFirstTimePointWithDistTrue()
	{
		VertexInterval vInterval = this.getFirstUnscannedInterval();
		if(vInterval == null)
			return null;
		else
			return vInterval.getLowBound();
	}
	
	/**
	 * Checks whether the given VertexInterval is the last
	 * @param o EgeInterval which it test for 
	 * @return true if getLast.equals(o)
	 */
	@Override
	public boolean isLast(VertexInterval o){
		return super.isLast(o);
		
		//TODO check if other fields are equal 
	}
}
