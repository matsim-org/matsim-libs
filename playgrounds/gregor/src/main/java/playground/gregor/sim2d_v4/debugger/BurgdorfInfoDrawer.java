/* *********************************************************************** *
 * project: org.matsim.*
 * BurgdorfInfoDrawer.java
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

package playground.gregor.sim2d_v4.debugger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;

public class BurgdorfInfoDrawer implements VisDebuggerAdditionalDrawer, AgentDepartureEventHandler, AgentArrivalEventHandler, LinkEnterEventHandler {

	private int total = 0;
	private int dest;
	private int bahnhof;
	
	
	private final Id id0 = new IdImpl("sim2d_0_23938408");
	
	private final Id id1 = new IdImpl("sim2d_0_-11036");
	
	public BurgdorfInfoDrawer(Scenario sc) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void draw(VisDebugger p) {
		int ts = 20;
		p.textSize(ts);
		float ml = p.textWidth("Festplatz: ");
		float tl = p.textWidth("60000");
		
		p.fill(222, 222, 222, 235);
		p.strokeWeight(1);
		p.stroke(255,255,255,255);
		float height = 5+ts+6f*ts+5;
		float width = 5+ml+tl+5;
		int tx = (int) (p.getWidth()-width)-5;
		int ty = (int) (p.getHeight()-height-5);
		p.rect(tx, ty, width, height,5);
		
		
		String time = "Uhrzeit:";// + p.setTime(-1);
		p.fill(0);
		p.text(time, tx+5, ty+5+ts);
		p.text(p.setTime(-1), tx+5+ml, ty+5+ts);
		
		String bahnhof = "Bahnhof:";//    " + 5000;
		p.text(bahnhof, tx+5, ty+5+ts+1.5f*ts);
		p.text(this.bahnhof+"", tx+5+ml, ty+5+ts+1.5f*ts);
		
		String strecke = "Strecke:";//   " + 4000;
		p.text(strecke, tx+5, ty+5+ts+3*ts);
		int str = this.total-this.bahnhof-this.dest;
		p.text(str+"", tx+5+ml, ty+5+ts+3*ts);
		String festplatz = "Festplatz: ";// + 50000;
		p.text(festplatz, tx+5, ty+5+ts+4.5f*ts);
		p.text(this.dest+"", tx+5+ml, ty+5+ts+4.5f*ts);
		String gesamt = "Gesamt:";// + 50000;
		p.text(gesamt, tx+5, ty+5+ts+6f*ts);
		p.text(this.total+"", tx+5+ml, ty+5+ts+6f*ts);
	}

	@Override
	public void reset(int iteration) {
		this.total = 0;
		this.dest = 0;
		this.bahnhof = 0;
		
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.total++;
		this.bahnhof++;
		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.dest++;
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().equals(this.id0) || event.getLinkId().equals(this.id1)) {
			this.bahnhof--;
		}
		
	}

}
