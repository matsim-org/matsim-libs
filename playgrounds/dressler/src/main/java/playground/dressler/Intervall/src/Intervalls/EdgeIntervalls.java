/* *********************************************************************** *
 * project: org.matsim.*												   *
 * EdgeIntervalls.java													   *
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


package playground.dressler.Intervall.src.Intervalls;

//java imports
import java.util.ArrayList;

//playground imports
import playground.dressler.ea_flow.FlowCalculationSettings;

/**
 * class representing the flow of an edge in a Time Expanded Network
 * @author Manuel Schneider
 *
 */
public class EdgeIntervalls {

//**********************************FIELDS*****************************************//
	
	/**
	 * internal binary search tree holding distinkt Intervalls
	 */
	private AVLTree _tree;
	
	/**
	 * reference to the last Intervall
	 */
	private EdgeIntervall _last; 

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
	 * one EdgeIntervall [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public EdgeIntervalls(final int traveltime){
		EdgeIntervall intervall = new EdgeIntervall(0,Integer.MAX_VALUE);
		_tree = new AVLTree();
		_tree.insert(intervall);
		_last = intervall;
		this._traveltime=traveltime;
	}

//-----------------------------------SPLITTING-------------------------------------//	
	
	/**
	 * Finds the EgdeIntervall containing t and splits this at t 
	 * giving it the same flow as the flow as the original 
	 * it inserts the new EdgeInterval after the original
	 * @param t time point to split at
	 * @return the new EdgeIntervall for further modification
 	 */
	public EdgeIntervall splitAt(final int t){
		boolean found = false;
		EdgeIntervall j = null;
		EdgeIntervall i = getIntervallAt(t);
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
		else throw new IllegalArgumentException("there is no Intervall that " +
				"can be split at "+t);
	}

//--------------------------------------FLOW---------------------------------------//	
	
	/**
	 * Gives the Flow on the Edge at time t
	 * @param t time
	 * @return flow at t
	 */
	public int getFlowAt(final int t){
		return getIntervallAt(t).getFlow();
	}
	
//-------------------------------------GETTER--------------------------------------//

	/**
	 * Finds the EdgeIntervall containing t in the collection
	 * @param t time
	 * @return  EdgeIntervall  containing t
	 */
	public EdgeIntervall getIntervallAt(final int t){
		if(t<0){
			throw new IllegalArgumentException("negative time: "+ t);
		}
		EdgeIntervall i = (EdgeIntervall) _tree.contains(t);
		if(i==null)throw new IllegalArgumentException("there is no Intervall " +
				"containing "+t);
		return i;
	}

	/**
	 * Gives a String representation of all stored EdgeIntervalls
	 * @return String representation
	 */
	@Override
	public String toString(){
		String l,r;
		switch (Intervall.getMode()){
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
			EdgeIntervall i= (EdgeIntervall) _tree._curr.obj;
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
	 * gives the last Stored EdgeIntervall
	 * @return EdgeIntervall with maximal lowbound
	 */
	public EdgeIntervall getLast(){
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
	 * Checks weather the given EdgeIntervall is the last
	 * @param o EgeIntervall which it test for 
	 * @return true if getLast.equals(o)
	 */
	public boolean isLast(final EdgeIntervall o){
		return (_last.equals(o));
	}
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(final int debug){
		EdgeIntervalls._debug=debug;
	}
	
	/**
	 * gives the next EdgeIntervall with respect of the order contained 
	 * @param o schould be contained
	 * @return next EdgeIntervall iff o is not last and contained. if o is last,
	 *  null is returned.
	 */
	public EdgeIntervall getNext(final EdgeIntervall o){
		_tree.goToNodeAt(o.getLowBound());
		EdgeIntervall j = (EdgeIntervall) _tree._curr.obj;
		if(j.equals(o)){
			_tree.increment();
			if(!_tree.isAtEnd()){
				EdgeIntervall i = (EdgeIntervall) _tree._curr.obj;
				_tree.reset();
				return i;
			}else{ 	
				return null;
			}
		}
		else throw new IllegalArgumentException("Intervall was not contained");
	}

	
	
	/**
	 * finds the next EdgeIntervall that has flow less than u after time t
	 * so that additional flow could be sent during the Intervall
	 * @param earliestStartTimeAtFromNode time >=0 !!!
	 * @param capacity capacity
	 * @return EdgeIntervall[a,b] with f<u  and a>=t
	 */
	public EdgeIntervall minPossibleForwards(final int earliestStartTimeAtFromNode,
			final int capacity){
		if (earliestStartTimeAtFromNode<0){
			throw new IllegalArgumentException("time shold not be negative");
		}
		if (capacity<=0){
			throw new IllegalArgumentException("capacity shold be positive");
		}
		boolean wasAtEnd = false;
		//search for the next intervall, at which flow can be send!
		for(_tree.goToNodeAt(earliestStartTimeAtFromNode); !wasAtEnd; _tree.increment()){
			if(_debug>0){
				System.out.println("f: " +
						((EdgeIntervall)_tree._curr.obj).getFlow()+" on: "+
						((EdgeIntervall)_tree._curr.obj));
			}
			EdgeIntervall currentIntervall = (EdgeIntervall)_tree._curr.obj;
			if(currentIntervall.getFlow()<capacity){
				if(_debug>0){
					System.out.println("capacity left: " +
							(capacity-currentIntervall.getFlow()));
				}
				int earliestPossibleStart = Math.max(earliestStartTimeAtFromNode,
						currentIntervall.getLowBound());
				return new EdgeIntervall(earliestPossibleStart, 
						currentIntervall.getHighBound(), currentIntervall.getFlow());
			}
			//to iterate over the intervalls
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
	 * @param incoming Intervall where we can start
	 * @param capacity Capacity of the Link
	 * @param forward indicates whether we use residual edge or not 
	 * @param TimeHorizon for easy reference
	 * @return plain old Intervall
	 */
	public ArrayList<Intervall> propagate(final Intervall incoming,
			final int capacity ,final boolean forward, int timehorizon){

		ArrayList<Intervall> result = new ArrayList<Intervall>();

		EdgeIntervall current;
		Intervall toinsert;

		int low = -1;
		int high = -1;						
		boolean collecting = false;

		if(forward) {			
			current = this.getIntervallAt(incoming.getLowBound());
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
						  toinsert = new Intervall(low, high);					  
						  result.add(toinsert);
						}
						collecting = false;
					}
				}
				
				if (this.isLast(current)) {
					break;
				} 
				current = this.getIntervallAt(current.getHighBound());
			    				
			}

			if (collecting) { // finish the Interval
				low = Math.max(low, incoming.getLowBound());
				low += this._traveltime;
				high = Math.min(high, incoming.getHighBound());
				high += this._traveltime;
				high = Math.min(high, timehorizon);
				if (low < high) {
					toinsert = new Intervall(low, high);					  
					result.add(toinsert);
				}
				collecting = false;
			}
		} else { // not forward
			if (incoming.getLowBound() - this._traveltime < 0) {
				current = this.getIntervallAt(0);
			} else {
			  current = this.getIntervallAt(incoming.getLowBound() - this._traveltime);
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
						  toinsert = new Intervall(low, high);					  
						  result.add(toinsert);
						}
						collecting = false;
					}
				}
				
				if (this.isLast(current)) {
					break;
				}
				current = this.getIntervallAt(current.getHighBound());				
			} 

			if (collecting) { // finish the Interval
				low = Math.max(low, incoming.getLowBound() - this._traveltime);					  
				high = Math.min(high, incoming.getHighBound() - this._traveltime);
				high = Math.min(high, timehorizon);
				if (low < high) {
					toinsert = new Intervall(low, high);					  
					result.add(toinsert);
				}
				collecting = false;
			}

		}
		return result;
	}
	

//------------------------Clean Up--------------------------------//
	/**
	 * unifies adjacent EdgeIntervalls, call only when you feel it is safe to do
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = this._last.getHighBound();
		EdgeIntervall i, j;
		i = getIntervallAt(0);		
		while (i.getHighBound() < timestop) {		  
		  j = this.getIntervallAt(i.getHighBound());
		  if(i.getHighBound() != j.getLowBound())
			  throw new RuntimeException("error in cleanup!");  
		  if (i.getFlow() == j.getFlow()) {
			  _tree.remove(i);
			  _tree.remove(j);
   		      j = new EdgeIntervall(i.getLowBound(), j.getHighBound(), i.getFlow()); 			  
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
		EdgeIntervall i = getIntervallAt(t);
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
		EdgeIntervall i = getIntervallAt(t);
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
		EdgeIntervall i= getIntervallAt(t);
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
