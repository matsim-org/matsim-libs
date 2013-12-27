/* *********************************************************************** *
 * project: org.matsim.*
 * CrossSectionFlowAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;


public class CrossSectionFlowAnalysis implements LinkEnterEventHandler, LinkLeaveEventHandler{

	private final Id enterLink;
	private final Id leaveLink;
	private final String fileName;
	
	private static final int UPDATE_INTERVAL = 1;
	private double lastUpdate = 0;
	private int cnt = 0;
	
	private final List<Measurement> measurements = new ArrayList<CrossSectionFlowAnalysis.Measurement>();
	private final double linkWidth;
	public CrossSectionFlowAnalysis(Id enterLink, Id leaveLink, String fileName, double linkWidth) {
		this.enterLink = enterLink;
		this.leaveLink = leaveLink;
		this.fileName = fileName;
		this.linkWidth = linkWidth;
	}
	
	@Override
	public void reset(int iteration) {
		//there is no time for a nicer way of doing this!! needs to be fixed!
		if (this.measurements.size() == 0) {
			return;
		}
			try {
				BufferedWriter bf = new BufferedWriter(new FileWriter(new File(this.fileName+"_"+this.enterLink.toString()+"+"+this.leaveLink.toString()+".it"+iteration)));
				for (Measurement m : this.measurements) {
					bf.append(m.time + " " + m.flow+ "\n");
				}
				bf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (event.getLinkId().equals(this.leaveLink)) {
			if (event.getTime() > this.lastUpdate+UPDATE_INTERVAL) {
				update(event.getTime()-this.lastUpdate,this.lastUpdate);
				this.lastUpdate = event.getTime();
				this.cnt = 0;
			}
			this.cnt++;
		}
		
	}

	private void update(double intervalLength,double time) {
		Measurement m = new Measurement();
		m.time = time;
		m.flow = (this.cnt/intervalLength)/this.linkWidth;
		this.measurements.add(m);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().equals(this.enterLink)) {
			if (event.getTime() > this.lastUpdate+UPDATE_INTERVAL) {
				update(event.getTime()-this.lastUpdate,this.lastUpdate);
				this.lastUpdate = event.getTime();
				this.cnt = 0;
			}
			this.cnt++;
		}
		
	}
	
	private final static class Measurement {
		double time;
		double flow;
	}

}
