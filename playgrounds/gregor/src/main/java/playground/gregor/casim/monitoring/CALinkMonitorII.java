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
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

public class CALinkMonitorII implements LinkEnterEventHandler,
LinkLeaveEventHandler {

	int dsCnt;
	int usCnt;
	long dsLeft;
	long usLeft;
	long left;
	private final Id ds;
	private final Id us;

	private final LinkedList<LinkEnterEvent> dsQ = new LinkedList<LinkEnterEvent>();
	private final LinkedList<LinkEnterEvent> usQ = new LinkedList<LinkEnterEvent>();
	
	private final LinkedList<Measure> measures = new LinkedList<Measure>();
	private final double area;
	private final double length;


	public CALinkMonitorII(Id ds, Id us, double length, double width) {
		this.us = us;
		this.ds = ds;
		this.measures.add(new Measure(0));
		this.area = width*length;
		this.length = length;
	}

	@Override
	public void reset(int iteration) {
	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {

		if (event.getLinkId() == this.ds){
			LinkEnterEvent enter = this.dsQ.poll();
			
			
			Measure oldM = this.measures.getLast();
			Measure newM = new Measure(event.getTime());
			newM.usTT = oldM.usTT;
			newM.usCnt = oldM.usCnt;
			this.dsCnt--;
			newM.dsCnt = this.dsCnt;
			newM.dsTT = event.getTime() - enter.getTime();
			this.measures.add(newM);
			
		} else if (event.getLinkId() == this.us){
			LinkEnterEvent enter = this.dsQ.poll();
			Measure oldM = this.measures.getLast();
			Measure newM = new Measure(event.getTime());
			newM.dsTT = oldM.dsTT;
			newM.dsCnt = oldM.dsCnt;
			this.usCnt--;
			newM.usCnt = this.usCnt;
			newM.usTT = event.getTime() - enter.getTime();
			this.measures.add(newM);
		} else {
			return;
		}
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId() == this.ds){
			this.dsQ.addLast(event);
			this.dsCnt++;
		} else if (event.getLinkId() == this.us){
			this.usQ.addLast(event);
			this.usCnt++;
		}
	}

	public void report(BufferedWriter bw) throws IOException {
		for (Measure m : this.measures) {

			double dsRho = m.dsCnt/this.area;
			double usRho = m.usCnt/this.area;

			double dsSpd = this.length/m.dsTT;
			if (m.dsCnt == 0) {
				dsSpd = 0;
			}
			double usSpd = this.length/m.usTT;
			if (m.usCnt == 0) {
				usSpd = 0;
			}
			
			bw.append(m.time + " " + m.dsCnt + " " + m.dsTT + " " + dsRho + " " + dsSpd+ " "+ m.usCnt + " " + m.usTT + " " + usRho + " " + usSpd + "\n");
		}
	}
	

	private final class Measure {
		public Measure(double time) {
			this.time = time;
		}
		int dsCnt;
		int usCnt;
		double dsTT;
		double usTT;
		double time;
	}
	
}
