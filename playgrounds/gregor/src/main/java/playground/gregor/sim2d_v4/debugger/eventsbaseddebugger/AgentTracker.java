/* *********************************************************************** *
 * project: org.matsim.*
 * AgentTracker.java
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

package playground.gregor.sim2d_v4.debugger.eventsbaseddebugger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import processing.core.PApplet;
import processing.core.PImage;

public class AgentTracker implements VisDebuggerAdditionalDrawer,
		LinkEnterEventHandler {

	private final Id id;// = new IdImpl("carblowup8888");
	private final Scenario sc;
	double x = 0;
	double y = 0;
	private PImage pI = null;
	
	public AgentTracker(Scenario sc, Id car) {
		this.sc = sc;
		this.id = car;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (Id.createPersonId(event.getVehicleId()).equals(this.id)) {
			if (event.getLinkId().toString().contains("el")) {
				this.x = 0;
				this.y = 0;
				return;
			}
			
			Link l = this.sc.getNetwork().getLinks().get(event.getLinkId());
		
			this.x = l.getCoord().getX();
			this.y = l.getCoord().getY();
		}

	}

	@Override
	public void draw(EventsBasedVisDebugger p) {
		if (this.pI == null) {
			this.pI = p.loadImage("/Users/laemmel/devel/hhw_hybrid/"+this.id.toString()+".png");
		}
		
		double ox = p.offsetX;
		double oy = p.offsetY;
		float fx = (float) (this.x+ox);
		float fy = (float) -(this.y + oy);
		p.fill(255,255,255,255);
		p.strokeWeight(10);
		p.stroke(0,0,0,255);
		p.ellipse(fx, fy, 60, 60);
		p.fill(0);
		p.ellipse(fx, fy, 20, 20);
//		p.fill(192,128,128,255);
//		p.text("Agent: 9445", fx+3, fy+3);
		p.imageMode(PApplet.CORNER);
		
//		float scale = 7.76158755511743467621f;
		float scale = 9.90597109232583625154f;
		p.image(this.pI, fx-scale*184, fy-scale*60,scale*184,scale*60);

	}

	@Override
	public void drawText(EventsBasedVisDebugger eventsBasedVisDebugger) {
		// TODO Auto-generated method stub

	}

}
