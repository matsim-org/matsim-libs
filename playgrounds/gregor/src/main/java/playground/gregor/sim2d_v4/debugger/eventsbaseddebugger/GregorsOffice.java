/* *********************************************************************** *
 * project: org.matsim.*
 * GregorsOffice.java
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

package playground.gregor.sim2d_v4.debugger.eventsbaseddebugger;

public class GregorsOffice implements VisDebuggerAdditionalDrawer {

	double x = 713016.11+8;
	double y = 6572166.05+4;
	
	@Override
	public void draw(EventsBasedVisDebugger p) {
		double ox = p.offsetX;
		double oy = p.offsetY;
		float fx = (float) (this.x+ox);
		float fy = (float) -(this.y + oy);
		
		p.fill(255,0,0,255);
		p.strokeWeight(1);
		p.ellipse(fx, fy, 3, 3);
		p.stroke(192,128,128,255);
		p.fill(192,128,128,255);
		p.text("Gregor's office", fx+3, fy+3);
	}

	@Override
	public void drawText(EventsBasedVisDebugger eventsBasedVisDebugger) {
		// TODO Auto-generated method stub
		
	}

}
