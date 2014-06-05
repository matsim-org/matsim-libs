/* *********************************************************************** *
 * project: org.matsim.*
 * CALinkMonitor.java
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

package playground.gregor.casim.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.events.CASimAgentConstructEventHandler;
import playground.gregor.casim.simulation.physics.CASimpleAgent;

public class CALinkMonitor implements LinkEnterEventHandler,
		LinkLeaveEventHandler, CASimAgentConstructEventHandler {
	
	private static final long WARMUP = 100;
	int dsCnt;
	int usCnt;
	long left;
	private final Id ds;
	private final Id us;
	
	private final Map<Id,AgentInfo> onLink = new HashMap<Id,AgentInfo>();
	
	private final List<Measure> measures = new ArrayList<Measure>();
	private Measure current = new Measure();
	private final Link link;
	
	public CALinkMonitor(Id ds, Id us, Link link) {
		this.us = us;
		this.ds = ds;
		this.link = link;
	}

	@Override
	public void reset(int iteration) {
		this.measures.add(this.current);
		this.current = new Measure();
		this.dsCnt = 0;
		this.usCnt = 0;
		this.left = 0;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		AgentInfo ai;
		if (event.getLinkId() == this.ds){
			ai = this.onLink.remove(event.getVehicleId());
			if (ai == null){
				return;
			}
			this.left++;
			this.dsCnt--;
			
		} else if (event.getLinkId() == this.us){
			ai = this.onLink.remove(event.getVehicleId());
			if (ai == null){
				return;
			}
			this.left++;
			this.usCnt--;
		} else {
			return;
		}
		double time = event.getTime() - ai.e.getTime();
		double speed = this.link.getLength()/time;
		double rho = (this.dsCnt+this.usCnt)/(this.link.getLength())/this.link.getCapacity();

		double flow = ((this.left-ai.left-1)/time);///this.caLink.getLink().getCapacity();
		double flowComp = rho * speed;
		
		if (event.getTime() < WARMUP) {
			return;
		}
		
		if (this.current.cnt == 0) {
			this.current.flow = flow;
			this.current.rho = rho;
			this.current.speed = speed;
			this.current.cnt++;
		} else {
			this.current.flow = this.current.cnt/(this.current.cnt+1.) * this.current.flow + 1./(this.current.cnt+1.)*flow;
			this.current.rho = this.current.cnt/(this.current.cnt+1.) * this.current.rho + 1./(this.current.cnt+1.)*rho;
			this.current.speed = this.current.cnt/(this.current.cnt+1.) * this.current.speed + 1./(this.current.cnt+1.)*speed;
			this.current.cnt++;
		}
//		System.out.println("dsCnt:" + this.dsCnt +" usCnt:" + this.usCnt + " total:" + (this.dsCnt+this.usCnt) + " speed:" + speed + " flow:" + flow + " rho:" + rho + " flowComp:" + flowComp);
	}
	
	
	@Override
	public void handleEvent(CASimAgentConstructEvent e) {
		CASimpleAgent a = (CASimpleAgent) e.getCAAgent();
		
		if (a.getCurrentLink().getLink().getId() != this.ds) {
			return;
		}
		
		
		
		if (a.getDir() == 1) {
			AgentInfo ai = new AgentInfo();
			ai.e = e;
			ai.left = 0;
			this.onLink.put(a.getId(), ai);
			this.dsCnt++;
		} else if (a.getDir() == -1){
			AgentInfo ai = new AgentInfo();
			ai.e = e;
			ai.left = 0;
			this.onLink.put(a.getId(), ai);
			this.usCnt++;			
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId() == this.ds){
			AgentInfo ai = new AgentInfo();
			ai.e = event;
			ai.left = this.left;
			this.onLink.put(event.getVehicleId(), ai);
			this.dsCnt++;
		} else if (event.getLinkId() == this.us){
			AgentInfo ai = new AgentInfo();
			ai.e = event;
			ai.left = this.left;
			this.onLink.put(event.getVehicleId(), ai);
			this.usCnt++;
		}
//		System.out.println("dsCnt:" + this.dsCnt +" usCnt:" + this.usCnt + " total:" + (this.dsCnt+this.usCnt));
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Measure m : this.measures) {
			buf.append(m.rho + " " + m.flow + " " + m.speed + "\n");
		}
		return buf.toString();
	}
	
	private static final class AgentInfo {
		long left;
		Event e;
	}

	private static final class Measure {
		public double speed;
		double cnt = 0; 
		double flow = 0;
		double rho = 0;
	}

	

}
