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
import playground.dressler.ea_flow.FlowEdgeTraversalCalculator;
import playground.dressler.ea_flow.GlobalFlowCalculationSettings;


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
	 * traveltime caclator
	 */
	public final FlowEdgeTraversalCalculator _traveltime;
	
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
	public EdgeIntervalls(final FlowEdgeTraversalCalculator traveltime){
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
		for(_tree.reset();!_tree.isAtEnd();_tree.increment()){
			EdgeIntervall i= (EdgeIntervall) _tree._curr.obj;
			str.append(l+i.getLowBound()+";"+i.getHighBound()+r+" " +
					"f: "+i.getFlow()+" \n");
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
	 * used to constrain the bounds of the intervall eIntervall, 
	 * so that no Holdover is needed
	 * @param eIntervall
	 * @param latestStartTimeAtFromNode the last, time (included) 
	 * at which flow can be send
	 * @return	null, if holdover is needed; a new Intervall otherwise
	 *  which forbids holdover
	 */
	public EdgeIntervall forbidHoldoverForwards(final EdgeIntervall eIntervall, 
			final int latestStartTimeAtFromNode){
		if(eIntervall == null)
			return null;
		if(latestStartTimeAtFromNode >= eIntervall.getLowBound())
		{
			int latestPossibleStart = Math.min(latestStartTimeAtFromNode +1,
					eIntervall.getHighBound());
			return new EdgeIntervall(eIntervall.getLowBound(), latestPossibleStart,
					eIntervall.getFlow());
		}
		return null;
	}
	
	/**
	 * finds the next EdgeIntervall after time arrivalTime - maxTraveltime 
	 * upon which f>0 
	 * so that flow could be sent over the Residual Edge starting at time t 
	 * and arrive 
	 * during the returned Intervall  
	 * @param earliestStartTimeAtFromNode time >= traveltime
	 * @return Edge first Intervall [a,b] with f>0 a=arrivalTime-traveltime
	 */
	public EdgeIntervall minPossibleBackwards(final int earliestStartTimeAtFromNode){
		//calc earliest start time at the to node 
		int earliestStartTimeAtToNode = earliestStartTimeAtFromNode - 
				this._traveltime.getMaximalTravelTime();
		earliestStartTimeAtToNode = Math.max(0, earliestStartTimeAtToNode);
		EdgeIntervall forwardIntervall = (EdgeIntervall)this.getIntervallAt(
				earliestStartTimeAtToNode);
		while(forwardIntervall != null)
		{
			int flow = this.getFlowAt(forwardIntervall.getLowBound());
			//we need flow to reverse it!
			if(flow > 0){
				int traveltime = this._traveltime.getTravelTimeForFlow(flow);
				//we have to arrive after or at the minimal time at which 
				//we can send flow
				if(forwardIntervall.getHighBound() + traveltime > 
						earliestStartTimeAtFromNode){
					int earliestStartTimeForward = Math.max(
							earliestStartTimeAtFromNode - traveltime,
							forwardIntervall.getLowBound());
					//if the flow can be send before the highbound of 
					//the forwardIntervall, we have found the first
					//valid intervall
					if(earliestStartTimeForward < forwardIntervall.getHighBound())
						return new EdgeIntervall(earliestStartTimeForward, 
								forwardIntervall.getHighBound(), 
								forwardIntervall.getFlow());
				}
			}
			if(forwardIntervall.getHighBound() == Integer.MAX_VALUE)
				break;
			forwardIntervall = this.getIntervallAt(forwardIntervall.getHighBound());
		}
		return null;
	}
	
	/**
	 * finds the next EdgeIntervall after time arrivalTime - maxTraveltime 
	 * upon which f>0 
	 * so that flow could be sent over the Residual Edge
	 *  starting at time t and arrive during the returned Intervall  
	 * @param earliestStartTimeAtFromNode time >= traveltime
	 * @return Edge first Intervall [a,b] with f>0 a=arrivalTime-traveltime
	 */
	public EdgeIntervall forbidHoldoverBackward(final EdgeIntervall eIntervall,
			final int latestStartTimeAtFromNode){
		if(eIntervall == null){
			return null;
		}
		int traveltime = _traveltime.getTravelTimeForFlow(eIntervall.getFlow());
		//the flow has to be negated before the latest possible arrival time at 
		//the from node
		if(latestStartTimeAtFromNode < eIntervall.getLowBound() + traveltime){
			return null;
		}
		int latestPossibleSend = Math.min(latestStartTimeAtFromNode-traveltime + 1,
				eIntervall.getHighBound());
		return new EdgeIntervall(eIntervall.getLowBound(), latestPossibleSend, 
				eIntervall.getFlow());
	}
		
	/**
	 * Gves A list Of Intervallt upon which the ther node can be reached over the Link
	 * @param arrivaleIntervall VertexIntervall where we can start
	 * @param u capacity of the Link
	 * @param forward indicates whether we use residual edge or not 
	 * @return
	 */
	public ArrayList<VertexIntervall> propagate(final VertexIntervall 
			arrivaleIntervall,final int u ,final boolean forward){
		ArrayList<VertexIntervall> result = new ArrayList<VertexIntervall>();
		int earliestStartPossible = arrivaleIntervall.getLowBound();
		//-1 is important!
		int latestStartPossible = arrivaleIntervall.getHighBound() - 1;
		VertexIntervall foundIntervall;
		EdgeIntervall currentIntervall = this.getIntervallAt(earliestStartPossible);
		if(forward){
			//iterate over our edge intervalls as long as the intervall
			//	is not null
			while(currentIntervall != null){
				int nextHighBound = currentIntervall.getHighBound();
				if(currentIntervall.getFlow() < u){
					EdgeIntervall nextPossibleIntervall = new EdgeIntervall(currentIntervall);
					nextPossibleIntervall.setFlow(currentIntervall.getFlow());
					nextPossibleIntervall.setLowBound(Math.max(earliestStartPossible,
							currentIntervall.getLowBound()));
					if(!GlobalFlowCalculationSettings.useHoldover)
						nextPossibleIntervall = this.forbidHoldoverForwards(nextPossibleIntervall,
								latestStartPossible);
					if(_debug>0){
						System.out.println("kapazitaet frei");
						System.out.println("old i: " +arrivaleIntervall);
						System.out.println("old j: " +nextPossibleIntervall);
					}
					if(nextPossibleIntervall!=null){
						int currentFlow = nextPossibleIntervall.getFlow();
						Integer travelTime = this._traveltime.getTravelTimeForAdditionalFlow(
								currentFlow);					
						nextPossibleIntervall = nextPossibleIntervall.shiftPositive(travelTime);
						if(_debug>0){
							System.out.println("shifted by: " + travelTime+ " -> " + 
									nextPossibleIntervall);
							System.out.println("new i: " +arrivaleIntervall);
							System.out.println("new j: " +nextPossibleIntervall);
							System.out.println("tau:" +this._traveltime);
						}
						foundIntervall = new VertexIntervall(nextPossibleIntervall);
						foundIntervall.setTravelTimeToPredecessor(travelTime);
						result.add(foundIntervall);
					}
				}
				if(GlobalFlowCalculationSettings.useHoldover){
					//if we use holdover, we dont need to propagate any intervall
					//after the first one found!
					break;
				}
				if(nextHighBound > latestStartPossible){
						//if the next intervall starts after our latestArrivalTime, 
						//we are finished
						break;
				}
				currentIntervall = this.getIntervallAt(nextHighBound);
			}
		}
		if(!forward){
			//latestStartPossible = arrivaleIntervall.getLastDepartureAtFromNode();
			while(currentIntervall != null)
			{	
				int nextHigh = currentIntervall.getHighBound();
				EdgeIntervall nextPossibleIntervall = minPossibleBackwards(
						Math.max(earliestStartPossible, currentIntervall.getLowBound()));
				if(nextPossibleIntervall == null)
				{
					//no intervall can be found after this!
					break;
				}
				
				if(!GlobalFlowCalculationSettings.useHoldover)
					nextPossibleIntervall = this.forbidHoldoverBackward(nextPossibleIntervall,
							latestStartPossible);
				if(nextPossibleIntervall!=null){
					nextHigh = nextPossibleIntervall.getHighBound() + 
					this._traveltime.getTravelTimeForFlow(nextPossibleIntervall.getFlow());
					foundIntervall = new VertexIntervall(nextPossibleIntervall);
					int traveltime = _traveltime.getTravelTimeForFlow(
							nextPossibleIntervall.getFlow());
					foundIntervall.setTravelTimeToPredecessor(traveltime);
					result.add(foundIntervall);
				}
				if(GlobalFlowCalculationSettings.useHoldover)
				{
					//if we use holdover, we dont need to propagate any intervall
					//after the first one found!
					break;
				}else{
					//we get the next edgeintervall
					if(nextHigh > latestStartPossible)
						//if the next intervall starts after our latestArrivalTime,
						//we are finished
						break;
					earliestStartPossible = nextHigh;
					currentIntervall = this.getIntervallAt(nextHigh);
				}
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
			throw new IllegalArgumentException("to much flow! flow: " + i.getFlow() + " + " +
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
	

//--------------------------------UNEUSED STUFF----------------------------------------//
	
	/**
	 * This method is not supported at the moment do not use it since it is not tested
 	 * @param arrivaleIntervall
	 * @param u
	 * @param forward
	 * @return
	 */
	public ArrayList<VertexIntervall> propagateBowEdge(final VertexIntervall 
			arrivaleIntervall, final int u ,final boolean forward){
		if(true){
			throw new RuntimeException("Method propagateBowEdge(final VertexIntervall " +
					"arrivaleIntervall, final int u ,final boolean forward) not supported at the moment");
		}
		ArrayList<VertexIntervall> result = new ArrayList<VertexIntervall>();
		int earliestStartPossible = arrivaleIntervall.getLowBound();
		//-1 is important!
		int latestStartPossible = arrivaleIntervall.getHighBound() - 1;
		VertexIntervall foundIntervall;
		EdgeIntervall currentIntervall = this.getIntervallAt(earliestStartPossible);
		if(forward){
			//iterate over our edge intervalls as long as the intervall
			//	is not null
			while(currentIntervall != null){
				EdgeIntervall nextPossibleIntervall;
				nextPossibleIntervall = this.minPossibleForwards(
						Math.max(currentIntervall.getLowBound(), earliestStartPossible),u);
				if(nextPossibleIntervall==null){
					if(_debug>0){
						System.out.println("no possible interval after:" + earliestStartPossible+
								"with cap: " +u);
					}
					break;
				}
				if(!GlobalFlowCalculationSettings.useHoldover)
					nextPossibleIntervall = this.forbidHoldoverForwards(nextPossibleIntervall,
							latestStartPossible);
				int nextHigh = Integer.MAX_VALUE;
				if(nextPossibleIntervall != null)
					nextHigh = nextPossibleIntervall.getHighBound();
				
				if(_debug>0){
					System.out.println("kapazitaet frei");
					System.out.println("old i: " +arrivaleIntervall);
					System.out.println("old j: " +nextPossibleIntervall);
				}
				if(nextPossibleIntervall!=null){
					int lastPossibleDeparture = nextPossibleIntervall.getHighBound()-1;
					int currentFlow = nextPossibleIntervall.getFlow();
					Integer travelTime = this._traveltime.getTravelTimeForAdditionalFlow(
							currentFlow);					
					nextPossibleIntervall = nextPossibleIntervall.shiftPositive(travelTime);
					if(_debug>0)
					{
						System.out.println("shifted by: " + travelTime+ " -> " +  
								nextPossibleIntervall);
						System.out.println("new i: " +arrivaleIntervall);
						System.out.println("new j: " +nextPossibleIntervall);
						System.out.println("tau:" +this._traveltime);
					}
					foundIntervall = new VertexIntervall(nextPossibleIntervall);
					foundIntervall.setTravelTimeToPredecessor(travelTime);
					foundIntervall.setLastDepartureAtFromNode(lastPossibleDeparture);
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
					int nextTimeStep = currentIntervall.getHighBound();
					if(nextHigh < nextTimeStep)
					{
						nextTimeStep = nextHigh;
						earliestStartPossible = nextHigh;
					}
					if(nextTimeStep > latestStartPossible)
						//if the next intervall starts after our latestArrivalTime, 
						//we are finished
						break;
					currentIntervall = this.getIntervallAt(nextTimeStep);
				}
			}
			result = addHoldover(result);
		}
		if(!forward){
			//latestStartPossible = arrivaleIntervall.getLastDepartureAtFromNode();
			while(currentIntervall != null)
			{	
				EdgeIntervall nextPossibleIntervall = minPossibleBackwards(
						Math.max(earliestStartPossible, currentIntervall.getLowBound()));
				if(nextPossibleIntervall == null)
				{
					//no intervall can be found after this!
					break;
				}
				if(!GlobalFlowCalculationSettings.useHoldover)
					nextPossibleIntervall = this.forbidHoldoverBackward(nextPossibleIntervall, 
							latestStartPossible);
				int nextHigh = Integer.MAX_VALUE;
				if(nextPossibleIntervall != null)
					nextHigh = nextPossibleIntervall.getHighBound() + 
					this._traveltime.getTravelTimeForFlow(nextPossibleIntervall.getFlow());
				if(nextPossibleIntervall!=null){
					foundIntervall = new VertexIntervall(nextPossibleIntervall);
					int traveltime = _traveltime.getTravelTimeForFlow(
							nextPossibleIntervall.getFlow());
					foundIntervall.setTravelTimeToPredecessor(traveltime);
					foundIntervall.setLastDepartureAtFromNode(
							nextPossibleIntervall.getHighBound() - 1);
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
					int nextTimeStep = currentIntervall.getHighBound();
					if(nextHigh < nextTimeStep)
					{
						nextTimeStep = nextHigh;
						earliestStartPossible = nextHigh;
					}
					if(nextTimeStep > latestStartPossible)
						//if the next intervall starts after our latestArrivalTime, 
						//we are finished
						break;
					currentIntervall = this.getIntervallAt(nextTimeStep);
				}
			}
			
			result = addHoldover(result);
		}
		return result;
	}
	

	/**
	 * This method is not supported at the moment do not use it since it is not tested
	 * @param oldIntervalls
	 * @return
	 */
	protected ArrayList<VertexIntervall> addHoldover(ArrayList<VertexIntervall> oldIntervalls)
	{
		if(true){
			throw new RuntimeException("Method addHoldover(ArrayList<VertexIntervall> oldIntervalls) not supported at the moment");
		}
		//order intervalls
		ArrayList<VertexIntervall> orderedResult = new ArrayList<VertexIntervall>(
				oldIntervalls.size());
		for(int i = 0; i < oldIntervalls.size(); ++i)
		{
			
			VertexIntervall toAdd = oldIntervalls.get(i);
			int insertAt = 0;
			if(orderedResult.size() > 0)
			{
				//search for insert position
				for(; insertAt < orderedResult.size(); ++insertAt)
				{
					if(toAdd.getLowBound() < orderedResult.get(insertAt).getLowBound())
					{
						break;
					}
				}
			}
			orderedResult.add(insertAt, toAdd);
		}
		//if space between: fill up
		for(int i = 0; i < orderedResult.size()-1; ++i)
		{
			VertexIntervall current = orderedResult.get(i);
			VertexIntervall next = orderedResult.get(i+1);
			if(current.getHighBound() < next.getLowBound())
			{
				//split up intervall
				VertexIntervall spaceIntervall = new VertexIntervall(current.getHighBound(), 
						next.getLowBound(), current);
				spaceIntervall.setOverridable(true);
				orderedResult.add(i+1, spaceIntervall);
				++i;
			}
		}
		if(orderedResult.size() > 0)
		{
			VertexIntervall last = orderedResult.get(orderedResult.size()-1);
			if(last.getHighBound() < Integer.MAX_VALUE)
			{
				//split up intervall
				VertexIntervall spaceIntervall = new VertexIntervall(last.getHighBound(),
						Integer.MAX_VALUE, last);
				spaceIntervall.setOverridable(true);
				orderedResult.add(spaceIntervall);
			}
		}
		return orderedResult;
	}

	/**
	 * This method is not supported at the moment do not use it since it is not tested
	 * gives the previous EdgeIntervall with respect to the order contained 
	 * @param o should be contained
	 * @return next EdgeIntervall iff o is not first and contained
	 */
	public EdgeIntervall getPrevious(final EdgeIntervall o){
		if(true){
			throw new RuntimeException("Method addHoldover(ArrayList<VertexIntervall> oldIntervalls) not supported at the moment");
		}
		if(o.getLowBound() == 0){
			return null;
		}
		return this.getIntervallAt(o.getLowBound()-1);
	}
	
	
}
