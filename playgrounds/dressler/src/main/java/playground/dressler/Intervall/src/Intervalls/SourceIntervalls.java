/* *********************************************************************** *
 * project: org.matsim.*												   *
 * SourceIntervalls.java													   *
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

import java.util.ArrayList;

//java imports

//playground imports

/**
 * class representing the flow out of a source in the Time Expanded Network
 * @author Manuel Schneider, Daniel Dressler
 *
 */
public class SourceIntervalls {

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
	 * debug flag
	 */
	private static int _debug =0;
	
//********************************METHODS******************************************//
		
//------------------------------CONSTRUCTORS---------------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeIntervall [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public SourceIntervalls(){
		EdgeIntervall intervall = new EdgeIntervall(0,Integer.MAX_VALUE);
		_tree = new AVLTree();
		_tree.insert(intervall);
		_last = intervall;		
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
				"containing"+t);
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
		str.append("SourceIntervall ");
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
		SourceIntervalls._debug=debug;
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
	 * Checks whether flow coming out of the source can be sent back   
	 * @param incoming The interval on which one arrives.
	 * @return the first interval with flow on it (within incoming), or null 
	 */
	public Intervall canSendFlowBack(final Intervall incoming){

		EdgeIntervall current;
		
		current = this.getIntervallAt(incoming.getLowBound());
		while (current != null) {		
			if (current.getFlow() > 0) {				
				int low = Math.max(current.getLowBound(), incoming.getLowBound());					  
				int high = Math.min(current.getHighBound(), incoming.getHighBound());
				return new Intervall(low, high);

			}
			if (current.getHighBound() >= incoming.getHighBound()) {
				break;
			}
			current = this.getIntervallAt(current.getHighBound());
		}
			

		return null;
	}
	
	
//------------------------Clean Up--------------------------------//
	/**
	 * unifies adjacent EdgeIntervalls, call only when you feel it is safe to do
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = getLast().getHighBound();
		EdgeIntervall i, j;
		i = getIntervallAt(0);
		while (i != null) {
		  if (i.getHighBound() == timestop) break;	
		  j = this.getIntervallAt(i.getHighBound());
		  
		  if ((i.getHighBound() == j.getLowBound()) && 
				  (i.getFlow() == j.getFlow())) {
			  _tree.remove(i);
			  _tree.remove(j);
			  _tree.insert(new EdgeIntervall(i.getLowBound(), j.getHighBound(), i.getFlow()));
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
		EdgeIntervall i = getIntervallAt(t);
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
		i.changeFlow(f, u);
	}
	
	/**
	 * decreases the flow into an edge from time t to t+1 by f if flow remains nonnegative
	 * @param t raising time
	 * @param f amount of flow to reduce
	 */
	public void augmentreverse(final int t,final int f){
		if (t<0){
			throw new IllegalArgumentException("negative time : "+ t);
		}
		EdgeIntervall i= getIntervallAt(t);
		if(f<0){
			throw new IllegalArgumentException("can not rduce flow by an negative amount " +
					"without specified capacity");
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