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
import org.matsim.core.gbl.Gbl;

import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.debug.RectEvent;


public class CANetworkDynamic {


	public static double RHO = 1;


	//Floetteroed Laemmel parameters
	public static final double RHO_HAT = 6.69;
	public static final double V_HAT = 1.27;
//		public static final double RHO_HAT = 5.09;
//		public static final double V_HAT = 1.26;

	public static final double ALPHA = 0;
	public static final double BETA = 0.39;
	public static final double GAMMA = 1.43;

	
	
	public static final double PED_WIDTH = .61;
	public static final double MAX_Z = ALPHA + BETA * Math.pow(RHO_HAT,GAMMA) + 1/(RHO_HAT*V_HAT);;

	private static final Logger log = Logger.getLogger(CANetworkDynamic.class);

	private final PriorityQueue<CAEvent> events = new PriorityQueue<CAEvent>();
	private final Network net;

	private final Map<Id,CANode> caNodes = new HashMap<Id,CANode>();
	private final Map<Id,CALink> caLinks = new HashMap<Id,CALink>();
	private final EventsManager em;

	private double globalTime = 0;

	private final long eventCnt = 0;




	private final DensityObserver densityObserver;


	private static int EXP_WARN_CNT;

	public CANetworkDynamic(Network net, EventsManager em) {
		this.net = net;
		this.em = em;
		this.densityObserver = new DensityObserver(em);
		init();
	}

	
	private void init() {
		for (Node n : this.net.getNodes().values()) {
			CANodeDynamic caNode = new CANodeDynamic(n, this);
			this.caNodes.put(n.getId(), caNode);
		}
		for (Link l : this.net.getLinks().values()) {
			CANodeDynamic us = (CANodeDynamic) this.caNodes.get(l.getFromNode().getId());
			CANodeDynamic ds = (CANodeDynamic) this.caNodes.get(l.getToNode().getId());
			Link rev = null;
			for (Link ll : l.getToNode().getOutLinks().values()) {
				if (ll.getToNode() == l.getFromNode()) {
					rev = ll;
				}
			}
			if (rev != null) {
				CALink revCA = this.caLinks.get(rev.getId());
				if (revCA != null){
					this.caLinks.put(l.getId(), revCA);
					continue;
				}
			}
			CALinkDynamic caL = new CALinkDynamic(l,rev, ds, us, this);
			this.densityObserver.registerCALink(caL);
			us.addLink(caL);
			ds.addLink(caL);
			this.caLinks.put(l.getId(), caL);
		}
	}
	
	
	public double getRho(CALinkDynamic l) {
		return this.densityObserver.getRho(l.getDownstreamLink().getId());
	}

	public void runUntil(double time) {

		while (this.events.size() > 0 && this.events.peek().getEventExcexutionTime() < time) {
			CAEvent e = this.events.poll();
//			System.out.println(e.getCAAgent() + " " + e);
			if (e.isObsolete()){
				if (EXP_WARN_CNT++ < 10 ) {
					log.info("dropping obsolete event: " + e);
					if (EXP_WARN_CNT == 10) {
						log.info(Gbl.FUTURE_SUPPRESSED);
					}
				}
				continue;
			}

			e.getCANetworkEntity().handleEvent(e);

			if (e.getEventExcexutionTime() > this.globalTime) {
				draw2();
				this.globalTime = e.getEventExcexutionTime();
			}
		}
	}


	/*package*/ void run() {
		this.globalTime = this.events.peek().getEventExcexutionTime();
		while (this.events.size() > 0) {
			CAEvent e = this.events.poll();
			
		
			
			
			if (e.isObsolete()){
				if (EXP_WARN_CNT++ < 10 ) {
					log.info("dropping obsolete event: " + e);
					if (EXP_WARN_CNT == 10) {
						log.info(Gbl.FUTURE_SUPPRESSED);
					}
				}
				continue;
			}
			
//			if (e.getCAAgent().getId().toString().equals("-3895") || e.getCAAgent().getId().toString().equals("1923")) {
//				System.out.println("got you!!!");
//			}
//			
//			if (e.toString().equals("time:25.721895573043653 a:a:1917 TTA")) {
//				System.out.println("got you!!!");
//			}	
			//			System.out.println(e);
			//			if (Double.isNaN(e.getEventExcexutionTime())){
			//				System.out.println("got you!!!");
			//			}
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
			//			256.356023511673

			if (e.isObsolete()){
				log.info("dropping obsolete event: " + e);
				continue;
			}


			//			System.out.println(e.getEventExcexutionTime());
			//			if (e.getEventExcexutionTime() <= ((CASimpleDynamicAgent)e.getCAAgent()).getLastEnterTime() ) {
			//				log.info("dropping inconsisten event");
			//				continue;
			//			}
//			CALink l = this.getCALink(new IdImpl("2"));
//			boolean aThereBefore = false;
//			if (l.getParticles()[162] != null && l.getParticles()[162].getId().toString().equals("-3895")) {
//				aThereBefore = true;
//			}
			e.getCANetworkEntity().handleEvent(e);
			
//			//DEBUG
//			if (aThereBefore && l.getParticles()[162] == null) {
//				System.out.println("got you!!!");
//			}
			
			
			//			checkConsitency();

			//			System.out.println(e);

			if (CASimDynamicExperiment_ZhangJ2011.VIS && e.getEventExcexutionTime() > this.globalTime+0.04) {

				draw2();
				this.globalTime = e.getEventExcexutionTime();
			}
		}
	}


	private void draw2() {
		for (CALink l : this.caLinks.values()) {
			double dx = l.getLink().getToNode().getCoord().getX()-l.getLink().getFromNode().getCoord().getX();
			double dy = l.getLink().getToNode().getCoord().getY()-l.getLink().getFromNode().getCoord().getY();
			double length = Math.sqrt(dx*dx+dy*dy);
			dx /= length;
			dy /= length;
			double incr = l.getLink().getLength()/l.getNumOfCells();
			dx *= incr;
			dy *= incr;
			double width =l.getLink().getCapacity();
			double x = l.getLink().getFromNode().getCoord().getX();//+dx/2;
			double y = l.getLink().getFromNode().getCoord().getY();//+dy/2;
			for (int i = 0; i < l.getNumOfCells(); i++) {
				if (l.getParticles()[i]!= null) {
					double ddx = 1;
					double ddy = 0;
					if (l.getParticles()[i].getDir() == -1) {
						ddx = -1;
					};
					XYVxVyEventImpl e = new XYVxVyEventImpl(l.getParticles()[i].getId(), x+dx/2, y+dy/2, ddx, ddy, this.globalTime);
					
					this.em.processEvent(e);
//					System.out.println(l.getParticles()[i]);
				} else {
					RectEvent e = new RectEvent(this.globalTime, x, y+width/2, dx, width, false);
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
				XYVxVyEventImpl e = new XYVxVyEventImpl(n.peekForAgent().getId(), x, y, 0, 0, this.globalTime);
				this.em.processEvent(e);
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
			double incr = l.getLink().getLength()/l.getNumOfCells();
			dx *= incr;
			dy *= incr;
			double x = l.getLink().getFromNode().getCoord().getX()+dx/2;
			double y = l.getLink().getFromNode().getCoord().getY()+dy/2;
			for (int i = 0; i < l.getNumOfCells(); i++) {
				if (l.getParticles()[i]!= null) {
					double ddx = 1;
					double ddy = 0;
					if (l.getParticles()[i].getDir() == -1) {
						ddx = -1;
					};
					XYVxVyEventImpl e = new XYVxVyEventImpl(l.getParticles()[i].getId(), x, y, ddx, ddy, this.globalTime);
					
					this.em.processEvent(e);
//					System.out.println(l.getParticles()[i]);
				}
				x+=dx;
				y+=dy;
			}
		}
		for (CANode n : this.caNodes.values()) {
			if (n.peekForAgent() != null) {
				double x = n.getNode().getCoord().getX();
				double y = n.getNode().getCoord().getY();
				XYVxVyEventImpl e = new XYVxVyEventImpl(n.peekForAgent().getId(), x, y, 0, 0, this.globalTime);
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

		//		if (event.getEventExcexutionTime() <= ((CASimpleDynamicAgent)event.getCAAgent()).getLastEnterTime()) {
		//			throw new RuntimeException("a");
		//		}

//		if (event.toString().equals("time:25.783212202329086 a:a:1923 SWAP")) {
//			System.out.println("got you!!!");
//		}
//		if (event.toString().equals("time:13.547189434599161 type:TTA obsolete:false")) {
//			System.out.println("got you!!!");
//		}		
		
//		CANetworkEntity e = event.getCANetworkEntity();
//		if (e instanceof CALinkDynamic) {
//			if (((CALinkDynamic) e).getParticles()[event.getCAAgent().getPos()] != event.getCAAgent() && event.getEventExcexutionTime() > 2){
//				System.out.println("got you!!!");
//			}
//		}

		event.getCAAgent().setCurrentEvent(event);
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
