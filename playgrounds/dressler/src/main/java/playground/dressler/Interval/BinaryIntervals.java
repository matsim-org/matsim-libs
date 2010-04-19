/* *********************************************************************** *

 * project: org.matsim.*												   *
 * BinaryIntervals.java													   *
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

import javax.management.RuntimeErrorException;

//playground imports
import playground.dressler.control.FlowCalculationSettings;

/**
 * Class representing a binary value over time.
 * @author Daniel Dressler
 *
 */
public class BinaryIntervals extends Intervals<BinaryInterval> {

	public static int _debug = 0;
	
//********************************METHODS******************************************//
		
//------------------------------CONSTRUCTORS---------------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * the given interval
	 */
	public BinaryIntervals(BinaryInterval interval){
		super(interval); 		
	}


//--------------------------------------VALUE---------------------------------------//	
	
	/**
	 * Gives the value at time t
	 * @param t time
	 * @return value at t
	 */
	public boolean getValAt(final int t){
		return getIntervalAt(t).val;
	}
	


//------------------------Clean Up--------------------------------//
	/**
	 * unifies adjacent Intervals, call only when you feel it is safe to do
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = this._last.getHighBound();
		BinaryInterval i, j;
		i = getIntervalAt(0);		
		while (i.getHighBound() < timestop) {		  
		  j = this.getIntervalAt(i.getHighBound());
		  if(i.getHighBound() != j.getLowBound())
			  throw new RuntimeException("error in cleanup!");  
		  if (i.val == j.val) {
			  // FIXME use a safer method for removing things!
			  _tree.remove(i);
			  _tree.remove(j);
   		      j = new BinaryInterval(i.getLowBound(), j.getHighBound(), i.val); 			  
			  _tree.insert(j);
			  gain++;
		  }
		  i = j;		  		 		
		}		
		this._last = i; // we might have to update it, just do it always
		return gain;
	}
	
//------------------------ Setting --------------------------------//
	
	/**
	 * Sets the value a time t (and time t only)
	 * @param t time
	 * @param v the new value to set
	 */
	public void augment(final int t, final boolean v){
		if (t<0) {
			throw new IllegalArgumentException("negative time: "+ t);
		}
		BinaryInterval i = getIntervalAt(t);
		

		if (i.val == v) {
			// nothing to do
			return;
		}
		
		// Let's do implicit cleanup!
		boolean joinup = false;
		boolean joindown = false;
		
		BinaryInterval down = null;
		BinaryInterval up = null;
		
		// maybe the interval at t+1 has the correct value ? 
		if (!isLast(i)) {
		   up = getIntervalAt(t + 1);
		   if (up.val == v) {
			   joinup = true;
			   /* This is a very special case.
			    In particular this means i != j and
			    i is [l, t+1) and j is [t+1, r)
			    and j has the value we want, while i does not.
			    Now we can simply change the endpoints a bit:
			    --i._r;
			    --j._l;			      
			   
			   
			   // Does i vanish?			  
			   if (i._l + 1 == i._r ) {
				  this._tree.remove(i);
			   } else {
				  i.setHighBound(i._r - 1);   
			   }
			   
			   j.setLowBound(j._l - 1);
			   
			   return;*/
		   }
		}
		
		// or maybe the interval at t-1 has the correct value ? 
		if (t > 0) {
		   down = getIntervalAt(t - 1);
		   if (down.val == v) {
			   joindown = true;
			   /* This is a very special case again.
			    In particular this means i != j and
			    i is [t, r) and j is [l, t)
			    and j has the value we want, while i does not.			    
			    Now we can simply change the endpoints a bit:
			    ++i._l;
			    ++j._r;
			   
			   
			   if (i._l + 1 == i._r ) {
				   this._tree.remove(i);   
			   } else {
				   i.setLowBound(i._l + 1);   
			   }			   
			   j.setHighBound(j._r  + 1);

			   return;*/
		   }
		}
		
		
		if (joinup) {
			if (joindown) {
				// there is just one wrong value in between
				// ... v )[ !v )[ v ... should become  ... v v v ...
				this._tree.remove(i);
				this._tree.remove(up);
				down.setHighBound(up._r);
				
				if (this._last == up) { 
				  this._last = down; // important!
				}
				return;
			} else {
				// we can join up, but only up
				// ... !v)[ v ... should become ... ) [!v v ...
				if (i._l + 1 == i._r ) {
					this._tree.remove(i);   
				} else {
					i.setHighBound(i._r - 1);   
				}			   
				up.setLowBound(up._l  - 1);
				return;
			}
		} else if (joindown) {
			// we can join down, but only down
			// ... v )[ !v ... should become ... v v )[ ... 
			if (i._l + 1 == i._r ) {
				this._tree.remove(i);   
			} else {
				i.setLowBound(i._l + 1);   
			}			   
			down.setHighBound(down._r  + 1);	
			
			if (this._last == i) { 
			  this._last = down; // important!
			}			
			return;
		}
		
		// nothing can be joined, so we effectively create an interval
		// of length 1 at time t.
		
		if(i.getLowBound() < t) {
			i = splitAt(t);
		}
		
		if(i.getHighBound() > (t+1)) {
			splitAt(t + 1);
			i = getIntervalAt(t); // just to be safe
		}
		
		i.val = v;
		
	}
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(int debug){
		BinaryIntervals._debug = debug;
	}


}
