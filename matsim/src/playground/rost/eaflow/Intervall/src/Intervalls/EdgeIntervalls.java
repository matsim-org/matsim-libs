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


package playground.rost.eaflow.Intervall.src.Intervalls;

import java.util.ArrayList;

import org.jfree.util.Log;

import playground.rost.eaflow.BowGraph.BowTravelTimeCost;




/**
 * class representing the flow of an edge in a Time Expanded Network
 * @author Manuel Schneider
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
	
	public final BowTravelTimeCost _traveltime;
	
	@SuppressWarnings("unused")
	private static boolean _debug =false;
	
	 
	
//-----------------------METHODS----------------------------------//
//****************************************************************//
	
	
//----------------------CONSTRUCTORS------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeIntervall [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public EdgeIntervalls(BowTravelTimeCost traveltime){
		EdgeIntervall intervall = new EdgeIntervall(0,Integer.MAX_VALUE);
		_tree = new AVLTree();
		_tree.insert(intervall);
		_last = intervall;
		this._traveltime=traveltime;
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
			str.append(l+i.getLowBound()+";"+i.getHighBound()+r+" f:"+i.getFlow()+" || ");
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
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(boolean debug){
		EdgeIntervalls._debug=debug;
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
	 * gives the previous EdgeIntervall with respect to the order contained 
	 * @param o should be contained
	 * @return next EdgeIntervall iff o isnot first and contained
	 */
	public EdgeIntervall getPrevious(EdgeIntervall o){
		if(o.getLowBound() == 0)
			return null;
		
		return this.getIntervallAt(o.getLowBound()-1);
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
		//TODO check whether this was the problem of negation in for
		for(_tree.goToNodeAt(t);!_tree.isAtEnd() ;_tree.increment()){
			if(_debug){
				System.out.println("f: " + ((EdgeIntervall)_tree._curr.obj).getFlow()+" on: "+((EdgeIntervall)_tree._curr.obj));
			}
			if(((EdgeIntervall)_tree._curr.obj).getFlow()<u){
				if(_debug){
					System.out.println("capacity left: " + (u-((EdgeIntervall)_tree._curr.obj).getFlow()));
				}
				return (EdgeIntervall)_tree._curr.obj;
			}
		}
		if(((EdgeIntervall)_tree._curr.obj).getFlow()<u){
			return (EdgeIntervall)_tree._curr.obj;
		}
		return null;
	}
	
	/**
	 * finds the next EdgeIntervall after time arrivalTime - maxTraveltime upon which f>0 
	 * so that flow could be sent over the Residual Edge starting at time t and arrive 
	 * during the returned Intervall  
	 * @param arrivalTime time >= traveltime
	 * @return Edge first Intervall [a,b] with f>0 a=arrivalTime-traveltime
	 */
	public EdgeIntervall minPossibleResidual(int arrivalTime){
		
		int earliestStartTime = arrivalTime - this._traveltime.getMaximalTravelTime();
		earliestStartTime = Math.max(0, earliestStartTime);
		for(_tree.goToNodeAt(earliestStartTime);!_tree.isAtEnd();_tree.increment())
		{
			EdgeIntervall eIntervall = (EdgeIntervall)_tree._curr.obj;
			int flow = this.getFlowAt(eIntervall.getLowBound());
			if(flow > 0)
			{
				int traveltime = this._traveltime.getCurrentTravelTimeForFlow(flow);
				if(eIntervall.getLowBound() + traveltime >= arrivalTime)
				{
					if(arrivalTime - traveltime >= eIntervall.getLowBound() && arrivalTime - traveltime >= 0)
					{
						EdgeIntervall result = new EdgeIntervall(arrivalTime - traveltime, eIntervall.getHighBound(), eIntervall.getFlow());
						return result;
					}
				}
			}			
		}
		return null;
	}
	
	public int getFirstPossibleBackwardEdgeUse(int arrivalTime)
	{
		EdgeIntervall earliestPossibleStart = this.minPossibleResidual(arrivalTime);
		int flow = earliestPossibleStart.getFlow();
		int traveltime = _traveltime.getCurrentTravelTimeForFlow(flow);
		if(earliestPossibleStart.getLowBound() + traveltime <= arrivalTime)
			return arrivalTime - _traveltime.getCurrentTravelTimeForFlow(flow);
		else
			return earliestPossibleStart.getLowBound() + traveltime;
	}
	
	/**
	 * TODO comment
	 * @param i
	 * @param u
	 * @param forward
	 * @return
	 */
	public ArrayList<VertexIntervall> propagate(Intervall i, int u ,boolean forward){
		ArrayList<VertexIntervall> result = new ArrayList<VertexIntervall>();
		int t = i.getLowBound();
		int max =i.getHighBound();
		VertexIntervall foundIntervall;
		if(forward){
			while(t<max){
				
				Intervall j =this.minPossible(t,u);
				if(j==null){
					if(_debug){
						System.out.println("no possible interval after:" + t+ "with cap: " +u);
					}
					break;
				}
				t=j.getHighBound();
				if(_debug){
					System.out.println("kapazitaet frei");
					System.out.println("old i: " +i);
					System.out.println("old j: " +j);
				}
				if( i.intersects(j)){
					//TODO test intensively
					int currentFlow = this.getFlowAt(j.getLowBound());
					//TODO change interface, so +1 wont be necessary
					Integer travelTime = (int)this._traveltime.getTravelTimeForAdditionalFlow(currentFlow);
					if(!_debug){
						j=i.Intersection(j).shiftPositive(travelTime);
					}else{
						j=i.Intersection(j);
						System.out.println("i intersection j:" + j);
						j=j.shiftPositive(travelTime);
						System.out.println("shifted by: " + _traveltime+ " -> " +  j);
					}
					if(j!=null){
						if(_debug){
							System.out.println("new i: " +i);
							System.out.println("new j: " +j);
							System.out.println("tau:" +this._traveltime);
						}
						foundIntervall = new VertexIntervall(j);
						foundIntervall.setStartTime(Math.min(i.getHighBound()-1, j.getLowBound()-travelTime));
						result.add(foundIntervall);
					}	
				}	
			}//TODO comment
		}
		if(!forward){
			Intervall j =minPossibleResidual(t);
				
			if(j!=null){
				System.out.println("watch out!");
				System.out.println("i: " + i);
				System.out.println("j: " + j);
				foundIntervall = new VertexIntervall(j);
				foundIntervall.setStartTime(i.getLowBound());
				result.add(foundIntervall);
			}	
		}
		//TODO repair and debug!!!!!
		/*//unify different flow intervalls with positive capacity
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
		}*/
		return result;
		
	}

//------------------------Clean Up--------------------------------//
	/**
	 * unifies adjacent intervalls, call only when you feel it is safe to do
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = getLast().getHighBound();
		EdgeIntervall i, j;
		i = getIntervallAt(0);
		while (i != null) {
		  if (i.getHighBound() == timestop) break;	
		  j = getNext(i);
		  
		  if ((i.getHighBound() == j.getLowBound()) && 
				  (i.getFlow() == j.getFlow())) {
			  EdgeIntervall ni = new EdgeIntervall(i.getLowBound(),j.getHighBound(),i.getFlow());
			  _tree.remove(i);
			  _tree.remove(j);
			  _tree.insert(ni);
			  i = ni;
			  gain++;

		  } else {
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
	public void augmentreverse(int t, int f){
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
			throw new IllegalArgumentException("flow would get negative");
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
	
	public int getCurrentForwardTravelTime(int time)
	{
		int flow = this.getFlowAt(time);
		return getTravelTimeByFlow(flow);
	}
	
	public int getTravelTimeByFlow(int flow)
	{
		return (int)this._traveltime.getCurrentTravelTimeForFlow(flow);
	}
	
	public int getTravelTimeForAdditionalFlow(int currentFlow)
	{
		return (int)this._traveltime.getTravelTimeForAdditionalFlow(currentFlow);
	}
	
	public int getTravelTimeForAdditionalFlowByTime(int time)
	{
		int flow = this.getFlowAt(time);
		return getTravelTimeForAdditionalFlow(flow);
	}
	
//------------------------MAIN METHOD--------------------------------//
	//TODO ROST CANT WORK THIS WAY!
//	public static void main(String[] args){
//		EdgeIntervalls.debug(true);
//		EdgeIntervalls test = new EdgeIntervalls(1);
//		test.augment(1, 1, 1);
//		test.augment(3, 1, 1);
//		Intervall i = new Intervall(1,Integer.MAX_VALUE);
//		ArrayList<Intervall> result = test.propagate(i, 1, true);
//		System.out.println("empty:  " +result.isEmpty());
//		if(!result.isEmpty()){
//			for (Intervall j :result){
//				System.out.println(j);
//			}
//		}
//	}
	
	public int getLatestPossibleStartTime(int arrivalTime)
	{
		int bestPossibleStartTime = arrivalTime - _traveltime.getMinimalTravelTime();
		EdgeIntervall eIntervall = this.getIntervallAt(bestPossibleStartTime);
		for(;;)
		{
			int flow = eIntervall.getFlow();
			Integer traveltime = _traveltime.getTravelTimeForAdditionalFlow(flow);
			
			if(traveltime != null && arrivalTime - traveltime >= eIntervall.getLowBound())
				return arrivalTime - traveltime;
			eIntervall = this.getPrevious(eIntervall);
		}
	}
	
	public int getRemainingBackwardCapacityWithThisTravelTime(int arrivalTime)
	{
		int flow = this.getFlowAt(arrivalTime);
		return _traveltime.getRemainingBackwardCapacityWithThisTravelTime(flow);
	}
	

}
