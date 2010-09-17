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
import java.util.LinkedList;

/**
 * class representing the flow of an edge in a Time Expanded Network
 * @author Manuel Schneider
 */
public class HoldoverIntervals extends Intervals<HoldoverInterval> implements EdgeFlowI{
	//TODO holdover done implement HoldoverIntervalls
	
	/**
	 * availability for easy access
	 */
	//public final Interval _whenAvailable;
	
	/**
	 * availability for easy access
	 */
	public final int _capacity;
	
	
	/**
	 * debug flag
	 */
	private static int _debug =0;
	
//********************************METHODS******************************************//
		
//------------------------------CONSTRUCTORS---------------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * the given interval, and stores the traveltime
	 */
	public HoldoverIntervals(HoldoverInterval interval, final int capacity){
		super(interval); 
		this._capacity = capacity;
	}


//--------------------------------------FLOW---------------------------------------//	
	
	/**
	 * Gives the holdover at time t
	 * @param t time
	 * @return holdover at t
	 */
	public int getFlowAt(final int t){
		return getIntervalAt(t).getFlow();
	}
	
//-------------------------------------GETTER--------------------------------------//

	
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
			final boolean primal, final boolean reverse, int timehorizon){
		if(reverse){
			throw new RuntimeException("no propagating of holdover implemented for reverse search"); 
		}
		ArrayList<Interval> result = new ArrayList<Interval>();
		HoldoverInterval current;
		Interval toinsert;
		//if(true) return result;

		int low = -1;
		int high = -1;						
		boolean collecting = false;
		if(primal){
			int effectiveStart = incoming.getHighBound() ;
			int effectiveEnd = timehorizon ;
			if(effectiveStart==effectiveEnd){
				return result;
			}
			current = this.getIntervalAt(effectiveStart);

			while (current.getLowBound() < effectiveEnd) {
				int flow = current.getFlow();
				if (flow < this._capacity) {				
					if (collecting) {
						high = current.getHighBound();
					} else {
						collecting = true;
						low = current.getLowBound();					  
						high = current.getHighBound();
					}

				} else {
					if (collecting) { // finish the Interval
						low = Math.max(low, effectiveStart);
						high = Math.min(high, effectiveEnd);
						if (low < high) {
							toinsert = new Interval(low, high);					  
							result.add(toinsert);
							collecting =false;		
							break;
						}
						collecting = false;
					}
				}
				System.out.println("current loww bound"+ current.getLowBound());
				if (this.isLast(current)) {
					break;
				} 
				current = this.getIntervalAt(current.getHighBound());

			}
			System.out.println("done looping");
			if (collecting) { // finish the Interval
				low = Math.max(low, effectiveStart);
				high = Math.min(high, effectiveEnd);
				if (low < high) {
					toinsert = new Interval(low, high);					  
					result.add(toinsert);
				}
				collecting = false;
			}
		}else{  //TODO holdover a implement propagation residual holdover
			;
		}
		
		return result;
	}
	

//------------------------Clean Up--------------------------------//
	/**
	 * unifies adjacent HoldoverIntervals, call only when you feel it is safe to do
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = this._last.getHighBound();
		HoldoverInterval i, j;
		i = getIntervalAt(0);		
		while (i.getHighBound() < timestop) {		  
		  j = this.getIntervalAt(i.getHighBound());
		  if(i.getHighBound() != j.getLowBound())
			  throw new RuntimeException("error in cleanup!");  
		  if (i.getFlow() == j.getFlow()) {
			  _tree.remove(i);
			  _tree.remove(j);
   		      j = new HoldoverInterval(i.getLowBound(), j.getHighBound(), i.getFlow()); 			  
			  _tree.insert(j);
			  gain++;
		  }
		  i = j;		  		 		
		}		
		this._last = i; // we might have to update it, just do it always
		return gain;
	}
	
//------------------------Augmentation--------------------------------//
	
	
	@Override
	public void augment(int tstart, int tstop, int gamma) {
		if (tstart<0){
			throw new IllegalArgumentException("negative time: "+ tstart);
		}
		if (tstop>this.getLast()._r){
			throw new IllegalArgumentException(" to late end of holdover:"+ tstop );
		}
		Pair<LinkedList<HoldoverInterval>,Pair<Integer,Integer>> relevant = getIntersecting(tstart,tstop);
		int lowest= relevant.second.first;
		int highest= relevant.second.second;
		
		if (highest + gamma > this._capacity){
			throw new IllegalArgumentException("too much flow! flow: " + highest + " + " +
					gamma + " > " + this._capacity);
		}
		if (lowest + gamma < 0){
			throw new IllegalArgumentException("negative flow! flow: " + lowest + " + " +
					gamma + " < 0");
		}
		for(HoldoverInterval i : relevant.first){
			if(i.getLowBound() < tstart){
				i= splitAt(tstart);
			}
			if(i.contains(tstop)){
				splitAt(tstop);
				i = getIntervalAt(tstop-1); // just to be safe
			}
			i.augment(gamma, this._capacity);
		}
		this.cleanup();
		
	}
	
	
	private Pair<LinkedList<HoldoverInterval>,Pair<Integer,Integer>> getIntersecting(int tstart,int tstop){
		LinkedList<HoldoverInterval> list = new LinkedList<HoldoverInterval>();
		HoldoverInterval temp = this.getIntervalAt(tstart);
		list.add(temp);
		int lowest = temp.getFlow();
		int highest=temp.getFlow();
		while(!this.isLast(temp)){
			temp=this.getNext(temp);
			if(temp._l>=tstop){
				break;
			}
			if (lowest>temp.getFlow()){
				lowest=temp.getFlow();
			}
			if (highest<temp.getFlow()){
				highest=temp.getFlow();
			}
			list.add(temp);
		}
		Pair<Integer,Integer> flows =new Pair<Integer,Integer>(lowest,highest);
		Pair<LinkedList<HoldoverInterval>,Pair<Integer,Integer>> result = new Pair<LinkedList<HoldoverInterval>,Pair<Integer,Integer>>(list,flows);
		return result;
	}


	@Override
	public void augmentUnsafe(int tstart, int tstop, int gamma) {
		if (tstart<0){
			throw new IllegalArgumentException("negative time: "+ tstart);
		}
		if (tstop>this.getLast()._r){
			throw new IllegalArgumentException(" to late end of holdover:"+ tstop );
		}
		Pair<LinkedList<HoldoverInterval>,Pair<Integer,Integer>> relevant = getIntersecting(tstart,tstop);
		int lowest= relevant.second.first;
		int highest= relevant.second.second;
		
		if (highest + gamma > this._capacity){
			throw new IllegalArgumentException("too much flow! flow: " + highest + " + " +
					gamma + " > " + this._capacity);
		}
		if (lowest + gamma < 0){
			throw new IllegalArgumentException("negative flow! flow: " + lowest + " + " +
					gamma + " < 0");
		}
		for(HoldoverInterval i : relevant.first){
			if(i.getLowBound() < tstart){
				i= splitAt(tstart);
			}
			if(i.contains(tstop)){
				splitAt(tstop);
				i = getIntervalAt(tstop-1); // just to be safe
			}
			i.augment(gamma, this._capacity);
		}
		
		
	}
	
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(int debug){
		HoldoverIntervals._debug=debug;
	}

	
	public boolean checkFlowAt(final int t, final int cap) {
		return this.getIntervalAt(t).checkFlow(cap);
	}


	@Override
	public int getLastTime() {	
		if (this.getLast().getFlow() == 0) {
			return this.getLast().getLowBound();
		}
		return this.getLast().getHighBound(); // usually the TimeHorizon!		
	}


	@Override
	public int getMeasure() {
		return this.getSize();
	}
	
	/**
	 * not suited for holdover
	 */
	@Deprecated
	public void augment(final int t, final int gamma){throw new RuntimeException("wrong augment for holdover!!!!");}
	
	/**
	 * not suited for holdover
	 */
	@Deprecated
	public void augmentUnsafe(final int t, final int gamma){throw new RuntimeException("wrong augment for holdover!!!!");}

	
	
}