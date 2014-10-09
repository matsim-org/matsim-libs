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

import java.io.BufferedWriter;
import java.io.IOException;
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
import playground.gregor.casim.simulation.physics.CAAgent;
import playground.gregor.casim.simulation.physics.CALinkDynamic;
import playground.gregor.casim.simulation.physics.CASimpleAgent;
import playground.gregor.casim.simulation.physics.CASimpleDynamicAgent;

public class CALinkMonitorIII implements LinkEnterEventHandler,
LinkLeaveEventHandler, CASimAgentConstructEventHandler {

	private  long warmup = 0;
	private  long warmdown = 2000;
	int dsCnt;
	int usCnt;
	long dsLeft;
	long usLeft;
	long left;
	private final Id ds;
	private final Id us;

	private final Map<Id,AgentInfo> onLink = new HashMap<Id,AgentInfo>();

	private final List<Measure> measures = new ArrayList<Measure>();
	private final List<Measure> tmpM = new ArrayList<Measure>(); 
	private Measure current = new Measure();
	private final Link link;
	private double rho40 = 0;
	private double rhoMSA = 0;
	private double msaCnt = 0;
	private boolean startet = false;

	double sampleSize = 0.02;


	private final List<AgentMeasure> ams = new ArrayList<AgentMeasure>();
	private CALinkDynamic caL;

	public CALinkMonitorIII(Id ds, Id us, Link link) {
		this.us = us;
		this.ds = ds;
		this.link = link;
	}

	@Override
	public void reset(int iteration) {
		this.current = new Measure();
		this.dsCnt = 0;
		this.usCnt = 0;
		this.left = 0;
		this.onLink.clear();
		this.tmpM.clear();
		this.rho40  = 0;
		this.rhoMSA  = 0;
		this.msaCnt = 0;
		this.startet = false;
		this.warmup = 1400;
		this.warmdown = 1600;
		this.dsLeft = 0;
		this.usLeft = 0;
	}

	public void save() {
		//		if ()
		this.measures.add(this.current);
		//		this.measures.addAll(this.tmpM);
	}

	public double getCurrentRho() {
		return this.current.rho;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {

		AgentInfo ai;
		boolean isUs = false;
		if (event.getLinkId() == this.ds){
			ai = this.onLink.remove(event.getVehicleId());
			if (ai == null){
				return;
			}
			this.left++;
			this.dsCnt--;
			this.dsLeft++;

		} else if (event.getLinkId() == this.us){
			isUs = true;
			ai = this.onLink.remove(event.getVehicleId());
			if (ai == null){
				return;
			}
			this.left++;
			this.usCnt--;
			this.usLeft++;
		} else {
			return;
		}
		double time = event.getTime() - ai.e.getTime();
		double speed = this.link.getLength()/time;
		double l = this.link.getLength();
		double w = this.link.getCapacity();
		double rho = (this.dsCnt+this.usCnt)/(w*l);

		//		System.out.println(speed + "  " + rho);

		double flow = ((this.left-ai.left-1)/time)/this.link.getCapacity();///this.caLink.getLink().getCapacity();
		double flowComp = rho * speed;
		//		rho = flow/speed;

		double cellSize = this.caL.getCellLength();
		int from = (int) (0.5+3/cellSize);
		int to = (int) (0.5+5/cellSize);
		
		AgentMeasure am = new AgentMeasure();
		int cnt = 0;
		double rhoSum = 0;
		for (int i = from; i <= to; i++) {


				CAAgent part = this.caL.getParticles()[i];
				if (part != null) {
					CASimpleDynamicAgent da = (CASimpleDynamicAgent) part;
					double myRho = da.getMyDirectionRho()+da.getTheirDirectionRho();
					am.rho += myRho;
					am.v += da.getV();//myRho;
					rhoSum += myRho;
					am.flow = (am.v*am.rho)/myRho;
					cnt++;
				}


		}
		
		if (cnt == 0) {
			am.v = 0;
			am.rho = 0;
		} else {
			am.v /= cnt;
			am.rho /= cnt;
		}

		am.flow = am.v*am.rho;
		this.ams.add(am);

		this.rho40 = (1.-(1./40.))*this.rho40 + rho/40.;
		//		if (this.msaCnt == 0) {
		//			this.rhoMSA = rho;
		//			this.msaCnt = 1;
		//		} else {
		//			this.rhoMSA = this.msaCnt/(this.msaCnt+1)*this.rhoMSA + 1./(this.msaCnt+1.) * rho;
		//			this.msaCnt++;
		//		}
		this.rhoMSA = this.msaCnt/(this.msaCnt+1.)*this.rhoMSA + 1./(this.msaCnt+1.) * rho;
		this.msaCnt++;

		double diff40 = Math.abs(this.rho40-rho);

		//		if (!this.startet & this.rho40 >= 4 & diff40 < 0.05) {
		//			this.warmup = this.left;
		//			this.warmdown = this.warmup+20;
		//			this.startet = true;
		//		} else if (!this.startet & this.rho40 >= 3 & diff40 < 0.05) {
		//			this.warmup = this.left;
		//			this.warmdown = this.warmup+50;
		//			this.startet = false;
		//		}
		//		double diff = Math.abs(this.rho10 - this.rhoMSA);
		if (this.left < this.warmup || this.left > this.warmdown ) {
			return;
		}
		this.startet = true;

		//		System.out.println(event);

		Measure m = new Measure();
		m.flow = flow;
		m.rho = rho;
		m.flowComp = flowComp;
		m.speed = speed;
		this.tmpM.add(m);

		if (this.current.cnt == 0) {
			this.current.flow = flow;
			this.current.rho = rho;
			this.current.flowComp = flowComp;
			this.current.speed = speed;
			this.current.cnt++;
		} else {
			this.current.flow = this.current.cnt/(this.current.cnt+1.) * this.current.flow + 1./(this.current.cnt+1.)*flow;
			this.current.flowComp = this.current.cnt/(this.current.cnt+1.) * this.current.flowComp + 1./(this.current.cnt+1.)*flowComp;
			this.current.rho = this.current.cnt/(this.current.cnt+1.) * this.current.rho + 1./(this.current.cnt+1.)*rho;
			this.current.speed = this.current.cnt/(this.current.cnt+1.) * this.current.speed + 1./(this.current.cnt+1.)*speed;
			this.current.cnt++;
		}
		if (isUs) {
			double usRho = (this.usCnt)/(this.link.getLength()*this.link.getCapacity());
			double usFlow = ((this.usLeft-ai.usLeft-1)/time)/this.link.getCapacity();///this.caLink.getLink().getCapacity();
			double usFlowComp = usRho * speed;

			if (this.current.usCnt == 0) {
				this.current.usFlow = usFlow;
				this.current.usRho = usRho;
				this.current.usFlowComp = usFlowComp;
				this.current.usSpeed = speed;
				this.current.usCnt++;				
			} else {
				this.current.usFlow = this.current.usCnt/(this.current.usCnt+1.) * this.current.usFlow + 1./(this.current.usCnt+1.)*usFlow;
				this.current.usFlowComp = this.current.usCnt/(this.current.usCnt+1.) * this.current.usFlowComp + 1./(this.current.usCnt+1.)*usFlowComp;
				this.current.usRho = this.current.usCnt/(this.current.usCnt+1.) * this.current.usRho + 1./(this.current.usCnt+1.)*usRho;
				this.current.usSpeed = this.current.usCnt/(this.current.usCnt+1.) * this.current.usSpeed + 1./(this.current.usCnt+1.)*speed;
				this.current.usCnt++;				
			}

		} else {
			double dsRho = (this.dsCnt)/(this.link.getLength()*this.link.getCapacity());
			double dsFlow = ((this.dsLeft-ai.dsLeft-1)/time)/this.link.getCapacity();///this.caLink.getLink().getCapacity();
			double dsFlowComp = dsRho * speed;

			if (this.current.dsCnt == 0) {
				this.current.dsFlow = dsFlow;
				this.current.dsRho = dsRho;
				this.current.dsFlowComp = dsFlowComp;
				this.current.dsSpeed = speed;
				this.current.dsCnt++;				
			} else {
				this.current.dsFlow = this.current.dsCnt/(this.current.dsCnt+1.) * this.current.dsFlow + 1./(this.current.dsCnt+1.)*dsFlow;
				this.current.dsFlowComp = this.current.dsCnt/(this.current.dsCnt+1.) * this.current.dsFlowComp + 1./(this.current.dsCnt+1.)*dsFlowComp;
				this.current.dsRho = this.current.dsCnt/(this.current.dsCnt+1.) * this.current.dsRho + 1./(this.current.dsCnt+1.)*dsRho;
				this.current.dsSpeed = this.current.dsCnt/(this.current.dsCnt+1.) * this.current.dsSpeed + 1./(this.current.dsCnt+1.)*speed;
				this.current.dsCnt++;				
			}

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
			ai.dsLeft = this.dsLeft;
			ai.usLeft = this.usLeft;
			this.onLink.put(event.getVehicleId(), ai);
			this.dsCnt++;
		} else if (event.getLinkId() == this.us){
			AgentInfo ai = new AgentInfo();
			ai.e = event;
			ai.left = this.left;
			ai.dsLeft = this.dsLeft;
			ai.usLeft = this.usLeft;
			this.onLink.put(event.getVehicleId(), ai);
			this.usCnt++;
		}
		//		System.out.println("dsCnt:" + this.dsCnt +" usCnt:" + this.usCnt + " total:" + (this.dsCnt+this.usCnt));
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Measure m : this.measures) {
			buf.append(m.rho + " " + m.flow + " " + m.speed + " " + m.usRho + " " + m.usFlowComp + " " + m.usSpeed  + " " + m.dsRho + " " + m.dsFlowComp + " " + m.dsSpeed);
		}
		this.measures.clear();
		return buf.toString();
	}

	public void writeAMS(BufferedWriter bf) throws IOException {
		Map<Integer,AgentMeasure> smooth = new HashMap<Integer,CALinkMonitorIII.AgentMeasure>();
		for (AgentMeasure a : this.ams) {
			int idx = (int) (a.rho*1000.+0.5);
			AgentMeasure sa = smooth.get(idx);
			if (sa == null) {
				sa = new AgentMeasure();
				smooth.put(idx, sa);
			}
			sa.flow = sa.cnt/(sa.cnt+1.) * sa.flow + 1./(sa.cnt+1) * a.flow;
			sa.rho = sa.cnt/(sa.cnt+1.) * sa.rho + 1./(sa.cnt+1) * a.rho;
			sa.v = sa.cnt/(sa.cnt+1.) * sa.v + 1./(sa.cnt+1) * a.v;
			sa.cnt++;
			
		}
		
		
		for (AgentMeasure a : smooth.values()) {
			System.out.println(a);
//			if (a.cnt < 10) {
//				continue;
//			}
			bf.append(Double.toString(a.rho));
			bf.append(' ');
			bf.append(Double.toString(a.flow));
			bf.append(' ');
			bf.append(Double.toString(a.v));
			bf.append('\n');
		}
	}

	private static final class AgentInfo {
		public long dsLeft;
		public long usLeft;
		long left;
		Event e;
	}

	private static final class AgentMeasure {
		double cnt = 0;
		double v;
		double rho;
		double flow;
		@Override
		public String toString() {
			return this.rho + " " + this.flow + " " + this.v;
		}
	}

	private static final class Measure {
		public double flowComp;
		public double speed;
		double cnt = 0; 
		double flow = 0;
		double rho = 0;
		double usSpeed;
		double dsSpeed;
		double usCnt;
		double dsCnt;
		double usFlow;
		double dsFlow;
		double usRho;
		double dsRho;
		double usFlowComp;
		double dsFlowComp;
	}


	public void setCALinkDynamic(CALinkDynamic l) {
		this.caL = l;
	}


}
