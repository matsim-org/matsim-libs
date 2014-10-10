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



	private CAAgent agent;
	private final Node node;
	private final CANetworkDynamic net;

	private final List<CALinkDynamic> links = new ArrayList<CALinkDynamic>();

	private final Map<CALink,Double> towardsLinkLastExitTimes = new HashMap<CALink,Double>();
	private final Map<CALink,CASimpleDynamicAgent> towardsLinkLastExitAgents = new HashMap<CALink,CASimpleDynamicAgent>();

	private final double width;

	private double agentLeft;

	private final double tFree;

	private final double cellLength;



	private final double ratio;



	private static int EXP_WARN_CNT;

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
//		this.tFree = 0.0001;
//		this.ratio = 0.0001;
		
//		double bwCellLength = 1/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.PED_WIDTH);
		
		
//		this.ratio = 1;
		
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
	public void putAgent(CAAgent a) {
		if (this.agent != null) {
			throw new RuntimeException("There is already an agent on node:" + this.node.getId());

		}
		this.agent = a;
		a.moveToNode(this);
	}
	@Override
	public CAAgent peekForAgent() {
		return this.agent;
	}

	@Override
	public CAAgent pollAgent(double time) {
		CAAgent a = this.agent;
		this.agent = null;
		this.agentLeft = time;
		return a;
	}

//	public double getTime() {
//		//		return this.agentLeft;
//		throw new RuntimeException("this method must return last enter time and not exit time!");
//
//	}

	@Override
	public void handleEvent(CAEvent e) {
		CAAgent a = e.getCAAgent();
		if (!(a instanceof CASimpleDynamicAgent)) {
			throw new RuntimeException("Cannot handle agent+" + a+ " only instances of" + CASimpleDynamicAgent.class.getName() + " are allowd");
		}

		CASimpleDynamicAgent dyna = (CASimpleDynamicAgent)a;

		double time = e.getEventExcexutionTime();
		if (e.getCAEventType() == CAEventType.SWAP){
			handelSwap(dyna,time);
		} else if (e.getCAEventType() == CAEventType.TTA){
			handleTTA(dyna,time);
		} else {
			throw new RuntimeException("Unknown event type: " + e.getCAEventType());
		}
	}

	private void handleTTA(CASimpleDynamicAgent a, double time) {

		Id nextLinkId = a.getNextLinkId();
		CALinkDynamic nextLink = (CALinkDynamic)this.net.getCALink(nextLinkId);

		if (nextLink.getDownstreamCANode() == this) {
			handleTTAEnterNextLinkFromDownstreamEnd(nextLink,a,time);
		} else if (nextLink.getUpstreamCANode() == this) {
			handleTTAEnterNextLinkFromUpstreamEnd(nextLink,a,time);
		} else {
			throw new RuntimeException("Inconsitent network or agent plan!");
		}


	}

	private void handleTTAEnterNextLinkFromUpstreamEnd(CALinkDynamic nextLink, CASimpleDynamicAgent a, double time) {

		//check pre-condition
		if (nextLink.getParticles()[0] == null) {
//			if (nextLink.getDsLastLeftTimes()[0] <= (time - a.getZ()/nextLink.getWidth())){
			double z = nextLink.getZ();
//			z *= CANetworkDynamic.PED_WIDTH;
			z *= this.ratio;
			if (nextLink.getDsLastLeftTimes()[0] <= (time - z)){
				handleTTAEnterNextLinkFromUpstreamEndOnPrecondition1(nextLink,a,time);
			} else {
				handleTTAEnterNextLinkFromUpstreamEndOnPrecondition2(nextLink,a,time);
			}
		} else {
			handleTTAOnPrecondition3(a,time);
		}
		//		

	}


	private void handleTTAEnterNextLinkFromUpstreamEndOnPrecondition1(
			CALinkDynamic nextLink, CASimpleDynamicAgent a, double time) {
		
//		//HACK break condition agent is at the end of its last link
//		if (nextLink.getLink().getId() == a.getCurrentLink().getLink().getId()) {
//			this.pollAgent(Double.NaN);	
//			return;
//		}

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
			CALinkDynamic nextLink, CASimpleDynamicAgent a, double time) {
		CAAgent inFrontOfMe = nextLink.getParticles()[1];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == -1) { //oncoming
				double d = nextLink.getD();
//				d *= CANetworkDynamic.PED_WIDTH;
				d *= this.ratio;
//				triggerSWAP(a,nextLink,time+d/nextLink.getWidth()+nextLink.getTFree());
				triggerSWAP(a,nextLink,time+d+nextLink.getTFree());
			} else {
				triggerTTA(a,nextLink,time+nextLink.getTFree());
			}
		} else {
			triggerTTA(a,nextLink,time+nextLink.getTFree());
		}

	}

	private void handleTTAEnterNextLinkFromUpstreamEndOnPrecondition2(
			CALinkDynamic nextLink, CASimpleDynamicAgent a, double time) {
//		double zStar = a.getZ()/this.width - (time - nextLink.getDsLastLeftTimes()[0]);
		double z = nextLink.getZ();
//		z *= CANetworkDynamic.PED_WIDTH;
		z *= this.ratio;
		double zStar = z - (time - nextLink.getDsLastLeftTimes()[0]);
		double nextTime = time + zStar;
		if (!(nextTime > time)) {
			nextTime += 0.0001;
		}
		
		
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}

	private void handleTTAOnPrecondition3(CASimpleDynamicAgent a, double time) {
		// nothing to be done here

	}

	/////////////////////////

	private void handleTTAEnterNextLinkFromDownstreamEnd(CALinkDynamic nextLink, CASimpleDynamicAgent a, double time) {

		//check pre-condition
		if (nextLink.getParticles()[nextLink.getNumOfCells()-1] == null) {
//			if (nextLink.getUsLastLeftTimes()[nextLink.getNumOfCells()-1] <= (time - a.getZ()/nextLink.getWidth())){
			double z = nextLink.getZ();
//			z *= CANetworkDynamic.PED_WIDTH;
			z *= this.ratio;
			if (nextLink.getUsLastLeftTimes()[nextLink.getNumOfCells()-1] <= (time - z)){
				handleTTAEnterNextLinkFromDownstreamEndOnPrecondition1(nextLink,a,time);
			} else {
				handleTTAEnterNextLinkFromDownstreamEndOnPrecondition2(nextLink,a,time);
			}
		} else {
			handleTTAOnPrecondition3(a,time);
		}
		//		

	}


	private void handleTTAEnterNextLinkFromDownstreamEndOnPrecondition1(
			CALinkDynamic nextLink, CASimpleDynamicAgent a, double time) {
		
//		//HACK break condition agent is at the end of its last link
//		if (nextLink.getLink().getId() == a.getCurrentLink().getLink().getId()) {
//			this.pollAgent(Double.NaN);	
//			return;
//		}

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
			CALinkDynamic nextLink, CASimpleDynamicAgent a, double time) {
		CAAgent inFrontOfMe = nextLink.getParticles()[nextLink.getNumOfCells()-2];
		if (inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == 1) { //oncoming
				double d = nextLink.getD();
//				d *= CANetworkDynamic.PED_WIDTH;
				d *= this.ratio;
//				triggerSWAP(a,nextLink,time+d/nextLink.getWidth()+nextLink.getTFree());
				triggerSWAP(a,nextLink,time+d+nextLink.getTFree());
			} else {
				triggerTTA(a,nextLink,time+nextLink.getTFree());
			}
		} else {
			triggerTTA(a,nextLink,time+nextLink.getTFree());
		}

	}

	private void handleTTAEnterNextLinkFromDownstreamEndOnPrecondition2(
			CALinkDynamic nextLink, CASimpleDynamicAgent a, double time) {
//		double zStar = a.getZ()/this.width - (time - nextLink.getUsLastLeftTimes()[nextLink.getNumOfCells()-1]);
		double z = nextLink.getZ();
//		z *= CANetworkDynamic.PED_WIDTH;
		z *= this.ratio;
		double zStar = z - (time - nextLink.getUsLastLeftTimes()[nextLink.getNumOfCells()-1]);
		double nextTime = time + zStar;
		if (!(nextTime > time)) {
			nextTime += 0.0001;
		}
		CAEvent e = new CAEvent(nextTime, a, this, CAEventType.TTA);
		this.net.pushEvent(e);
	}


	private void triggerSWAP(CAAgent a, CANetworkEntity ne, double time) {
		CAEvent e = new CAEvent(time, a, ne, CAEventType.SWAP);
		this.net.pushEvent(e);

	}

	private void triggerTTA(CAAgent toBeTriggered, CANetworkEntity ne, double time) {
		CAEvent e = new CAEvent(time, toBeTriggered, ne, CAEventType.TTA);
		this.net.pushEvent(e);

	}


	//======================================================

	private void triggerPrevAgent(double time, CASimpleDynamicAgent a) {

		double cap = 0;
		List<Tuple<CALinkDynamic,CAAgent>> cands = new ArrayList<Tuple<CALinkDynamic,CAAgent>>();
		for (CALinkDynamic l : this.links) {
			if (l.getDownstreamCANode() == this) {
				if (l.getParticles()[l.getNumOfCells()-1] != null && l.getParticles()[l.getNumOfCells()-1].getDir() == 1) {
					cands.add(new Tuple<CALinkDynamic,CAAgent>(l,l.getParticles()[l.getNumOfCells()-1]));
					cap += l.getLink().getCapacity();
				}
			} else if (l.getUpstreamCANode() == this) {
				if (l.getParticles()[0] != null && l.getParticles()[0].getDir() == -1) {
					cands.add(new Tuple<CALinkDynamic,CAAgent>(l,l.getParticles()[0]));
					cap += l.getLink().getCapacity();
				}
			}
		}
		if (cands.size() == 0) {
			return;
		}

		double rnd = cap*MatsimRandom.getRandom().nextDouble();
		double incr = cap/cands.size();
		double comp = incr;
		for (Tuple<CALinkDynamic, CAAgent> t : cands) {
			if (rnd <= comp) {
				//				CAEvent e = new CAEvent(time + this.z +  1/(this.vHat*this.rhoHat), t.getSecond(), t.getFirst(), CAEventType.TTA);
//				double z = a.getZ();
//				double z = t.getSecond().getZ();
				double z = t.getFirst().getZ();
//				z *= CANetworkDynamic.PED_WIDTH;
				z *= this.ratio;
//				double width = t.getFirst().getWidth();
//				CAEvent e = new CAEvent(time + z/width, t.getSecond(), t.getFirst(), CAEventType.TTA);
				CAEvent e = new CAEvent(time + z, t.getSecond(), t.getFirst(), CAEventType.TTA);
				this.net.pushEvent(e);
				return;
			}
			comp += incr;
		}

		throw new RuntimeException("should be unreachable!");
	}



	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	private void handelSwap(CASimpleDynamicAgent a, double time) {
		Id nextLinkId = a.getNextLinkId();
		CALinkDynamic nextLink = (CALinkDynamic)this.net.getCALink(nextLinkId);


		if (nextLink.getDownstreamCANode() == this) {
			handleSwapWithDownStreamEnd(a,time,nextLink);
		} else if (nextLink.getUpstreamCANode() == this) {
			handleSwapWithUpStreamEnd(a,time,nextLink);
		} else {
			throw new RuntimeException("Inconsitent network or agent plan!");
		}

	}

	private void handleSwapWithUpStreamEnd(CASimpleDynamicAgent a, double time,
			CALinkDynamic nextLink) {
		
		CAAgent swapA = nextLink.getParticles()[0];
		this.pollAgent(0);
		this.putAgent(swapA);
		nextLink.getParticles()[0] = a;
		a.materialize(0, 1);
		nextLink.fireUpstreamLeft(swapA, time);
		nextLink.fireUpstreamEntered(a, time);
		a.moveOverNode(nextLink,time);

		nextLink.getDsLastLeftTimes()[0]=time;
		this.towardsLinkLastExitTimes.put(nextLink, time);

		//TODO check post-conditions & new events
		checkPostConditionForAgentEnteredLinkFromUpstreamEnd(nextLink, a, time);
		checkPostConditionForAgentSwapedToNode(swapA,time);

	}

	private void checkPostConditionForAgentSwapedToNode(CAAgent swapA,
			double time) {
		Id nextLinkId = swapA.getNextLinkId();
		CALinkDynamic nextLink = (CALinkDynamic)this.net.getCALink(nextLinkId);
		if (nextLink.getDownstreamCANode() == this) {
			checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromDownstreamEnd(swapA, nextLink, time);
		} else {
			checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd(swapA, nextLink, time);
		}

	}

	private void checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromUpstreamEnd(
			CAAgent swapA, CALinkDynamic nextLink, double time) {

		CAAgent inFrontOfMe = nextLink.getParticles()[0];
		if ( inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == -1) {
				double d = nextLink.getD();
//				d *= CANetworkDynamic.PED_WIDTH;
				d *= this.ratio;
//				triggerSWAP(swapA, this, time+d/this.width+this.tFree);
				triggerSWAP(swapA, this, time+d+this.tFree);
			} else {
				triggerTTA(swapA,this,time+this.tFree);
			}
		} else  {
			triggerTTA(swapA,this,time+this.tFree);
		}

	}

	private void checkPostConditionForAgentSwapedToNodeAndWantsToEnterNextLinkFromDownstreamEnd(
			CAAgent swapA, CALinkDynamic nextLink, double time) {
		CAAgent inFrontOfMe = nextLink.getParticles()[nextLink.getNumOfCells()-1];
		if ( inFrontOfMe != null) {
			if (inFrontOfMe.getDir() == 1) {
				double d = nextLink.getD();
//				d *= CANetworkDynamic.PED_WIDTH;
				d *= this.ratio;
//				triggerSWAP(swapA, this, time+d/this.width+this.tFree);
				triggerSWAP(swapA, this, time+d+this.tFree);
			} else {
				triggerTTA(swapA,this,time+this.tFree);
			}
		} else  {
			triggerTTA(swapA,this,time+this.tFree);
		}
	}

	private void handleSwapWithDownStreamEnd(CASimpleDynamicAgent a,
			double time, CALinkDynamic nextLink) {
		
		CAAgent swapA = nextLink.getParticles()[nextLink.getNumOfCells()-1];
		this.pollAgent(0);
		this.putAgent(swapA);
		nextLink.getParticles()[nextLink.getNumOfCells()-1] = a;
		a.materialize(nextLink.getNumOfCells()-1, -1);
		nextLink.fireDownstreamLeft(swapA, time);
		nextLink.fireDownstreamEntered(a, time);
		a.moveOverNode(nextLink,time);

		nextLink.getDsLastLeftTimes()[nextLink.getNumOfCells()-1]=time;
		this.towardsLinkLastExitTimes.put(nextLink, time);
		
		// check post-conditions & new events
		checkPostConditionForAgentEnteredLinkFromDownstreamEnd(nextLink, a, time);
		checkPostConditionForAgentSwapedToNode(swapA,time);
	}

	@Override
	public Node getNode() {
		return this.node;
	}

	public double getLastNodeExitTimeForAgent(CASimpleDynamicAgent a) {
		Id n = a.getNextLinkId();
		CALinkDynamic nextLink = (CALinkDynamic)this.net.getCALink(n);
		return this.towardsLinkLastExitTimes.get(nextLink);
	}

}
