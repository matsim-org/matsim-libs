/* *********************************************************************** *
 * project: org.matsim.*
 * Flow.java
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

package playground.dressler.ea_flow;

//java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.dressler.Interval.EdgeInterval;
import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.SourceIntervals;
/**
 * Class representing a dynamic flow on an network with multiple sources and a single sink
 * @author Manuel Schneider
 *
 */

public class Flow {
////////////////////////////////////////////////////////////////////////////////////////
//--------------------------FIELDS----------------------------------------------------//
////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * The global settings.
	 */
	private final FlowCalculationSettings _settings;

	/**
	 * The network on which we find routes.
	 * We expect the network not to change between runs!
	 */
	private final NetworkLayer _network;

	/**
	 * Edge representation of flow on the network
	 */
	private HashMap<Link, EdgeIntervals> _flow;


	/**
	 * Source outflow, somewhat like holdover for sources
	 */
	private HashMap<Node, SourceIntervals> _sourceoutflow;

	/**
	 * TimeExpandedTimeExpandedPath representation of flow on the network
	 */
	private final LinkedList<TimeExpandedPath> _TimeExpandedPaths;

	/**
	 * list of all sources
	 */
	private final ArrayList<Node> _sources;
	
	/**
	 * list of all sinks
	 */
	private final ArrayList<Node> _sinks;

	/**
	 * stores unsatisfied demands for each source
	 */
	private Map<Node,Integer> _demands;

	/**
	 * the Time Horizon (for easy access)
	 */
	private int _timeHorizon;


	/**
	 * total flow augmented so far
	 */
	private int totalflow;



	/**
	 * TODO use debug mode
	 * flag for debug mode
	 */
	@SuppressWarnings("unused")
	private static int _debug = 0;


///////////////////////////////////////////////////////////////////////////////////
//-----------------------------Constructors--------------------------------------//
///////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor that initializes a zero flow over time for the specified settings
	 * @param settings
	 */
	public Flow(FlowCalculationSettings settings) {

		this._settings = settings;
		this._network = settings.getNetwork();
		this._flow = new HashMap<Link,EdgeIntervals>();
		this._sourceoutflow = new HashMap<Node, SourceIntervals>();

		this._TimeExpandedPaths = new LinkedList<TimeExpandedPath>();
		this._demands = new HashMap<Node, Integer>();
		this._sources = new ArrayList<Node>();
		this._sinks = new ArrayList<Node>();


		for(Node node : this._network.getNodes().values()){
			if (this._settings.isSource(node)) {
				int i = this._settings.getDemand(node);
				this._sources.add(node);
				EdgeInterval temp = new EdgeInterval(0,this._settings.TimeHorizon);
				this._sourceoutflow.put(node, new SourceIntervals(temp));
				this._demands.put(node, i);
			} else if (this._settings.isSink(node)) {
				this._sinks.add(node);
			}
		}
		// initialize EdgeIntervalls
		for (Link edge : this._network.getLinks().values()) {
			EdgeInterval temp =new EdgeInterval(0,settings.TimeHorizon);
			this._flow.put(edge, new EdgeIntervals(temp, this._settings.getLength(edge)));
		}
		
		this._timeHorizon = settings.TimeHorizon;
		this.totalflow = 0;

	}

//////////////////////////////////////////////////////////////////////////////////
//--------------------Flow handling Methods-------------------------------------//
//////////////////////////////////////////////////////////////////////////////////
	/**
	 * Method to determine whether a Node is a Source with positive demand
	 * @param node Node that is checked
	 * @return true iff Node is a Source and has positive demand
	 */
	public boolean isActiveSource(final Node node) {
		Integer i = _demands.get(node);
		if (i== null){
			return false;
		}else{
			return (i > 0);
		}
	}

	/**
	 * Method for residual bottleneck capacity of the TimeExpandedPath
	 * (also limited by the source)
	 * @param TimeExpandedPath
	 * @return minimum over all unused capacities and the demand in the first node
	 */
	private int bottleNeckCapacity(final TimeExpandedPath TimeExpandedPath){
		//check if first node is a source
		Node source = TimeExpandedPath.getSource();
		if(!this._demands.containsKey(source)){
			throw new IllegalArgumentException("Startnode is no source " + TimeExpandedPath);
		}
		int result = this._demands.get(source);
		if(result == 0) {
			System.out.println("Weird. Source of TimeExpandedPath had no supply left.");
			return 0;
		}
		//go through the path edges
		//System.out.println("augmenting path: ");

		int cap;
		for(PathStep step : TimeExpandedPath.getPathSteps()){

			// FIXME really bad style ...
			if (step instanceof StepEdge) {
				StepEdge se = (StepEdge) step;
				Link edge = se.getEdge();

				if(se.getForward()){
					cap = this._settings.getCapacity(edge) - this._flow.get(edge).getFlowAt(se.getStartTime());
				} else {
					cap = this._flow.get(edge).getFlowAt(se.getArrivalTime());
				}
				if(cap<result ){
					result= cap;
				}
			} else if (step instanceof StepSourceFlow) {
				StepSourceFlow ssf = (StepSourceFlow) step;
				Node node  = ssf.getStartNode().getRealNode();

				if (!ssf.getForward()) {
					SourceIntervals si = this._sourceoutflow.get(node);
					if (si == null) {
						System.out.println("Weird. Source of StepSourceFlow has no sourceoutflow!");
						return 0;
					} else {
						cap = si.getFlowAt(ssf.getStartTime());
						if (cap < result) {
							result = cap;
						}
					}
				}
				/* no else, because outflow out of a source has no cap.
				   (the demand of the original source is accounted for,
				   demand of sources we pass through does not matter) */
			} else if (step instanceof StepSinkFlow) {
				// FIXME cannot handle residual stepsinkflow yet!
				if (!step.getForward()) {
					throw new RuntimeException("BottleNeck for residual StepSinkFlow not supported yet!");
				}
				// else no cap, because inflow into sink is uncapped
			} else {
				throw new RuntimeException("Unsupported kind of PathStep!");
			}

		}
		//System.out.println(""+ result);
		return result;
	}

	/**
	 * Method to add another TimeExpandedPath to the flow. The TimeExpandedPath will be added with flow equal to its bottleneck capacity
	 * @param TimeExpandedPath the TimeExpandedPath on which the maximal possible flow is augmented
	 * @return Amount of flow augmented
	 */
	public int augment(TimeExpandedPath TEP){
	  int bottleneck = bottleNeckCapacity(TEP);
	  this.augment(TEP, bottleneck);
	  return bottleneck;
	}

	/**
	 * Method to add another TimeExpandedPath to the flow with a given flow value on it
	 * @param TEP The TimeExpandedPath on which the maximal flow possible is augmented
	 * @param gamma Amount of flow to augment
	 * @return Amount of flow that was really augmented. Should be gamma under most circumstances.
	 */
	public void augment(TimeExpandedPath TEP, int gamma){
	  if (TEP.hadToFixSourceLinks()) {
	    System.out.println("TimeExpandedPath should start with PathEdge of type SourceOutflow! Fixed.");
	  }
	  TEP.setFlow(gamma);
	  if (gamma == 0) {
		  if (_debug > 0) {
			  System.out.println("I refuse to augment with 0 flow.");
		  }
		  return;
	  }
	  dumbaugment(TEP, gamma); // throws exceptions if something is wrong
	  this.totalflow += gamma;

	  this.unfold(TEP);
	}

	/**
	 * Method to change just the flow values on the step
	 * @param step The Step
	 * @param gamma Amount of flow to augment, positive or negative!
	 */

	private void augmentStep(PathStep step, int gamma) {

		// FIXME really bad style ...
		if (step instanceof StepEdge) {
			StepEdge se = (StepEdge) step;
			Link edge = se.getEdge();

			if(se.getForward()){
				this._flow.get(edge).augment(se.getStartTime(), gamma, this._settings.getCapacity(edge));
			}	else {
				this._flow.get(edge).augment(se.getArrivalTime(), -gamma, this._settings.getCapacity(edge));
			}
		} else if (step instanceof StepSourceFlow) {
			StepSourceFlow ssf = (StepSourceFlow) step;
			Node source;

			if (ssf.getForward()) {
				source = ssf.getStartNode().getRealNode(); 
			} else {
				source = ssf.getArrivalNode().getRealNode();
			}

			Integer demand = this._demands.get(source);

			if (demand == null) {
				throw new IllegalArgumentException("Startnode is no source four StepSourceFlow " + step);
			}

			if (ssf.getForward()) {
				demand -= gamma;
				if (demand < 0) {
					throw new IllegalArgumentException("too much flow on StepSourceFlow " + step);
				}
				this._sourceoutflow.get(source).augment(ssf.getArrivalTime(), gamma, Integer.MAX_VALUE);
				this._demands.put(source, demand);
			} else {
				demand += gamma;
				if (demand > this._settings.getDemand(source)) {
					throw new IllegalArgumentException("too much flow sent back into source " + step);
				}
				this._sourceoutflow.get(source).augment(ssf.getStartTime(), -gamma, Integer.MAX_VALUE);
				this._demands.put(source, demand);
			}

		} else if (step instanceof StepSinkFlow) {
			// FIXME cannot handle residual stepsinkflow yet!
			if (!step.getForward()) {
				throw new RuntimeException("BottleNeck for residual StepSinkFlow not supported yet!");
			} else {
				// FIXME, adjust demand of sink
			}			
		} else {
			throw new RuntimeException("Unsupported kind of PathStep!");
		}

	}

	/**
	 * Method to change just the flow values on the step, no checking is done!
	 * @param step The Step
	 * @param gamma Amount of flow to augment, positive or negative!
	 */

	private void augmentStepUnsafe(PathStep step, int gamma) {

		// FIXME really bad style ...
		if (step instanceof StepEdge) {
			StepEdge se = (StepEdge) step;
			Link edge = se.getEdge();

			if(se.getForward()){
				this._flow.get(edge).augmentUnsafe(se.getStartTime(), gamma);
			}	else {
				this._flow.get(edge).augmentUnsafe(se.getArrivalTime(), -gamma);
			}
		} else if (step instanceof StepSourceFlow) {
			StepSourceFlow ssf = (StepSourceFlow) step;
			Node source;

			if (ssf.getForward()) {
				source = ssf.getStartNode().getRealNode(); 
			} else {
				source = ssf.getArrivalNode().getRealNode();
			}

			Integer demand = this._demands.get(source);

			if (demand == null) {
				throw new IllegalArgumentException("Startnode is no source four StepSourceFlow " + step);
			}

			if (ssf.getForward()) {
				demand -= gamma;
				this._sourceoutflow.get(source).augmentUnsafe(ssf.getArrivalTime(), gamma);
				this._demands.put(source, demand);
			} else {
				demand += gamma;
				this._sourceoutflow.get(source).augmentUnsafe(ssf.getStartTime(), -gamma);
				this._demands.put(source, demand);
			}

		} else {
			throw new RuntimeException("Unsupported kind of PathStep!");
		}

	}

	/**
	 * Method to check the flow on the edge associated with step
	 * @param step The Step to check.
	 * @return true iff 0 <= flow <= capacity at time associated with step
	 */

	private boolean checkStep(PathStep step) {

		// FIXME really bad style ...
		if (step instanceof StepEdge) {
			StepEdge se = (StepEdge) step;
			Link edge = se.getEdge();

			if(se.getForward()){
				return this._flow.get(edge).getIntervalAt(se.getStartTime()).checkFlow(this._settings.getCapacity(edge));

			}	else {
				return this._flow.get(edge).getIntervalAt(se.getArrivalTime()).checkFlow(this._settings.getCapacity(edge));
			}
		} else if (step instanceof StepSourceFlow) {
			StepSourceFlow ssf = (StepSourceFlow) step;
			Node source;

			if (ssf.getForward()) {
				source = ssf.getStartNode().getRealNode(); 
			} else {
				source = ssf.getArrivalNode().getRealNode();
			}

			Integer demand = this._demands.get(source);

			if (demand == null) {
				throw new IllegalArgumentException("Startnode is no source four StepSourceFlow " + step);
			}

			if (demand < 0 || demand > this._settings.getDemand(source)) {
				return false;
			    //throw new RuntimeException("Demand of source is negative or too large!");
			}

			if (ssf.getForward()) {
				return this._sourceoutflow.get(source).getIntervalAt(ssf.getArrivalTime()).checkFlow(Integer.MAX_VALUE);
			} else {
				return this._sourceoutflow.get(source).getIntervalAt(ssf.getStartTime()).checkFlow(Integer.MAX_VALUE);
			}

		} else {
			throw new RuntimeException("Unsupported kind of PathStep!");
		}
	}

	/**
	 * Method to change just the flow values according to TimeExpandedPath and gamma
	 * @param TEP The TimeExpandedPath on which the flow is augmented
	 * @param gamma Amount of flow to augment
	 * @return Amount of flow that was really augmented. Should be gamma under most circumstances.
	 */

	private void dumbaugment(TimeExpandedPath TEP, int gamma) {

		for (PathStep step : TEP.getPathSteps()) {
			augmentStep(step, gamma);
		}

	}


	/**
	 * method to resolve residual edges in TEP
	 * @param TEP A TimeExpandedPath
	 */
	private void unfold(TimeExpandedPath TEPtoAdd){
		// this function could be quite slow ...
		/*
		 * Paths in flow._timeexpandedpaths are always good, i.e. use just forward steps.
		 * The current path may contain loops! (Not from the BellmanFord,
		 *   but from unfolding with other paths.) These must be removed first.
		 * Other unprocessed paths can also contain loops.
		 * Unprocessed paths cannot contain forward steps for residual steps from the
		 * current path (or any other unprocessed path), because that would imply a loop
		 * in some path that caused these unprocessed paths. (use induction)
		 *
		 * FIXME removing loops can change the flow on the edges!
		 * make sure that this function always keeps the flow and the TEP collection in sync!
		 */

		// TEPs that need processing
		LinkedList<TimeExpandedPath> unfinishedTEPs = new LinkedList<TimeExpandedPath>();
		unfinishedTEPs.add(TEPtoAdd);

		// processed TEPs that we want to add later on
		LinkedList<TimeExpandedPath> goodTEPs = new LinkedList<TimeExpandedPath>();

		// processed TEPs that now have 0 flow and we want to remove them later
		LinkedList<TimeExpandedPath> zeroTEPs = new LinkedList<TimeExpandedPath>();

		while (!unfinishedTEPs.isEmpty()) {
			TimeExpandedPath TEP = unfinishedTEPs.poll();
			if (_debug > 0) {
			  System.out.println("Unfolding: " + TEP);
			}

		/*	// DEBUG
			for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
				if (tempTEP.getFlow() == 0) {
					 System.out.println("Bad allPath.getFlow() == 0... " + tempTEP);
					 throw new RuntimeException("Bad allPath.getFlow() == 0");
				  }
			}*/


			boolean onlyForward = true;

			/* check for loops in the path!
			 * these can be pairwise opposing residual edges
			 * or visiting the same time-expanded node twice
			 * be careful with sourceoutflow-nodes ...
			 */

/*			// DEBUG
			if (!TEP.check()) {
				System.out.println("Bad unprocessed TEP!");
				System.out.println(TEP);
				throw new RuntimeException("Bad unprocessed TEP!");
			}
			if (TEP.getFlow() == 0) {
				System.out.println("TEP.getFlow() == 0");
				throw new RuntimeException("TEP.getFlow() == 0");
			}*/

			PathStep startLoop = null, endLoop = null;

			for (PathStep step1 : TEP.getPathSteps()) {
				startLoop = step1;
				boolean afterwards = false;
				// TODO just start searching at step1 ... would be much nicer
				for (PathStep step2 : TEP.getPathSteps()) {
					if (step1 == step2) { // not step.equals(step2), that would be counterproductive
						afterwards = true;
						continue;
					} else if (afterwards) {
						if (step1.getStartNode().equals(step2.getStartNode())) {
							// loop!
							//System.out.println("Loop! " + step1 + " and " + step2);
							endLoop = step2;
						}
					}
				}
				if (endLoop != null) {
					// this is the loop starting the earliest and ending the latest!
					break;
				}
			}

			if (endLoop != null) {
				ArrayList<PathStep> loop = new ArrayList<PathStep>();
				if (_debug > 0) {
				  System.out.println("with loops: " + TEP);
				}
				TimeExpandedPath newTEP = new TimeExpandedPath();
				newTEP.setFlow(TEP.getFlow());
				boolean inloop = false;
				for (PathStep step : TEP.getPathSteps()) {
					if (step == startLoop) {
						inloop = true;
					} else if (step == endLoop) {
						inloop = false;
					}
					if (!inloop) {
						newTEP.append(step);
					} else {
						/* FIXME adjust flow on edges to be in sync with TEP collection!
						   However, removing the flow might temporarily exceed [0, u] boundaries
						   if the TEP could unfold with itself */
						augmentStepUnsafe(step, -TEP.getFlow());
						loop.add(step);
					}
				}

				// just to be sure
				// if (_debug > 0) {
				for (PathStep step : loop) {
					if (!checkStep(step)) {
						System.out.println("without loops: " + TEP);
						System.out.println("Bad unlooped new TEP: " + newTEP);
						System.out.println("problem step: " + step);
						throw new RuntimeException("Unsafe unlooping violated bounds on flow!");
					}
				}
			    // }

				if (_debug > 0) {
					System.out.println("without loops: " + TEP);
					if (!newTEP.check()) {
						System.out.println("Bad unlooped new TEP:");
						System.out.println(newTEP);
						throw new RuntimeException("Bad unlooped TEP!");
					}
				}

				TEP = newTEP;
			}



			// traverse in order
			// be careful not to rearrange it and expect things to continue
			for (PathStep step : TEP.getPathSteps()) {
			   if (!step.getForward()) {
				   onlyForward = false;
				   if (_debug > 0) {
				     System.out.println("Found backwards step: " + step);
				   }

				   int flowToUndo = TEP.getFlow();

				   if (flowToUndo == 0) {
					   System.out.println("flowToUndo == 0");
					   System.out.println("processing path: " + TEP);
					   System.out.println("good paths:");
					   for (TimeExpandedPath tempTEP : goodTEPs) {
							System.out.println(tempTEP);
						}
					   System.out.println("unfinished paths:");
					   for (TimeExpandedPath tempTEP : unfinishedTEPs) {
							System.out.println(tempTEP);
						}
					   throw new RuntimeException("flowToUndo == 0");
				   }

  				   // search for an appropriate TEP to unfold with
				   for (TimeExpandedPath otherTEP : this._TimeExpandedPaths) {
					   
					   // FIXME this seems to slow down rather than speed up the augmenting!
					   // That is somewhat surprising, though. Needs more testing.
					   if (!otherTEP.doesTouch(step.getStartNode()) || !otherTEP.doesTouch(step.getArrivalNode())) {
						   // this path cannot contain the reverse of step
						   //System.out.println("Skipped checking a path!");
						   continue;
					   }
					   
 					   // DEBUG
					   if (otherTEP.getFlow() == 0) {
						   throw new RuntimeException("OtherTEP.getFlow() == 0 even earlier");
					   }

					   if (flowToUndo == 0) break;
					   for (PathStep otherStep : otherTEP.getPathSteps()) {
						   if (step.isResidualVersionOf(otherStep)) {

							   // DEBUG
							   if (otherTEP.getFlow() == 0) {
								   throw new RuntimeException("OtherTEP.getFlow() == 0 already");
							   }

							   if (_debug > 0) {
							     System.out.println("Found step to unfold with: " + otherStep);
							     System.out.println("It's in otherTEP: " + otherTEP);
							   }

 							   // must do this everytime so we get true fresh copies of myHead and myTail!
							   LinkedList<TimeExpandedPath> myParts = TEP.splitPathAtStep(step, false);
							   TimeExpandedPath myHead = myParts.getFirst();
							   TimeExpandedPath myTail = myParts.getLast();

							   // split the found path
							   LinkedList<TimeExpandedPath> otherParts = otherTEP.splitPathAtStep(step, false);
							   TimeExpandedPath otherHead = otherParts.getFirst();
							   TimeExpandedPath otherTail = otherParts.getLast();

							   int augment = Math.min(flowToUndo, otherTEP.getFlow());

							   if (_debug > 0) {
							     System.out.println("myHead " + myHead);
							     System.out.println("myTail " + myTail);
							     System.out.println("otherHead " + otherHead);
							     System.out.println("otherTail " + otherTail);
							   }

							   if (otherTEP.getFlow() == 0) {
								   System.out.println("OtherTEP.getFlow() == 0");
								   System.out.println("processing path: " + TEP);
								   System.out.println("other path: " + otherTEP);
								   System.out.println("good paths:");
								   for (TimeExpandedPath tempTEP : goodTEPs) {
										System.out.println(tempTEP);
									}
								   System.out.println("unfinished paths:");
								   for (TimeExpandedPath tempTEP : unfinishedTEPs) {
										System.out.println(tempTEP);
									}
								   throw new RuntimeException("OtherTEP.getFlow() == 0");
							   }

							   myHead.addTailToPath(otherTail);
							   myHead.setFlow(augment);
							   goodTEPs.add(myHead);

							   otherHead.addTailToPath(myTail);
							   otherHead.setFlow(augment);
							   unfinishedTEPs.add(otherHead);

							   otherTEP.setFlow(otherTEP.getFlow() - augment);

							   flowToUndo -= augment;

							   if (_debug > 0) {
							     System.out.println("What's left of otherTEP: ");
							     System.out.println(otherTEP);
							   }

							   // flow might be zero, should be deleted!
							   if (otherTEP.getFlow() == 0) {
							     zeroTEPs.add(otherTEP);
							   }

							   // we found the forward step we were looking for
							   // look for another TEP to unfold with
							   break;
						   }
					   }
				   }

				   if (flowToUndo > 0) {
					   System.out.println("problem path: " + TEP);
					   System.out.println("good paths:");
					   for (TimeExpandedPath tempTEP : goodTEPs) {
							System.out.println(tempTEP);
						}
					   System.out.println("unfinished paths:");
					   for (TimeExpandedPath tempTEP : unfinishedTEPs) {
							System.out.println(tempTEP);
					   }
					   System.out.println("zero paths:");
					   for (TimeExpandedPath tempTEP : zeroTEPs) {
							System.out.println(tempTEP);
					   }
					   System.out.println("All paths so far:");
						for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
							System.out.println(tempTEP);
						}

					   throw new RuntimeException("Could not undo all flow on backwards step!");
				   }

				   // this TEP is broken up ... don't bother with it anymore
				   break;
			   }
			}
			if (onlyForward) {
				goodTEPs.add(TEP);
			}

			// DEBUG
			int vorher = this._TimeExpandedPaths.size();
			int zero = zeroTEPs.size();

			this._TimeExpandedPaths.removeAll(zeroTEPs);
			zeroTEPs.clear();

			int nachher = this._TimeExpandedPaths.size();
			if (nachher + zero != vorher) {
				throw new RuntimeException("Did not delete all paths with zero flow!");
			}


			/*// DEBUG
			for (TimeExpandedPath tempTEP : goodTEPs) {
			  if (tempTEP.getFlow() == 0) {
				 System.out.println("Bad goodPath.getFlow() == 0... " + tempTEP);
				 throw new RuntimeException("Bad goodPath.getFlow() == 0");
			  }
			}*/

			this._TimeExpandedPaths.addAll(goodTEPs);
			goodTEPs.clear();

			if (_debug > 0) {
				System.out.println("All paths so far:");
				for (TimeExpandedPath tempTEP : this._TimeExpandedPaths) {
					System.out.println(tempTEP);
				}
			}
		}
	}

	/**
	 * decides whether a Node is a non-active (depleted) Source
	 * @param node Node to check for
	 * @return true iff node is a Source now with demand 0
	 */
	public boolean isNonActiveSource(final Node node){
		if (this._settings.isSource(node)) { // superfluous ... only sources are in _demands
		  Integer i = this._demands.get(node);
		  return (i != null && i == 0);
		}
		return false;
	}

////////////////////////////////////////////////////////////////////////////////////
//-----------evaluation methods---------------------------------------------------//
////////////////////////////////////////////////////////////////////////////////////

	/**
	 * gives back an array containing the amount of flow into the sink for all time steps from 0 to time horizon
	 */
	public int[] arrivals(){
		int maxtime = 0;
		int[] temp = new int[this._timeHorizon+1];
		for (TimeExpandedPath TimeExpandedPath : _TimeExpandedPaths){
			int flow = TimeExpandedPath.getFlow();
			int time = TimeExpandedPath.getArrival();
			if (maxtime < time){
				maxtime = time;
			}
			temp[time]+=flow;
		}

		int[] result = new int[maxtime+1];
		for(int i=0; i<=maxtime;i++){
			result[i]=temp[i];
		}
		return result;

	}

	/**
	 * gives back an array containing the total amount of flow into the sink by a given time
	 * for all time steps from 0 to time horizon
	 */
	public int[] arrivalPattern(){
		int[] result = this.arrivals();
		int sum = 0;
		for (int i=0;i<result.length; i++){
			sum+=result[i];
			result[i]=sum;
		}
		return result;
	}
	/**
	 * String representation of the arrivals specifying the amount of flow into the sink
	 * for all time steps from 0 to time horizon
	 * @return String representation of the arrivals
	 */
	public String arrivalsToString(){
		//StringBuilder strb1 = new StringBuilder();
		StringBuilder strb2 = new StringBuilder("  arrivals:");
		int[] a =this.arrivals();
		for (int i=0; i<a.length;i++){
			String temp = String.valueOf(a[i]);
			strb2.append(" "+i+":"+temp);
		}
		return strb2.toString();
	}

	/**
	 * a STring specifying the total amount of flow into the sink by a given time
	 * for all time steps from 0 to time horizon
	 * @return String representation of the arrival pattern
	 */
	public String arrivalPatternToString(){
		//StringBuilder strb1 = new StringBuilder();
		StringBuilder strb2 = new StringBuilder("arrival pattern:");
		int[] a =this.arrivalPattern();
		for (int i=0; i<a.length;i++){
			String temp = String.valueOf(a[i]);
			strb2.append(" "+i+":"+temp);
		}
		return strb2.toString();
	}

//////////////////////////////////////////////////////////////////////////////////////
//---------------------------Plans Converter----------------------------------------//
//////////////////////////////////////////////////////////////////////////////////////


	@SuppressWarnings("unchecked")

	/**
	 *
	 */
	public PopulationImpl createPopulation(String oldfile){
		//check whether oldfile exists
		//boolean org = (oldfile!=null);
		//HashMap<Node,LinkedList<Person>> orgpersons = new  HashMap<Node,LinkedList<Person>>();

		//read old network an find out the startnodes of persons if oldfile exists
		/*if(org){
			Population population = new PopulationImpl(PopulationImpl.NO_STREAMING);
			new MatsimPopulationReader(population,_network).readFile(oldfile);
			_network.connect();
			for(Person person : population.getPersons().values() ){
				Link link = person.getPlans().get(0).getFirstActivity().getLink();
				if (link == null) continue; // happens with plans that don't match the network.

				Node node = link.getToNode();
				if(orgpersons.get(node)==null){
					LinkedList<Person> list = new LinkedList<Person>();
					list.add(person);
					orgpersons.put(node, list);
				}else{
					LinkedList<Person> list = orgpersons.get(node);
					list.add(person);
				}
			}
		}*/

		//construct Population
		PopulationImpl result = new ScenarioImpl().getPopulation();
		int id =1;
		for (TimeExpandedPath path : this._TimeExpandedPaths){
			if(path.isforward()){
				//units of flow on the Path
				int nofpersons = path.getFlow();
				// list of links in order of the path
				LinkedList<Id> ids = new LinkedList<Id>();
				for (PathStep step : path.getPathSteps()){
					if (step instanceof StepEdge) {
					  ids.add(((StepEdge) step).getEdge().getId());
					}
				}


				//if (!emptylegs) {
					// normal case, write the routes!
					LinkNetworkRouteImpl route;

					Node firstnode  = _network.getLinks().get(ids.get(0)).getFromNode();

					// for each unit of flow construct a Person
					for (int i =1 ; i<= nofpersons;i++){
						//add the first edge if olfile exists
						String stringid = null;
						PersonImpl orgperson = null;
						/*if(org && (( orgpersons.get(firstnode))!=null) ){
							LinkedList<Person> list = orgpersons.get(firstnode);
							orgperson = list.getFirst();
							list.remove(0);
							if(list.isEmpty()){
								orgpersons.remove(firstnode);
							}
							Link firstlink = orgperson.getPlans().get(0).getFirstActivity().getLink();
							if(i==1){
							ids.add(0,firstlink.getId());
							}
							stringid = orgperson.getId().toString();
						}else{*/
							stringid = "new"+String.valueOf(id);
							id++;
						//}

//						route = new BasicRouteImpl(ids.get(0),ids.get(ids.size()-1));
						Id startLinkId = ids.get(0);
						Id endLinkId = ids.get(ids.size()-1);
						route = new LinkNetworkRouteImpl(startLinkId, endLinkId, _network);

						List<Id> routeLinkIds = null;
						if (ids.size() > 1) {
							routeLinkIds = new ArrayList<Id>();
//							route.setLinkIds(ids.subList(1, ids.size()-1));
							for (Id iid : ids.subList(1, ids.size()-1)){
								routeLinkIds.add(iid);
							}
						}
						route.setLinkIds(startLinkId, routeLinkIds, endLinkId);


						LegImpl leg = new LegImpl(TransportMode.car);
						//Leg leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
						leg.setRoute(route);
						Link fromlink =_network.getLinks().get(ids.getFirst());
						ActivityImpl home = new ActivityImpl("h", fromlink.getId());
//						home.setLinkId(fromlink.getId());
						Link tolink =_network.getLinks().get(ids.getLast());
						ActivityImpl work = new ActivityImpl("w", tolink.getId());
//						work.setLinkId(tolink.getId());


						//Act home = new org.matsim.population.ActImpl("h", path.getPathEdges().getFirst().getEdge());
						home.setEndTime(0);
						//home.setCoord(_network.getLink(ids.getFirst()).getFromNode().getCoord());
						// no end time for now.
						//home.setEndTime(path.getPathEdges().getFirst().getTime());

						//Act work = new org.matsim.population.ActImpl("w", path.getPathEdges().getLast().getEdge());
						work.setEndTime(0);
						//work.setCoord(_network.getLink(ids.getLast()).getToNode().getCoord());


						Id matsimid  = new IdImpl(stringid);
						PersonImpl p = new PersonImpl(matsimid);
						PlanImpl plan = new org.matsim.core.population.PlanImpl(p);
						plan.addActivity(home);
						plan.addLeg(leg);
						plan.addActivity(work);
						p.addPlan(plan);
						result.addPerson(p);
						id++;
					}

			}else{ // residual edges
				// this should not happen!
				System.out.println("createPopulation encountered a residual step in");
				System.out.println(path);
				System.out.println("This should not happen!");
			}


		}


		return result;
	}

//////////////////////////////////////////////////////////////////////////////////////
//------------------- Clean Up---------------------------------------//
//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Call the cleanup-method for each edge
	 */
	public int cleanUp() {
		int gain = 0;
		for (EdgeIntervals EI : _flow.values()) {
		  gain += EI.cleanup();
		}
		for (Node node : this._sourceoutflow.keySet()) {
			 SourceIntervals si = this._sourceoutflow.get(node);
			 if (si != null)
			   gain += si.cleanup();
		}
		return gain;
	}

//////////////////////////////////////////////////////////////////////////////////////
//-------------------Getters Setters toString---------------------------------------//
//////////////////////////////////////////////////////////////////////////////////////


	/**
	 * returns a String representation of the entire flows
	 */
	@Override
	public String toString(){
		StringBuilder strb = new StringBuilder();
		for(Link link : _flow.keySet()){
			EdgeIntervals edge =_flow.get(link);
			strb.append(link.getId().toString()+ ": " + edge.toString()+ "\n");
		}
		return strb.toString();
	}

	/**
	 * @return the _demands
	 */
	public Map<Node, Integer> getDemands() {
		return this._demands;
	}

	/**
	 * @return the _flow
	 */
	public EdgeIntervals getFlow(Link edge) {
		return this._flow.get(edge);
	}

	public SourceIntervals getSourceOutflow(Node node) {
		return this._sourceoutflow.get(node);
	}

	/**
	 * @return the _sink
	 */
	public ArrayList<Node> getSinks() {
		return _sinks;
	}

	/**
	 * @return the _sources
	 */
	public ArrayList<Node> getSources() {
		return this._sources;
	}

	/**
	 * @return the network
	 */
	public NetworkLayer getNetwork() {
		return this._network;
	}

	/**
	 * @return the total flow so far
	 */
	public int getTotalFlow() {
		return this.totalflow;
	}

    /** @return the paths
	*/
	public LinkedList<TimeExpandedPath> getPaths() {
		return this._TimeExpandedPaths;
	}

	/**

	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(int debug){
		Flow._debug=debug;
	}

	public int getStartTime()
	{
		return 0;
	}

	public int getEndTime()
	{
		return this.arrivals().length-1;
	}

	public String measure() {
		String result;
		// statistics for EdgeIntervalls and SourceIntervalls
		// min, max, avg size
		int min = Integer.MAX_VALUE;
		int max = 0;
		long sum = 0;
		for (EdgeIntervals i : this._flow.values()) {
			int size = i.getSize();
			sum += size;
			min = Math.min(min, size);
			max = Math.max(max, size);
		}
		result = "  Size of EdgeIntervalls (min/avg/max): " + min + " / " + sum / (double) this._network.getNodes().size() + " / " + max + "\n";

		for (SourceIntervals i : this._sourceoutflow.values()) {
			int size = i.getSize();
			sum += size;
			min = Math.min(min, size);
			max = Math.max(max, size);
		}
		result += "  Size of SourceIntervalls (min/avg/max): " + min + " / " + sum / (double) this._network.getNodes().size() + " / " + max + "\n";
		return result;
	}

}
