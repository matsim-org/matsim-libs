/* *********************************************************************** *

 * project: org.matsim.*												   *
 * ShadowEdgeFlow.java													   *
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

/**
 * Class representing the flow of an edge in a Time Expanded Network
 * This version tries to be faster by using only a "shadow" of the flow. 
 * @author Daniel Dressler
 */
public class ShadowEdgeFlow implements EdgeFlowI {

	/**
	 * traveltime for easy access 
	 */
	public final int _traveltime;
	
	/**
	 * availability for easy access
	 */
	public final Interval _whenAvailable;
	
	/**
	 * availability for easy access
	 */
	public final int _capacity;
	
	
	/**
	 * debug flag
	 */
	private static int _debug = 0;
	
	private ArrayList<Integer> _flow;	
	
	private BinaryIntervals _forwardshadow;
	private BinaryIntervals _residualshadow;
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * the given interval, and stores the traveltime
	 */
	public ShadowEdgeFlow(Interval interval, final int traveltime, final int capacity, final Interval whenAvailable){
		
		// usually there is forward capacity in an empty flow, but only if there is some capacity at all. 
		this._forwardshadow = new BinaryIntervals(new BinaryInterval(interval, capacity > 0));
		// there never is residual capacity in an empty flow
		this._residualshadow = new BinaryIntervals(new BinaryInterval(interval, false));
		
		this._traveltime = traveltime;
		this._capacity = capacity;
		
		// Intervals expects that it starts at 0, so we cannot restrict ourselves
		// to just the available interval ...
		this._whenAvailable = whenAvailable;
		
		this._flow = new ArrayList<Integer>();		
	}

	@Override
	public void augment(int t, int gamma) {
		if (t<0){
			throw new IllegalArgumentException("negative time: "+ t);
		}
		
		if (t >= this._flow.size()) {
		  for (int n = this._flow.size(); n <= t; n++) {			  
			  this._flow.add(0);
		  }		  
		}
		
		int f = this._flow.get(t);
		int newf = f + gamma;
		
		if (newf > this._capacity) {
			throw new IllegalArgumentException("too much flow! flow: " + f + " + " +
					gamma + " > " + this._capacity);
		}
		if (newf < 0) {
			throw new IllegalArgumentException("negative flow! flow: " + f + " + " +
					gamma + " < 0");
		}			
		
		// set the flow value
		this._flow.set(t, newf);
		
		// update the shadows
		this._residualshadow.augment(t, newf > 0);
		this._forwardshadow.augment(t, newf < _capacity);
		
	}

	@Override
	public void augmentUnsafe(int t, int gamma) {
		
		if (t >= this._flow.size()) {
			_flow.ensureCapacity(t + 1);
			for (int n = this._flow.size(); n <= t; n++) {
				this._flow.add(0);
			}		  
		}
		
		int f = this._flow.get(t);	
		int newf = f + gamma;
		
		// set the flow value
		this._flow.set(t, newf);
		
		// update the shadows
		this._residualshadow.augment(t, newf > 0);
		this._forwardshadow.augment(t, newf < _capacity);
		
	}

	@Override
	public boolean checkFlowAt(int t, int cap) {
		int f = this._flow.get(t);
		
		// check capacity
		if (f < 0) return false;
		if (f > _capacity) return false;
		
		// check consistencies of the shadows as well
		if (f > 0 && !_residualshadow.getValAt(t)) return false;
		
		if (f < _capacity && !_forwardshadow.getValAt(t)) return false;
			
		return true;
	}

	@Override
	public int cleanup() {
		// not needed! cleanup is done implicitly
		if (_debug > 0) {
			// DEBUG
			int g = _forwardshadow.cleanup() + _residualshadow.cleanup();
			if (g > 0) {
				System.out.println("CleanUp() found something!? gain = " + g);			
			}
			return g;
		} else { 		
		  return 0;
		}
	}

	@Override
	public int getFlowAt(int t) {
		if (t >= this._flow.size()) {
		  return 0;
		} else {
			return this._flow.get(t);
		}
	}

	@Override
	public int getLastTime() {
		return Math.max(this._forwardshadow.getLastTime(), this._residualshadow.getLastTime());
	}

	@Override
	public int getMeasure() {
		return _forwardshadow.getMeasure() + _residualshadow.getMeasure();
	}

	@Override
	public boolean isLast(Interval o) {
		// FIXME
		// this is a hack ...
		// It is not clear, whether this should be for forward or for residual shadow.
		// On the other hand, simply comparing pointers should mean that this is no problem.
		
		return _forwardshadow.isLast(o) || _residualshadow.isLast(o);
	}

	@Override
	public ArrayList<Interval> propagate(Interval incoming, boolean primal,
			boolean reverse, int timehorizon) {
		ArrayList<Interval> result = new ArrayList<Interval>();

		BinaryInterval current;
		Interval toinsert;
		
		// Handle all cases of primal and reverse in one unified matter.
		// One always just runs through the list of intervals anyway.
		// With the shadows, one only has to shift input and output appropriately.

		int inputoffset, outputoffset;
		if (!reverse) {
		  if (primal) {
			  inputoffset = 0;
			  outputoffset = this._traveltime;
		  } else {
			  inputoffset = -this._traveltime;
			  outputoffset = 0; // relative to "current", not to incoming
		  }
		} else { // reverse search
		  if (primal) {
			  inputoffset = -this._traveltime;
			  outputoffset = 0; // relative to "current", not to incoming
		  } else {
			  inputoffset =  0;
			  outputoffset = this._traveltime;
		  }		  
		}

		// switch which shadow we consider
		
		BinaryIntervals BI;
		if (primal) {
			BI = _forwardshadow;
		} else {
			BI = _residualshadow;
		}

		
		int low = -1;
		int high = -1;						
		boolean collecting = false;
		
		int effectiveStart = Math.max(incoming.getLowBound() + inputoffset, this._whenAvailable.getLowBound());
		int effectiveEnd = Math.min(incoming.getHighBound() + inputoffset, this._whenAvailable.getHighBound());

		BinTreeIterator<BinaryInterval> iter = new BinTreeIterator<BinaryInterval>(BI._tree, effectiveStart);
		//SkipListIterator<BinaryInterval> iter = new SkipListIterator<BinaryInterval>(BI, effectiveStart);
				
		current = iter.next();
		//current = this.getIntervalAt(effectiveStart);

		while (current.getLowBound() < effectiveEnd) {
			if (current.val) {				
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
					low += outputoffset;
					low = Math.max(low, 0);
					high = Math.min(high, effectiveEnd);
					high += outputoffset;
					high = Math.min(high, timehorizon);

					if (low < high) {
						toinsert = new Interval(low, high);					  
						result.add(toinsert);
					}
					collecting = false;
				}
			}

			/*if (this.isLast(current)) {
			break;
		} 
		current = this.getIntervalAt(current.getHighBound());*/


		if (!iter.hasNext()) break;
		current = iter.next();

		}

		if (collecting) { // finish the Interval
			low = Math.max(low, effectiveStart);
			low += outputoffset;
			low = Math.max(low, 0);
			high = Math.min(high, effectiveEnd);
			high += outputoffset;
			high = Math.min(high, timehorizon);

			if (low < high) {
				toinsert = new Interval(low, high);					  
				result.add(toinsert);
			}
			collecting = false;
		}
	
		return result;
	}
	
	@Override
	public String toString() {
		return "flow array:\n" + this._flow.toString() + "\n forward shadow\n" + this._forwardshadow.toString() + "\n backward shadow\n" + this._residualshadow.toString(); 
	}
	
	@Override
	@Deprecated
	public void augment(int tstart, int tstop, int gamma) {}


	@Override
	@Deprecated
	public void augmentUnsafe(int tstart, int tstop, int gamma) {}
}

