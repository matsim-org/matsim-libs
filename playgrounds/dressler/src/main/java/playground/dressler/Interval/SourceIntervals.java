/* *********************************************************************** *
 * project: org.matsim.*												   *
 * SourceIntervals.java													   *
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

import java.util.ArrayList;

//java imports

//playground imports

/**
 * class representing the flow out of a source in the Time Expanded Network
 * @author Manuel Schneider, Daniel Dressler
 *
 */
public class SourceIntervals {

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
	 * debug flag
	 */
	private static int _debug =0;
	
//********************************METHODS******************************************//
		
//------------------------------CONSTRUCTORS---------------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeInterval [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public SourceIntervals(){
		EdgeInterval interval = new EdgeInterval(0,Integer.MAX_VALUE);
		_tree = new AVLTree();
		_tree.insert(interval);
		_last = interval;		
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
				"containing"+t);
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
		str.append("SourceInterval ");
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
		SourceIntervals._debug=debug;
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
	 * Checks whether flow coming out of the source can be sent back   
	 * @param incoming The interval on which one arrives.
	 * @return the first interval with flow on it (within incoming), or null 
	 */
	public Interval canSendFlowBack(final Interval incoming){

		EdgeInterval current;
		
		current = this.getIntervalAt(incoming.getLowBound());
		while (current != null) {		
			if (current.getFlow() > 0) {				
				int low = Math.max(current.getLowBound(), incoming.getLowBound());					  
				int high = Math.min(current.getHighBound(), incoming.getHighBound());
				return new Interval(low, high);

			}
			if (current.getHighBound() >= incoming.getHighBound()) {
				break;
			}
			current = this.getIntervalAt(current.getHighBound());
		}
			

		return null;
	}
	
	
//------------------------Clean Up--------------------------------//
	/**
	 * unifies adjacent EdgeIntervals, call only when you feel it is safe to do
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = getLast().getHighBound();
		EdgeInterval i, j;
		i = getIntervalAt(0);
		while (i != null) {
		  if (i.getHighBound() == timestop) break;	
		  j = this.getIntervalAt(i.getHighBound());
		  
		  if ((i.getHighBound() == j.getLowBound()) && 
				  (i.getFlow() == j.getFlow())) {
			  _tree.remove(i);
			  _tree.remove(j);
			  _tree.insert(new EdgeInterval(i.getLowBound(), j.getHighBound(), i.getFlow()));
			  gain++;
		  }else{
			  i = j;
		  }		 		 
		}
		return gain;
	}
	
//------------------------Augmentation--------------------------------//
	
	/**
	 * increeases the flow into an edge from time t to t+1 by f if capacity is obeyed
	 * @param t raising time
	 * @param f aumount of flow to augment
	 * @param u capcity of the edge
	 */
	public void augment(final int t,final int f,final int u){
		if (t<0){
			throw new IllegalArgumentException("negative time: "+ t);
				}
		EdgeInterval i = getIntervalAt(t);
		if (i.getFlow()+f>u){
			throw new IllegalArgumentException("too much flow! flow: " + i.getFlow() + " + " +
					f + " > " + u);
		}

		if (i.getFlow()+f<0){
			throw new IllegalArgumentException("negative flow! flow: " + i.getFlow() + " + " +
					f + " < 0");
		}
		if(i.getLowBound() < t){
			i= splitAt(t);
		}
		if(i.getHighBound() > (t+1)){
			splitAt(t+1);
		}
		i.augment(f, u);
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

}