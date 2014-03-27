/* *********************************************************************** *
 * project: org.matsim.*
 * CANetwork.java
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

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;


public class CANetwork {
	
	
	public static double RHO = 1;
	
	private static final Logger log = Logger.getLogger(CANetwork.class);
	
	private final PriorityQueue<CAEvent> events = new PriorityQueue<CAEvent>();
	private final Network net;
	
	private final Map<Id,CANode> caNodes = new HashMap<Id,CANode>();
	private final Map<Id,CALink> caLinks = new HashMap<Id,CALink>();
	private final EventsManager em;
	
	private double globalTime;
	
	private final long eventCnt = 0;
	
	public CANetwork(Network net, EventsManager em) {
		this.net = net;
		this.em = em;
		init();
	}
	
	private void init() {
		for (Node n : this.net.getNodes().values()) {
			CANode caNode = new CANode(n, this);
			this.caNodes.put(n.getId(), caNode);
		}
		for (Link l : this.net.getLinks().values()) {
			CANode us = this.caNodes.get(l.getFromNode().getId());
			CANode ds = this.caNodes.get(l.getToNode().getId());
			Link rev = null;
			for (Link ll : l.getToNode().getOutLinks().values()) {
				if (ll.getToNode() == l.getFromNode()) {
					rev = ll;
				}
			}
			CALink revCA = this.caLinks.get(rev.getId());
			if (revCA != null){
				this.caLinks.put(l.getId(), revCA);
				continue;
			}
			
			CALink caL = new CALink(l,rev, ds, us, this);
			us.addLink(caL);
			ds.addLink(caL);
			this.caLinks.put(l.getId(), caL);
		}
	}

	public void runUntil(double time) {
		this.globalTime = this.events.peek().getEventExcexutionTime();
		while (this.events.size() > 0 && this.events.peek().getEventExcexutionTime() < time) {
			CAEvent e = this.events.poll();
			if (e.getCANetworkEntity() instanceof CALink && e.getCANetworkEntity() != ((CASimpleAgent)e.getCAAgent()).getCurrentLink()){
				log.info("dropping inconsisten event");
				continue;
			}
			
			e.getCANetworkEntity().handleEvent(e);
			
			if (e.getEventExcexutionTime() > this.globalTime) {
				draw();
				this.globalTime = e.getEventExcexutionTime();
			}
		}
	}
	
	
	/*package*/ void run() {
		this.globalTime = this.events.peek().getEventExcexutionTime();
		while (this.events.size() > 0) {
			CAEvent e = this.events.poll();
//			if ((e.getCAAgent().getId().toString().equals("-1") || e.getCAAgent().getId().toString().equals("492"))&& this.globalTime >= 22.){
//				System.out.println("--> " + e);
//				checkBlockingOnLink();
//			}
//			checkConsitency();
//			
//			System.out.println("DEBUG");
//			if (e.getEventExcexutionTime() >= 309.38471064716487 && e.getCAAgent().getId().toString().equals("46")) {
//				System.out.println("DEBUG");
//				
//			}
			
			if (e.getCANetworkEntity() instanceof CALink && e.getCANetworkEntity() != ((CASimpleAgent)e.getCAAgent()).getCurrentLink()){
				log.info("dropping inconsisten event");
				continue;
			}
			
			e.getCANetworkEntity().handleEvent(e);
			
//			checkConsitency();

//			System.out.println(e);
			
//			if (e.getEventExcexutionTime() > this.globalTime+0.04) {
//				
//				draw();
//				this.globalTime = e.getEventExcexutionTime();
//			}
		}
	}
	
	private void checkBlockingOnLink() {
		
		System.out.println("DEBUG");
		for (CALink l : this.caLinks.values()) {
			for (int i = 1; i < l.getNumOfCells()-1; i++) {
				CAAgent p = l.getParticles()[i];
				if (p != null) {
					boolean isBlocking = false;
					int nextIdx = p.getPos()+p.getDir();
					if (l.getParticles()[nextIdx] == null) {
						isBlocking = true;
						for (CAEvent e : this.events) {
							if (e.getCAAgent() == p && e.getCAEventType() == CAEventType.TTA){
								isBlocking = false;
								break;
							}
						}
					}
					if (isBlocking) {
						System.out.println("BLOCKER");
					}
				}
			}
		}
	}
	
	private void checkConsitency() {
		System.out.println("DEBUG");
		for (CALink l : this.caLinks.values()) {
			for (int i = 0; i < l.getNumOfCells(); i++) {
				CAAgent p = l.getParticles()[i];
				if (p != null) {
					if (p.getPos() != i) {
						throw new RuntimeException();
					}
				}
			}
		}
		
	}

	private void draw() {
		for (CALink l : this.caLinks.values()) {
			double dx = l.getLink().getToNode().getCoord().getX()-l.getLink().getFromNode().getCoord().getX();
			double dy = l.getLink().getToNode().getCoord().getY()-l.getLink().getFromNode().getCoord().getY();
			double length = Math.sqrt(dx*dx+dy*dy);
			dx /= length;
			dy /= length;
			double incr = 1/(5.091*l.getLink().getCapacity());
			dx *= incr;
			dy *= incr;
			double x = l.getLink().getFromNode().getCoord().getX()+dx/2;
			double y = l.getLink().getFromNode().getCoord().getY()+dy/2;
			for (int i = 0; i < l.getNumOfCells(); i++) {
				if (l.getParticles()[i]!= null) {
					XYVxVyEventImpl e = new XYVxVyEventImpl(l.getParticles()[i].getId(), x, y, l.getParticles()[i].getDir(), l.getParticles()[i].getDir(), this.globalTime);
					this.em.processEvent(e);
				}
				x+=dx;
				y+=dy;
			}
		}
		for (CANode n : this.caNodes.values()) {
			if (n.peekForAgent() != null) {
				double x = n.getNode().getCoord().getX();
				double y = n.getNode().getCoord().getY();
				XYVxVyEventImpl e = new XYVxVyEventImpl(n.peekForAgent().getId(), x, y, 1, 1, this.globalTime);
				this.em.processEvent(e);
			}
		}
		
	}

	public void pushEvent(CAEvent event) {
//		System.out.println("DEBUG");
//		if (event.getCANetworkEntity() instanceof CALink) {
//			if (((CASimpleAgent)event.getCAAgent()).getCurrentLink() != event.getCANetworkEntity()){
//				throw new RuntimeException("bug!");
//			}
//		} else if (event.getEventExcexutionTime() >= 309.3847106471648){
//			System.out.println("DEBUG");	
//		}
//		event.cnt = this.eventCnt++;
//		if (event.cnt == 53943){
//			System.out.println("DEBUG");
//		}
//		System.out.println("<-- " + event);
//		if (event.getCAAgent().getId().toString().equals("36")){
//			System.out.println("<-- " + event);
//		}
		
//		if ((event.getCAAgent().getId().toString().equals("-24") || event.getCAAgent().getId().toString().equals("-24"))&& event.getEventExcexutionTime() >= 1387.78){
//			System.out.println("<-- " + event);
//		}
		
		this.events.add(event);
	}
	
	public CAEvent pollEvent() {
		return this.events.poll();
	}
	
	public CAEvent peekEvent() {
		return this.events.peek();
	}

	public CALink getCALink(Id nextLinkId) {
		return this.caLinks.get(nextLinkId);
	}

	public CANode getCANode(Id id) {
		return this.caNodes.get(id);
	}
	
	public EventsManager getEventsManager() {
		return this.em;
	}

}
