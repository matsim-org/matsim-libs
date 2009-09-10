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

import playground.rost.eaflow.ea_flow.FlowEdgeTraversalCalculator;
import playground.rost.eaflow.ea_flow.GlobalFlowCalculationSettings;




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
	
	public final FlowEdgeTraversalCalculator _traveltime;
	
	@SuppressWarnings("unused")
	private static boolean _debug =false;
	
	 
	
//-----------------------METHODS----------------------------------//
//****************************************************************//
	
	
//----------------------CONSTRUCTORS------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeIntervall [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public EdgeIntervalls(FlowEdgeTraversalCalculator traveltime){
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
	 * gives the next EdgeIntervall with respect of the order contained 
	 * @param o schould be contained
	 * @return next EdgeIntervall iff o is not last and contained. if o is last, null is returned.
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
				}
				else 	
					return null;
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
	 * @param earliestStartTimeAtFromNode time >=0 !!!
	 * @param capacity capacity
	 * @return EdgeIntervall[a,b] with f<u  and a>=t
	 */
	public EdgeIntervall minPossibleForwards(int earliestStartTimeAtFromNode, int capacity){
		if (earliestStartTimeAtFromNode<0){
			throw new IllegalArgumentException("time shold not be negative");
		}
		if (capacity<=0){
			throw new IllegalArgumentException("capacity shold be positive");
		}
		boolean wasAtEnd = false;
		//search for the next intervall, in which flow can be send!
		for(_tree.goToNodeAt(earliestStartTimeAtFromNode); !wasAtEnd;_tree.increment()){
			if(_debug){
				System.out.println("f: " + ((EdgeIntervall)_tree._curr.obj).getFlow()+" on: "+((EdgeIntervall)_tree._curr.obj));
			}
			EdgeIntervall currentIntervall = (EdgeIntervall)_tree._curr.obj;
			if(currentIntervall.getFlow()<capacity){
				if(_debug){
					System.out.println("capacity left: " + (capacity-currentIntervall.getFlow()));
				}
				return new EdgeIntervall(Math.max(earliestStartTimeAtFromNode, currentIntervall.getLowBound()), currentIntervall.getHighBound(), currentIntervall.getFlow());
			}
			if(_tree.isAtEnd())
				wasAtEnd = true;
		}
		return null;
	}
	
	public EdgeIntervall forbidHoldoverForwards(EdgeIntervall eIntervall, int latestStartTimeAtFromNode){
		if(eIntervall == null)
			return null;
		if(latestStartTimeAtFromNode >= eIntervall.getLowBound())
		{
			return new EdgeIntervall(eIntervall.getLowBound(), Math.min(latestStartTimeAtFromNode+1, eIntervall.getHighBound()), eIntervall.getFlow());
		}
		return null;
	}
	
	/**
	 * finds the next EdgeIntervall after time arrivalTime - maxTraveltime upon which f>0 
	 * so that flow could be sent over the Residual Edge starting at time t and arrive 
	 * during the returned Intervall  
	 * @param earliestStartTimeAtFromNode time >= traveltime
	 * @return Edge first Intervall [a,b] with f>0 a=arrivalTime-traveltime
	 */
	public EdgeIntervall minPossibleBackwards(int earliestStartTimeAtFromNode){
		
		int earliestStartTimeAtToNode = earliestStartTimeAtFromNode - this._traveltime.getMaximalTravelTime();
		earliestStartTimeAtToNode = Math.max(0, earliestStartTimeAtToNode);
		boolean wasAtEnd = false;
		for(_tree.goToNodeAt(earliestStartTimeAtToNode); _tree._curr.obj != null ;_tree.increment())
		{
			EdgeIntervall eIntervall = (EdgeIntervall)_tree._curr.obj;
			int flow = this.getFlowAt(eIntervall.getLowBound());
			//we need flow to reverse it!
			if(flow > 0)
			{
				int traveltime = this._traveltime.getTravelTimeForFlow(flow);
				//we have to arrive after or at the arrivalTime
				if(eIntervall.getLowBound() + traveltime >= earliestStartTimeAtFromNode)
				{
					//the flow can be send after the earliestStartTime from the from node of the intervall with the flow 	
					if(earliestStartTimeAtFromNode - traveltime >= eIntervall.getLowBound())
					{
						return new EdgeIntervall(earliestStartTimeAtFromNode-traveltime, eIntervall.getHighBound(), eIntervall.getFlow());
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * finds the next EdgeIntervall after time arrivalTime - maxTraveltime upon which f>0 
	 * so that flow could be sent over the Residual Edge starting at time t and arrive 
	 * during the returned Intervall  
	 * @param earliestStartTimeAtFromNode time >= traveltime
	 * @return Edge first Intervall [a,b] with f>0 a=arrivalTime-traveltime
	 */
	public EdgeIntervall forbidHoldoverBackward(EdgeIntervall eIntervall, int latestStartTimeAtFromNode)
	{
		if(eIntervall == null)
			return null;
		int traveltime = _traveltime.getTravelTimeForFlow(eIntervall.getFlow());
		//the flow has to be negated before the latest possible arrival time at 
		//the from node
		if(latestStartTimeAtFromNode - traveltime <= eIntervall.getHighBound())
			return new EdgeIntervall(eIntervall.getLowBound(), latestStartTimeAtFromNode-traveltime+1, eIntervall.getFlow());
		return null;
		
	}
		
	/**
	 * TODO comment
	 * @param arrivaleIntervall
	 * @param u
	 * @param forward
	 * @return
	 */
	public ArrayList<VertexIntervall> propagate(Intervall arrivaleIntervall, int u ,boolean forward){
		ArrayList<VertexIntervall> result = new ArrayList<VertexIntervall>();
		int earliestStartPossible = arrivaleIntervall.getLowBound();
		//-1 is important!
		int latestStartPossible =arrivaleIntervall.getHighBound() - 1;
		
		VertexIntervall foundIntervall;
		EdgeIntervall currentIntervall = this.getIntervallAt(earliestStartPossible);
		if(forward){
			//iterate over our edge intervalls as long as the intervall
			//	is not null
			while(currentIntervall != null){
				
				EdgeIntervall nextPossibleIntervall;
				
				nextPossibleIntervall = this.minPossibleForwards(earliestStartPossible,u);
				if(nextPossibleIntervall==null){
					if(_debug){
						System.out.println("no possible interval after:" + earliestStartPossible+ "with cap: " +u);
					}
					break;
				}
				
				if(!GlobalFlowCalculationSettings.useHoldover)
					nextPossibleIntervall = this.forbidHoldoverForwards(nextPossibleIntervall, latestStartPossible);
				
				if(_debug){
					System.out.println("kapazitaet frei");
					System.out.println("old i: " +arrivaleIntervall);
					System.out.println("old j: " +nextPossibleIntervall);
				}
				
				
				
				if(nextPossibleIntervall!=null)
				{
					int currentFlow = nextPossibleIntervall.getFlow();
					Integer travelTime = this._traveltime.getTravelTimeForAdditionalFlow(currentFlow);
					nextPossibleIntervall = nextPossibleIntervall.shiftPositive(travelTime);
					if(_debug)
					{
						System.out.println("shifted by: " + travelTime+ " -> " +  nextPossibleIntervall);
						System.out.println("new i: " +arrivaleIntervall);
						System.out.println("new j: " +nextPossibleIntervall);
						System.out.println("tau:" +this._traveltime);
					}
					foundIntervall = new VertexIntervall(nextPossibleIntervall);
					foundIntervall.setStartTime(Math.min(arrivaleIntervall.getHighBound()-1, nextPossibleIntervall.getLowBound()-travelTime));
					//foundIntervall.setStartTime(arrivaleIntervall.getHighBound()-1);
					result.add(foundIntervall);
				}
				if(GlobalFlowCalculationSettings.useHoldover)
				{
					//if we use holdover, we dont need to propagate any intervall
					//after the first one found!
					break;
				}
				else
				{
					int tmp = nextPossibleIntervall == null ? 0 : nextPossibleIntervall.getHighBound();
					//we get the next edgeintervall
					int nextTimeStep = Math.max(currentIntervall.getHighBound(), tmp );
					if(nextTimeStep > latestStartPossible)
						//if the next intervall starts after our latestArrivalTime, we are finished
						break;
					currentIntervall = this.getIntervallAt(nextTimeStep);
				}
			}
		}
		if(!forward){
			while(currentIntervall != null)
			{	
				EdgeIntervall nextPossibleIntervall = minPossibleBackwards(earliestStartPossible);
				if(nextPossibleIntervall == null)
				{
					//no intervall can be found after this!
					break;
				}
				if(!GlobalFlowCalculationSettings.useHoldover)
					nextPossibleIntervall = this.forbidHoldoverBackward(nextPossibleIntervall, latestStartPossible);
				
				if(nextPossibleIntervall!=null){
					System.out.println("i: " + arrivaleIntervall);
					System.out.println("j: " + nextPossibleIntervall);
					foundIntervall = new VertexIntervall(nextPossibleIntervall);
					foundIntervall.setStartTime(arrivaleIntervall.getLowBound());
					result.add(foundIntervall);
				}
				if(GlobalFlowCalculationSettings.useHoldover)
				{
					//if we use holdover, we dont need to propagate any intervall
					//after the first one found!
					break;
				}
				else
				{
					//we get the next edgeintervall
					int tmp = nextPossibleIntervall == null ? 0 : nextPossibleIntervall.getHighBound();
					//we get the next edgeintervall
					int nextTimeStep = Math.max(currentIntervall.getHighBound(), tmp );
					if(nextTimeStep > latestStartPossible)
						//if the next intervall starts after our latestArrivalTime, we are finished
						break;
					currentIntervall = this.getIntervallAt(nextTimeStep);
				}
			}
		}
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
		return (int)this._traveltime.getTravelTimeForFlow(flow);
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
