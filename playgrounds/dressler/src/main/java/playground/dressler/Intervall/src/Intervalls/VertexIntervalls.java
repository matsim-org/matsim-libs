/* *********************************************************************** *
 * project: org.matsim.*												   *
 * VertexIntervalls.java												   *
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
package playground.dressler.Intervall.src.Intervalls;

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
public class VertexIntervalls {

//------------------------FIELDS----------------------------------//
	
	/**
	 * internal binary search tree holding distinct VertexIntervall instances
	 */
	private AVLTree _tree;
	
	/**
	 * reference to the last VertexIntervall
	 */
	private VertexIntervall _last; 
	
	/**
	 * flag for debug mode
	 */
	@SuppressWarnings("unused")
	private static boolean _debug = false;
	
	
//-----------------------METHODS----------------------------------//
//****************************************************************//
	
	 
//----------------------CONSTRUCTORS------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeIntervall [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public VertexIntervalls(){
		VertexIntervall intervall = new VertexIntervall(0,Integer.MAX_VALUE);
		_tree = new AVLTree();
		_tree.insert(intervall);
		_last = intervall;
	}
	
//------------------------SPLITTING--------------------------------//	
	
	/**
	 * Finds the VertexIntervall containing t and splits this at t 
	 * giving it the same flow as the flow as the original 
	 * it inserts the new VertexInterval after the original
	 * @param t time point to split at
	 * @return the new VertexIntervall for further modification
 	 */
	public VertexIntervall splitAt(int t){
		boolean found = false;
		VertexIntervall j = null;
		VertexIntervall i = getIntervallAt(t);
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
		else throw new IllegalArgumentException("there is no Intervall that can be split at "+t);
	}

//------------------------------FLOW-------------------------//	
	
	/**
	 * Gives the predecessor Link on the Vertex at time t
	 * @param t time
	 * @return flow at t
	 */
	public PathStep getPred(int t){
		return getIntervallAt(t).getPredecessor().copyShiftedToArrival(t);
	}
	

//------------------------------GETTER-----------------------//
	
	
	/**
	 * Finds the VertexIntervall containing t in the collection
	 * @param t time
	 * @return  VertexIntervall  containing t
	 */
	public VertexIntervall getIntervallAt(int t){
		if(t<0){
			throw new IllegalArgumentException("negative time: "+ t);
		}
		VertexIntervall i = (VertexIntervall) _tree.contains(t);
		if(i==null)throw new IllegalArgumentException("there is no Intervall containing "+t);
		return i;
	}
	
	/**
	 * Gives a String representation of all stored VertexIntervall Instances line by line
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
			VertexIntervall i= (VertexIntervall) _tree._curr.obj;
			str.append(l+i.getLowBound()+";"+i.getHighBound()+r+" d:"+i.getReachable()+ " scanned: " + i.isScanned() + " pred:"+i.getPredecessor()+"\n");			
		}
		return str.toString();
	}
	
	
	/**
	 * Gives the last stored VertexIntervall
	 * @return VertexIntervall with maximal lowbound
	 */
	public VertexIntervall getLast(){
		return _last;
	}
	
	
	/**
	 * checks whether last is referenced right
	 * @return true iff everything is OK
	 */
	public boolean checkLast(){
		return _last==_tree._getLast().obj;
	}
	
	/**
	 * Checks whether the given VertexIntervall is the last
	 * @param o EgeIntervall which it test for 
	 * @return true if getLast.equals(o)
	 */
	public boolean isLast(VertexIntervall o){
		return (_last.equals(o));
	}
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(boolean debug){
		VertexIntervalls._debug=debug;
	}
	
	/**
	 * gives the next VertexIntervall with respect to the order contained 
	 * @param o should be contained
	 * @return next VertexIntervall iff o is not last and contained
	 */
	public VertexIntervall getNext(VertexIntervall o){
		_tree.goToNodeAt(o.getLowBound());
		VertexIntervall j = (VertexIntervall) _tree._curr.obj;
		if(j.equals(o)){
			_tree.increment();
			if(!_tree.isAtEnd()){
				VertexIntervall i = (VertexIntervall) _tree._curr.obj;
				_tree.reset();
				return i;
			}else 	throw new IllegalArgumentException("Intervall was already last");
		}
		else throw new IllegalArgumentException("Intervall was not contained");
	}
	
	/**
	 * finds the first VertexIntervall within which
	 *  the node is reachable from the source
	 * @return specified VertexIntervall or null if none exist
	 */
	public VertexIntervall getFirstPossible(){
		VertexIntervall result = this.getIntervallAt(0);
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
		VertexIntervall test =this.getFirstPossible();
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
	 * @param arrive VertexIntervalls at which node is reachable
	 * @param pred Predecessor PathStep. It will always be shifted to the beginning of the intervall
	 * @return true iff anything was changed
	 */
	public boolean setTrue(ArrayList<Intervall> arrive, PathStep pred) {
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
	 * @param arrive Intervall at which node is reachable
	 * @param pred Predecessor PathStep. It will always be shifted to the beginning of the intervall
	 * @return true iff anything was changed
	 */
	public boolean setTrue(Intervall arrive, PathStep pred){
		// slightly slower, but easier to manage if this just calls the new setTrueList
		ArrayList<VertexIntervall> temp = setTrueList(arrive, pred);
		return (temp != null && !temp.isEmpty());
		
		/*boolean changed = false;
		VertexIntervall current = this.getIntervallAt(arrive.getLowBound());
		int t = current.getHighBound();
		while(current.getLowBound() < arrive.getHighBound()){
			//either ourIntervall was never reachable before and is not scanned
			if(!current.getReachable() && !current.isScanned())
			{
				//test if the intervalls intersect at all (using the condition in while head above)
				if(arrive.getLowBound() >= current.getLowBound()
						|| arrive.getHighBound() > current.getLowBound())
				{
					//if arrive contains ourIntervall, we relabel it completely
					if(arrive.contains(current))
					{
						current.setArrivalAttributes(pred);						
						changed = true;
					}
					else if(current.contains(arrive))
					{
						//if arrive is contained..
						//we adapt our intervall, so that our lowbound equals
						//the low bound of the arrive intervall..
						if(current.getLowBound() < arrive.getLowBound())
						{
							current = this.splitAt(arrive.getLowBound());
						}
						//or we set our highbound to the highbound of arrival
						if(current.getHighBound() > arrive.getHighBound())
						{
							this.splitAt(arrive.getHighBound());
							current = this.getIntervallAt(arrive.getHighBound()-1);
						}
						//ourintervall has exactly the same bounds as arrive
						//so relabel it completely
						current.setArrivalAttributes(pred);						
						changed = true;
					}
					else
					{
						//ourIntervall intersects arrive, but is neither contained nor does it contain
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
							current = this.getIntervallAt(arrive.getHighBound()-1);
						}
						//we set the attributes
						current.setArrivalAttributes(pred);
						changed = true;
					}
				}
			}
			t = current.getHighBound();
			//pick next Intervall
			if(Integer.MAX_VALUE==t){
				break;
			}
			current= this.getIntervallAt(t);
		}	
		return changed;*/
	}
	
	/**
	 * Sets arrival true for all intervals in arrive and sets predecessor to link for each time t
	 * where it was null beforehand
	 * @param arrive Intervalls at which node is reachable
	 * @param pred Predecessor PathStep. It will always be shifted to the beginning of the intervall
	 * @return null or list of changed intervals iff anything was changed
	 */
    public ArrayList<VertexIntervall> setTrueList(ArrayList<Intervall> arrive, PathStep pred) {
		
		if (arrive == null || arrive.isEmpty()) { return null; }
				
		ArrayList<VertexIntervall> changed = new ArrayList<VertexIntervall>();
		
		// there used to be condensing here ...
		// but propagate already condenses these days
		
		Iterator<Intervall> iterator = arrive.iterator();
		Intervall i;
								
		while(iterator.hasNext()) {
			i = iterator.next();	        
		    changed.addAll(setTrueList(i, pred));
		}
				
		return changed;
	}

    /**
	 * Sets arrival true for all time steps in arrive and sets predecessor to link for each time t
	 * where it was null beforehand
	 * @param arrive Intervall at which node is reachable
	 * @return null or list of changed intervals iff anything was changed
	 */
	public ArrayList<VertexIntervall> setTrueList(Intervall arrive, PathStep pred){
		// TODO Test !
		ArrayList<VertexIntervall> changed = new ArrayList<VertexIntervall>();		
		VertexIntervall current = this.getIntervallAt(arrive.getLowBound());
		int t = current.getHighBound();
		while(current.getLowBound() < arrive.getHighBound()){
			//current was never reachable before and is not scanned
			if(!current.getReachable() && !current.isScanned())
			{
				//test if the intervalls intersect at all (using the condition in while head above)
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
						//the low bound of the arrive intervall..
						if(current.getLowBound() < arrive.getLowBound())
						{
							current = this.splitAt(arrive.getLowBound());
						}
						//or we set our highbound to the highbound of arrival
						if(current.getHighBound() > arrive.getHighBound())
						{
							this.splitAt(arrive.getHighBound());
							current = this.getIntervallAt(arrive.getHighBound()-1);
						}
						//current has exactly the same bounds as arrive
						//so relabel it completely
						current.setArrivalAttributes(pred);						
						changed.add(current);
					}
					else
					{
						//ourIntervall intersects arrive, but is neither contained nor does it contain
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
							current = this.getIntervallAt(arrive.getHighBound()-1);
						}
						//we set the attributes
						current.setArrivalAttributes(pred);
						changed.add(current);
					}
				}
			}
			t = current.getHighBound();
			//pick next Intervall
			// TODO isLast() is better! 
			if(Integer.MAX_VALUE==t){
				break;
			}
			current= this.getIntervallAt(t);
		}	
		return changed;
	}
	
	
	
//------------------------Clean Up--------------------------------//

	/**
	 * unifies adjacent intervalls, call only when you feel it is safe to do
	 * @return number of unified VertexIntervalls
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = getLast().getHighBound();		
	    
		//System.out.println("VertexIntervalls.cleanup()");
		//System.out.println(this.toString());
		
		VertexIntervall i, j;
		i = getIntervallAt(0);		
		while (i.getHighBound() < timestop) {		  
		  j = this.getIntervallAt(i.getHighBound());
		  if(i.getHighBound() != j.getLowBound())
			  throw new RuntimeException("error in cleanup!");
		  if (i.getReachable() == j.getReachable() 
				  && i.isScanned() == j.isScanned()
				  && i.getPredecessor().equals(j.getPredecessor())) {
			  _tree.remove(i);
			  _tree.remove(j);
			  j = new VertexIntervall(i.getLowBound(), j.getHighBound(), i);
			  _tree.insert(j);			  
			  gain++;
		  } 
		  i = j;
		}
		this._last = i;
		
		return gain;
	}
	
	/**
	 * Gives the first reachable but unscanned VertexIntervall 
	 * @return the VertexIntervall or null if it does not exist
	 */
	public VertexIntervall getFirstUnscannedIntervall()
	{
		int lowBound = 0;
		while(lowBound < Integer.MAX_VALUE)
		{
			VertexIntervall vI = this.getIntervallAt(lowBound);
			if(vI.getReachable() &&  !vI.isScanned())
				return vI;
			lowBound = vI.getHighBound();
		}
		return null;
	}
	
	/**
	 * Returns the lowbound of the first unscanned but reachable VertexIntervall
	 * @return the Value of the lowbound or null if it does not exist
	 */
	public Integer getFirstTimePointWithDistTrue()
	{
		VertexIntervall vIntervall = this.getFirstUnscannedIntervall();
		if(vIntervall == null)
			return null;
		else
			return vIntervall.getLowBound();
	}
}
