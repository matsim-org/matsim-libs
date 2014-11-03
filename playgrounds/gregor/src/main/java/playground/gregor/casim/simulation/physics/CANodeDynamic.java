/* *********************************************************************** *
 * project: org.matsim.*
 * CANode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;

//TODO implement time gap z for nodes[GL Aug' 14]
public class CANodeDynamic implements CANode{

	private static final Logger log = Logger.getLogger(CANodeDynamic.class);



	private CAMoveableEntity agent;
	private final Node node;
	private final CANetworkDynamic net;

	private final List<CALinkDynamic> links = new ArrayList<CALinkDynamic>();

	private final Map<CALink,Double> towardsLinkLastExitTimes = new HashMap<CALink,Double>();
	private final Map<CALink,CAMoveableEntity> towardsLinkLastExitAgents = new HashMap<CALink,CAMoveableEntity>();

	private final double width;

	private double agentLeft;

	private final double tFree;

	private final double cellLength;



	private final double ratio;



	private final double epsilon;


	public CANodeDynamic(Node node, CANetworkDynamic net){
		double width = 0;
		for (Link l : node.getInLinks().values()) {
			if (l.getCapacity() > width) {
				width = l.getCapacity();
			}
		}
		this.width = width;
		this.node = node;
		this.net = net;
		this.ratio = CANetworkDynamic.PED_WIDTH/this.width;
		this.cellLength = this.ratio/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.PED_WIDTH);
		this.tFree = this.cellLength/CANetworkDynamic.V_HAT;
		this.epsilon = tFree/1000;
		
	}

	@Override
	public void addLink(CALink link) {
		if (!(link instanceof CALinkDynamic)) {
			throw new RuntimeException("Incompatible link type:" + link.getClass().toString());
		}
		this.links.add((CALinkDynamic)link);
		this.towardsLinkLastExitTimes.put(link, -1.);
	}


	@Override
	public void putAgent(CAMoveableEntity a) {
		if (this.agent != null) {
			throw new RuntimeException("There is already an agent on node:" + this.node.getId());

		}
		this.agent = a;
		a.moveToNode(this);
	}
	@Override
	public CAMoveableEntity peekForAgent() {
		return this.agent;
	}

	@Override
	public CAMoveableEntity pollAgent(double time) {
		CAMoveableEntity a = this.agent;
		this.agent = null;
		this.agentLeft = time;
		return a;
	}

	@Override
	public void handleEvent(CAEvent e) {
		CAMoveableEntity a = e.getCAAgent();

		double time = e.getEventExcexutionTime();
		if (e.getCAEventType() == CAEventType.SWAP){
			handelSwap(a,time);
		} else if (e.getCAEventType() == CAEventType.TTA){
			handleTTA(a,time);
		} else {
			throw new RuntimeException("Unknown event type: " + e.getCAEventType());
		}
	}

	private void handleTTA(CAMoveableEntity a, double time) {

		Id<Link> nextLinkId = a.getNextLinkId();
		CALinkDynamic nextLink = (CALinkDynamic)this.net.getCALink(nextLinkId);

		if (nextLink.getDownstreamCANode() == this) {
			handleTTAEnterNextLinkFromDownstreamEnd(nextLink,a,time);
		} else if (nextLink.getUpstreamCANode() == this) {
			handleTTAEnterNextLinkFromUpstreamEnd(nextLink,a,time);
		} else {
			throw new RuntimeException("Inconsitent network or agent plan!");
		}


	}

	private void handleTTAEnterNextLinkFromUpstreamEnd(CALinkDynamic nextLink, CAMoveableEntity a, double time) {

		//check pre-condition
		if (nextLink.getParticles()[0] == null) {
			double z = nextLink.getZ(a);
			z *= this.ratio;
			if (nextLink.getLastLeftTimes()[0] <= (time - z+epsilon)){
				handleTTAEnterNextLinkFromUpstreamEndOnPrecondition1(nextLink,a,time);
			} else {
				handleTTAEnterNextLinkFromUpstreamEndOnPrecondition2(nextLink,a,time);
			}
		} else {
			handleTTAOnPrecondition3(a,time);
		}

	}


	private void handleTTAEnterNextLinkFromUpstreamEndOnPrecondition1(
			CALinkDynamic nextLink, CAMoveableEntity a, double time) {
		

		this.towardsLinkLastExitTimes.put(nextLink, time);
		this.towardsLinkLastExitAgents.put(nextLink, a);

		this.pollAgent(Double.NaN);

		nextLink.getParticles()[0] = a; 
		a.materialize(0, 1);
		this.towardsLinkLastExitTimes.put(nextLink, time);
		a.moveOverNode(nextLink, time);
		nextLink.fireUpstreamEntered(a, time);

		//check post-condition and generate events
		//first for persons behind
		triggerPrevAgent(time,a);

		//second for oneself
		checkPostConditionForAgentEnteredLinkFromUpstreamEnd(nextLink,a, time);

	}

	private void checkPostConditionForAgentEnteredLinkFromUpstreamEnd(
			CALinkDynamic nextLink, CAMoveableEntity a, double time) {
		CAMoveableEntity inFrontOfMe = nextLink.getParticles()[1];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == -1) { //oncoming
				double d = nextLink.getD(a);
				d *= this.ratio;
				triggerSWAP(a,nextLink,time+d+nextLink.getTFree());
			} 
		} else {
			triggerTTA(a,nextLink,time+nextLink.getTFree());
		}

	}

	private void handleTTAEnterNextLinkFromUpstreamEndOnPrecondition2(
			CALinkDynamic nextLink, CAMoveableEntity a, double time) {
		double z = nextLink.getZ(a);
		z *= this.ratio;
		double zStar = z - (time - nextLink.getLastLeftTimes()[0]);
		double nextTime = time + zStar;
		
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void handleTTAOnPrecondition3(CAMoveableEntity a, double time) {
		// nothing to be done here

	}

	/////////////////////////

	private void handleTTAEnterNextLinkFromDownstreamEnd(CALinkDynamic nextLink, CAMoveableEntity a, double time) {

		//check pre-condition
		if (nextLink.getParticles()[nextLink.getNumOfCells()-1] == null) {
			double z = nextLink.getZ(a);
			z *= this.ratio;
			if (nextLink.getLastLeftTimes()[nextLink.getNumOfCells()-1] <= (time - z+epsilon)){
				handleTTAEnterNextLinkFromDownstreamEndOnPrecondition1(nextLink,a,time);
			} else {
				handleTTAEnterNextLinkFromDownstreamEndOnPrecondition2(nextLink,a,time);
			}
		} else {
			handleTTAOnPrecondition3(a,time);
		}
	}


	private void handleTTAEnterNextLinkFromDownstreamEndOnPrecondition1(
			CALinkDynamic nextLink, CAMoveableEntity a, double time) {
		
		this.towardsLinkLastExitTimes.put(nextLink, time);
		this.towardsLinkLastExitAgents.put(nextLink, a);

		this.pollAgent(Double.NaN);

		nextLink.getParticles()[nextLink.getNumOfCells()-1] = a; 
		a.materialize(nextLink.getNumOfCells()-1, -1);
		this.towardsLinkLastExitTimes.put(nextLink, time);
		a.moveOverNode(nextLink, time);
		nextLink.fireDownstreamEntered(a, time);


		//check post-condition and generate events
		//first for persons behind
		triggerPrevAgent(time,a);

		//second for oneself
		checkPostConditionForAgentEnteredLinkFromDownstreamEnd(nextLink,a, time);

	}

	private void checkPostConditionForAgentEnteredLinkFromDownstreamEnd(
			CALinkDynamic nextLink, CAMoveableEntity a, double time) {
		CAMoveableEntity inFrontOfMe = nextLink.getParticles()[nextLink.getNumOfCells()-2];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == 1) { //oncoming
				double d = nextLink.getD(a);
				d *= this.ratio;
				triggerSWAP(a,nextLink,time+d+nextLink.getTFree());
			} 
		} else {
			triggerTTA(a,nextLink,time+nextLink.getTFree());
		}

	}

	private void handleTTAEnterNextLinkFromDownstreamEndOnPrecondition2(
			CALinkDynamic nextLink, CAMoveableEntity a, double time) {
		double z = nextLink.getZ(a);
		z *= this.ratio;
		double zStar = z - (time - nextLink.getLastLeftTimes()[nextLink.getNumOfCells()-1]);
		double nextTime = time + zStar;
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}


	private void triggerSWAP(CAMoveableEntity a, CANetworkEntity ne, double time) {
		CAEvent e = new CAEvent(time, a, ne, CAEventType.SWAP);
		this.net.pushEvent(e);

	}

	private void triggerTTA(CAMoveableEntity toBeTriggered, CANetworkEntity ne, double time) {
		CAEvent e = new CAEvent(time, toBeTriggered, ne, CAEventType.TTA);
		this.net.pushEvent(e);

	}


	//======================================================

	private void triggerPrevAgent(double time, CAMoveableEntity a) {

		double cap = 0;
		List<Tuple<CALinkDynamic,CAMoveableEntity>> cands = new ArrayList<Tuple<CALinkDynamic,CAMoveableEntity>>();
		for (CALinkDynamic l : this.links) {
			if (l.getDownstreamCANode() == this) {
				if (l.getParticles()[l.getNumOfCells()-1] != null && l.getParticles()[l.getNumOfCells()-1].getDir() == 1) {
					cands.add(new Tuple<CALinkDynamic,CAMoveableEntity>(l,l.getParticles()[l.getNumOfCells()-1]));
					cap += 1;//l.getLink().getCapacity();
				}
			} else if (l.getUpstreamCANode() == this) {
				if (l.getParticles()[0] != null && l.getParticles()[0].getDir() == -1) {
					cands.add(new Tuple<CALinkDynamic,CAMoveableEntity>(l,l.getParticles()[0]));
					cap += 1;//l.getLink().getCapacity();
				}
			}
		}
		if (cands.size() == 0) {
			return;
		}

		double rnd = cap*MatsimRandom.getRandom().nextDouble();
		double incr = cap/cands.size();
		double comp = incr;
		for (Tuple<CALinkDynamic, CAMoveableEntity> t : cands) {
			if (rnd <= comp) {
				double z = t.getFirst().getZ(t.getSecond());
				z *= this.ratio;
				CAEvent e = new CAEvent(time + z, t.getSecond(), t.getFirst(), CAEventType.TTA);
				this.net.pushEvent(e);
				return;
			}
			comp += incr;
		}

		throw new RuntimeException("should be unreachable!");
	}



	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	private void handelSwap(CAMoveableEntity a, double time) {
		Id<Link> nextLinkId = a.getNextLinkId();
		CALinkDynamic nextLink = (CALinkDynamic)this.net.getCALink(nextLinkId);


		if (nextLink.getDownstreamCANode() == this) {
			handleSwapWithDownStreamEnd(a,time,nextLink);
		} else if (nextLink.getUpstreamCANode() == this) {
			handleSwapWithUpStreamEnd(a,time,nextLink);
		} else {
			throw new RuntimeException("Inconsitent network or agent plan!");
		}

	}

	private void handleSwapWithUpStreamEnd(CAMoveableEntity a, double time,
			CALinkDynamic nextLink) {
		
		CAMoveableEntity swapA = nextLink.getParticles()[0];
		this.pollAgent(0);
		this.putAgent(swapA);
		nextLink.getParticles()[0] = a;
		a.materialize(0, 1);
		nextLink.fireUpstreamLeft(swapA, time);
		nextLink.fireUpstreamEntered(a, time);
		a.moveOverNode(nextLink,time);

		nextLink.getLastLeftTimes()[0]=time;
		this.towardsLinkLastExitTimes.put(nextLink, time);

		checkPostConditionForAgentEnteredLinkFromUpstreamEnd(nextLink, a, time);
		checkPostConditionForAgentSwapedToNode(swapA,time);

	}

	private void checkPostConditionForAgentSwapedToNode(CAMoveableEntity swapA,
			double time) {
		Id<Link> nextLinkId = swapA.getNextLinkId();
		CALinkDynamic nextLink = (CALinkDynamic)this.net.getCALink(nextLinkId);
		if (nextLink.getDownstreamCANode() == this) {
			checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromDownstreamEnd(swapA, nextLink, time);
		} else {
			checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd(swapA, nextLink, time);
		}

	}

	private void checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd(
			CAMoveableEntity swapA, CALinkDynamic nextLink, double time) {

		CAMoveableEntity inFrontOfMe = nextLink.getParticles()[0];
		if ( inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == -1) {
				double d = nextLink.getD(swapA);
				d *= this.ratio;
				triggerSWAP(swapA, this, time+d+this.tFree);
			}
		} else  {
			triggerTTA(swapA,this,time+this.tFree);
		}

	}

	private void checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromDownstreamEnd(
			CAMoveableEntity swapA, CALinkDynamic nextLink, double time) {
		CAMoveableEntity inFrontOfMe = nextLink.getParticles()[nextLink.getNumOfCells()-1];
		if ( inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == 1) {
				double d = nextLink.getD(swapA);
				d *= this.ratio;
				triggerSWAP(swapA, this, time+d+this.tFree);
			} 
		} else  {
			triggerTTA(swapA,this,time+this.tFree);
		}
	}

	private void handleSwapWithDownStreamEnd(CAMoveableEntity a,
			double time, CALinkDynamic nextLink) {
		
		CAMoveableEntity swapA = nextLink.getParticles()[nextLink.getNumOfCells()-1];
		this.pollAgent(0);
		this.putAgent(swapA);
		nextLink.getParticles()[nextLink.getNumOfCells()-1] = a;
		a.materialize(nextLink.getNumOfCells()-1, -1);
		nextLink.fireDownstreamLeft(swapA, time);
		nextLink.fireDownstreamEntered(a, time);
		a.moveOverNode(nextLink,time);

		nextLink.getLastLeftTimes()[nextLink.getNumOfCells()-1]=time;
		this.towardsLinkLastExitTimes.put(nextLink, time);
		
		// check post-conditions & new events
		checkPostConditionForAgentEnteredLinkFromDownstreamEnd(nextLink, a, time);
		checkPostConditionForAgentSwapedToNode(swapA,time);
	}

	@Override
	public Node getNode() {
		return this.node;
	}

	public double getLastNodeExitTimeForAgent(CAMoveableEntity a) {
		Id<Link> n = a.getNextLinkId();
		CALinkDynamic nextLink = (CALinkDynamic)this.net.getCALink(n);
		return this.towardsLinkLastExitTimes.get(nextLink);
	}

	@Override
	public List<CALinkDynamic> getLinks() {
		return this.links;
	}

}
