/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeIntervalls.java
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

import org.matsim.network.Link;




/**
 * class representing the flow of an edge in a Time Expanded Network
 * @author manuelschneider
 *
 */
public class VertexIntervalls {

//------------------------FIELDS----------------------------------//
	/**
	 * internal binary search tree holding distinkt Intervalls
	 */
	private AVLTree _tree;
	/**
	 * reference to the last Intervall
	 */
	private VertexIntervall _last; 
	
	private int _demand=0;
	
	private boolean _source=false;
	
	
	
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
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeIntervall [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public VertexIntervalls(int demand, boolean source){
		VertexIntervall intervall = new VertexIntervall(0,Integer.MAX_VALUE);
		_tree = new AVLTree();
		_tree.insert(intervall);
		_last = intervall;
		_demand=demand;
		_source=source;
	}

//------------------------SPLITTING--------------------------------//	
	
	/**
	 * Finds the EgdeIntervall containing t and splits this at t 
	 * giving it the same flow as the flow as the original 
	 * it inserts the new EdgeInterval after the original
	 * @param t time point to split at
	 * @return the new EdgeIntervall for further modification
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
	 * Gives the predesessing Link on the Vertex at time t
	 * @param t time
	 * @return flow at t
	 */
	public Link getPred(int t){
		return getIntervallAt(t).getPredecessor();
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
		if(i==null)throw new IllegalArgumentException("there is no Intervall containing"+t);
		return i;
	}
	
	
	/**
	 * Geves a String representation of all stored Intervalls linewise
	 * @return String representation
	 */
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
			str.append(l+i.getLowBound()+";"+i.getHighBound()+r+" d:"+i.getDist()+"\n");
		}
			
		return str.toString();
		
	}
	
	
	/**
	 * gives the last Stored VertexIntervall
	 * @return VertexIntervall with maximal lowbound
	 */
	public VertexIntervall getLast(){
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
	 * Checks weather the given VertexIntervall is the last
	 * @param o EgeIntervall which it test for 
	 * @return true if getLast.equals(o)
	 */
	public boolean isLast(VertexIntervall o){
		return (_last.equals(o));
	}
	
	
	/**
	 * gives the next VertexIntervall with respect ot the order contained 
	 * @param o schould be contained
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
	 * finds the first intervall within which
	 *  the node is reachable from the source
	 * @return specified Intervall or null if none exist
	 */
	private VertexIntervall getFirstPossible(){
		VertexIntervall result = this.getIntervallAt(0);
		while(!this.isLast(result)){
			if (result.getDist()){
				return result;
			}else{
				result=this.getNext(result);
			}
		}
		if (result.getDist()){
			return result;
		}	
		return null;
	}
	
	public int firstPossibleTime(){
		VertexIntervall test =this.getFirstPossible();
		if(test!=null){
			return test.getLowBound();
		}else{
			return Integer.MAX_VALUE;
		}
		
	}
	
	/**
	 * 
	 * @param arrive
	 * @return
	 */
	public boolean setTrue(ArrayList<Intervall> arrive,Link link) {
		boolean changed = false;
		for(int i=0; i< arrive.size(); i++){
			boolean temp=setTrue(arrive.get(i),link);
			if(temp){
				changed=true;
			}
		}
		return changed;
	}
	//TODO comments
	/**
	 * 
	 * @param arrive
	 * @param link
	 * @return
	 */
	public boolean setTrue(Intervall arrive,Link link){
		boolean changed = false;
		VertexIntervall test = this.getIntervallAt(arrive.getLowBound());
		int t= test.getHighBound();
		while(test.getLowBound()<arrive.getHighBound()){
			t=test.getHighBound();
			if(!test.getDist()){
				// just relabel since it is contained
				if(arrive.contains(test)){
					test.setDist(true);
					test.setPredecessor(link);
					changed=true;
				}else{
					//upper part of test must be relabeld
					if(test.getLowBound()<arrive.getLowBound()&& test.getHighBound()<=arrive.getHighBound()){
						VertexIntervall temp= this.splitAt(arrive.getLowBound());
						temp.setDist(true);
						temp.setPredecessor(link);
						changed=true;
					}else{
						//lower part of test must be relabeld
						if(test.getLowBound()>=arrive.getLowBound()&& test.getHighBound()>arrive.getHighBound()){
							int temptime=test.getLowBound();
							this.splitAt(arrive.getHighBound());
							VertexIntervall temp= this.getIntervallAt(temptime);
							temp.setDist(true);
							temp.setPredecessor(link);
							changed=true;
							
						}else{
							//middle of tet must be relabeld
							if(test.contains(arrive)){
								int temptime = arrive.getLowBound();
								this.splitAt(arrive.getLowBound());
								this.splitAt(arrive.getHighBound());
								VertexIntervall temp= this.getIntervallAt(temptime);
								temp.setDist(true);
								temp.setPredecessor(link);
								changed=true;
							}
						}
					}
					
				}
				
			}
			//pick next Intervall
			test= this.getIntervallAt(t);
		}	
		return changed;
	}
	

	
	/**
	 * finds the next VertexIntervall that has flow less than u after time t
	 * @param t time
	 * @param u capacity
	 * @return
	 */
	/**
	public VertexIntervall minPossible(int t,int u){
		if (t<0){
			throw new IllegalArgumentException("time shold not be negative");
		}
		if (u<=0){
			throw new IllegalArgumentException("capacity shold be positive");
		}
		for(_tree.goToNodeAt(t);_tree.isAtEnd() ;_tree.increment()){
			if(((VertexIntervall)_tree._curr.obj).getFlow()<u){
				return (VertexIntervall)_tree._curr.obj;
			}
		}
		return null;
	}
	**/
	

	
//------------------------MAIN METHOD--------------------------------//
	
	

}
