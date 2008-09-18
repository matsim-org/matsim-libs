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




/**
 * class representing the flow of an edge in a Time Expanded Network
 * @author manuelschneider
 *
 */
public class EdgeIntervalls {

//------------------------FIELDS----------------------------------//
	/**
	 * internal binary search tree holding distinkt Intervalls
	 */
	private AVLTree _tree;
	/**
	 * reference to the last Intervall
	 */
	private EdgeIntervall _last; 
	
	final int traveltime;
	
	
	
//-----------------------METHODS----------------------------------//
//****************************************************************//
	
	
//----------------------CONSTRUCTORS------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeIntervall [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public EdgeIntervalls(int traveltime){
		EdgeIntervall intervall = new EdgeIntervall(0,Integer.MAX_VALUE);
		_tree = new AVLTree();
		_tree.insert(intervall);
		_last = intervall;
		this.traveltime=traveltime;
	}

//------------------------SPLITTING--------------------------------//	
	
	/**
	 * Finds the EgdeIntervall containing t and splits this at t 
	 * giving it the same flow as the flow as the original 
	 * it inserts the new EdgeInterval after the original
	 * @param t time point to split at
	 * @return the new EdgeIntervall for further modification
 	 */
	public EdgeIntervall splitAt(int t){
		
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
		else throw new IllegalArgumentException("there is no Intervall that can be split at "+t);
	}

//------------------------------FLOW-------------------------//	
	
	/**
	 * Gives the Flow on the Edge at time t
	 * @param t time
	 * @return flow at t
	 */
	public int getFlowAt(int t){
		return getIntervallAt(t).getFlow();
	}
	

//------------------------------GETTER-----------------------//
	
	

	/**
	 * Finds the EdgeIntervall containing t in the collection
	 * @param t time
	 * @return  EdgeIntervall  containing t
	 */
	public EdgeIntervall getIntervallAt(int t){
		if(t<0){
			throw new IllegalArgumentException("negative time: "+ t);
		}
		EdgeIntervall i = (EdgeIntervall) _tree.contains(t);
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
			EdgeIntervall i= (EdgeIntervall) _tree._curr.obj;
			str.append(l+i.getLowBound()+";"+i.getHighBound()+r+" f:"+i.getFlow()+"\n");
		}
			
		return str.toString();
		
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
	public boolean isLast(EdgeIntervall o){
		return (_last.equals(o));
	}
	
	
	/**
	 * gives the next EdgeIntervall with respect ot the order contained 
	 * @param o schould be contained
	 * @return next EdgeIntervall iff o isnot last and contained
	 */
	public EdgeIntervall getNext(EdgeIntervall o){
		_tree.goToNodeAt(o.getLowBound());
		
			EdgeIntervall j = (EdgeIntervall) _tree._curr.obj;
			if(j.equals(o)){
				_tree.increment();
				if(!_tree.isAtEnd()){
					EdgeIntervall i = (EdgeIntervall) _tree._curr.obj;
					_tree.reset();
					return i;
				}else 	throw new IllegalArgumentException("Intervall was already last");
			}
			else throw new IllegalArgumentException("Intervall was not contained");
		
		
	}

	
	/**
	 * finds the next EdgeIntervall that has flow less than u after time t
	 * so that additional flow could be sent during the Intervall
	 * @param t time >=0 !!!
	 * @param u capacity
	 * @return EdgeIntervall[a,b] with f<u  and a>=t
	 */
	public EdgeIntervall minPossible(int t,int u){
		if (t<0){
			throw new IllegalArgumentException("time shold not be negative");
		}
		if (u<=0){
			throw new IllegalArgumentException("capacity shold be positive");
		}
		for(_tree.goToNodeAt(t);_tree.isAtEnd() ;_tree.increment()){
			System.out.println("f: " + ((EdgeIntervall)_tree._curr.obj).getFlow());
			if(((EdgeIntervall)_tree._curr.obj).getFlow()<u){
				return (EdgeIntervall)_tree._curr.obj;
			}
		}
		if(((EdgeIntervall)_tree._curr.obj).getFlow()<u){
			return (EdgeIntervall)_tree._curr.obj;
		}
		return null;
	}
	
	/**
	 * finds the next EdgeIntervall after time t-traveltime upon which f>0 
	 * so that flow could be sent over the Residual Edge starting at time t and arrive 
	 * during the returned Intervall  
	 * @param t time >= traveltime
	 * @return Edge Intervall [a,b] with f>0 a>=t-traveltime
	 */
	public EdgeIntervall minPossibleResidual(int t){
		t=t-traveltime;
		t=Math.max(0, t);
		for(_tree.goToNodeAt(t);_tree.isAtEnd() ;_tree.increment()){
			if(((EdgeIntervall)_tree._curr.obj).getFlow()>0){
				return ((EdgeIntervall)_tree._curr.obj);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param i
	 * @param u
	 * @param forward
	 * @return
	 */
	public ArrayList<Intervall> propagate(Intervall i, int u ,boolean forward){
		ArrayList<Intervall> result = new ArrayList<Intervall>();
		int t = i.getLowBound();
		int max =i.getHighBound();
		if(forward){
			while(t<max){
				
				Intervall j =minPossible(t,u);
				if(j==null){
					break;
				}
				t=j.getHighBound();
				System.out.println("kapazitaet frei");
				if( i.intersects(j)){
					j=i.Intersection(j).shiftPositive(traveltime);
					if(j!=null){
						result.add(i.Intersection(j));
					}	
				}	
			}//TODO implement backwards and comment
		}
		if(!forward){
			while(t<max){
				Intervall j =minPossibleResidual(t);
				if(j==null){
					break;
				}
				t=j.getHighBound();
				if( i.intersects(j)){
					j=i.Intersection(j);
					if(j!=null){
						result.add(i.Intersection(j));
					}	
				}	
			}
			
		}
		if(!result.isEmpty()){
			ArrayList<Intervall> temp= new ArrayList<Intervall>();
			Intervall part= result.get(0);
			for(int j=1;j< result.size(); j++){
				Intervall test = result.get(j);
				if(part.getHighBound()==test.getLowBound()){
					part.setHighBound(test.getHighBound());
				}else{
					temp.add(part);
					part=test;
				}
			}
			temp.add(part);
			result=temp; 
		}
		return result;
		//TODO unify different flow intervalls with positive capacity
	}
	
//------------------------Augmentation--------------------------------//
	
	/**
	 * increeases the flow into an edge from time t to t+1 by f if capacity is obeyed
	 * @param t raising time
	 * @param f aumount of flow to augment
	 * @param u capcity of the edge
	 */
	public void augment(int t, int f, int u){
		if (t<0){
			throw new IllegalArgumentException("negative time: "+ t);
				}
		EdgeIntervall i = getIntervallAt(t);
		if (i.getFlow()+f>u){
			throw new IllegalArgumentException("to much flow! flow: " + i.getFlow() + " + " + f + " > " + u);
		}
		if (i.getFlow()+f<0){
			throw new IllegalArgumentException("negative flow! flow: " + i.getFlow() + " + " + f + " < 0");
		}
		if(!(i.getLowBound()==t)){
			i= splitAt(t);
		}
		i.changeFlow(f, u);
		if(i.getHighBound() > (t+1)){
			i= splitAt(t+1);
			i.changeFlow((-f), u);
		}
	}
	/**
	 * dencreeases the flow into an edge from time t to t+1 by f if flow remains nonnegative
	 * @param t raising time
	 * @param f aumount of flow to reduce
	 */
	public void augmentrverse(int t, int f){
		if (t<0){
			throw new IllegalArgumentException("negative time : "+ t);
		}
		EdgeIntervall before =null;
		EdgeIntervall after =null;
		EdgeIntervall i= getIntervallAt(t);
		if(i.getLowBound()>0 ){
			before = getIntervallAt(i.getLowBound()-1);
		}
		if(i.getHighBound()< Integer.MAX_VALUE ){
			after= getIntervallAt(i.getHighBound());
		}
		if(f<0){
			throw new IllegalArgumentException("can not rduce flow by an negative amount without specified capacity");
		}
		int oldflow= i.getFlow();
		if(oldflow-f <0){
			throw new IllegalArgumentException("flow wold get negative");
		}
		i.setFlow(oldflow-f);
		if (before!=null){
			if(before.getFlow()== i.getFlow() ){
				_tree.remove(before);
				i.setLowBound(before.getLowBound());
			}
		}
		if(after!=null){
			if( after.getFlow() == i.getFlow() ){
				_tree.remove(i);
				after.setLowBound(i.getLowBound());
			}
		}
	}
	
//------------------------MAIN METHOD--------------------------------//
	
	

}
