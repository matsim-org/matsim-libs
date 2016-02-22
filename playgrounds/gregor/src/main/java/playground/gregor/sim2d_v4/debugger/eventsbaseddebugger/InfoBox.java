/* *********************************************************************** *
 * project: org.matsim.*
 * InfoBoc.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.misc.Time;
import processing.core.PConstants;

public class InfoBox implements ClockedVisDebuggerAdditionalDrawer, VisDebuggerOverlay {

	private final EventBasedVisDebuggerEngine dbg;
	private final double dT;

	private double last = 0;
	private double speedup = 1;
	private int nrAgents = 0;
	
	public InfoBox(EventBasedVisDebuggerEngine dbg, Scenario sc) {
		this.dbg = dbg;
		this.dT = 0.1;//((Sim2DScenario) sc.getScenarioElement(Sim2DScenario.ELEMENT_NAME)).getSim2DConfig().getTimeStepSize();
	}

	@Override
	public void draw(EventsBasedVisDebugger p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawText(EventsBasedVisDebugger p) {
		p.strokeWeight(2);
		p.fill(0,0,0,235);
		p.stroke(222,222,222,255);
		int round = 10;

		
		float ts = 18;
		float x = 5 + round;
		float y = 5 + round + ts;
		p.textSize(ts);	
//		PVector cv = p.zoomer.getCoordToDisp(new PVector((float)(li.tx+p.offsetX),(float)-(li.ty+p.offsetY)));
//		p.fill(0,0,0,255);
//		float w = p.textWidth(li.text);
		
		double t = p.time;
		String tm = Time.writeTime(t, Time.TIMEFORMAT_HHMMSS);
		String stm = "time: " + tm;
		float w = p.textWidth(stm);
//		p.rect(5, 5, 5+15+w+round, 5+round+ts+round + ts + ts/2 + ts + ts/2,round);
		p.rect(5, 5, 5+15+w+round, 5+round+ts+ round,round);
		
		p.fill(255);
		p.textAlign(PConstants.LEFT);
		p.text(stm, x, y);
//		double sph = this.speedup > .98 ? Math.round(this.speedup) : this.speedup;
//		String tt = Integer.toString(ttt);
//		String dec = Integer.toString((int)((this.speedup-ttt)*100));
//		p.text("# 2D agents: " + this.nrAgents , x, y+ts+ts/2);
//		p.text("fps: " + (int)(p.frameRate+.5) , x, y+ts+ts/2 + ts + ts/2);

	}

	@Override
	public void update(double time) {
		double delta = (time - this.last);
		if (delta <= 0) {
			return;
		}
		this.speedup = 0.99*this.speedup + 0.01 * (1000.*this.dT)/delta;
		this.last = time;
		
	}

	public void setNrAgents(int nrAgents) {
		this.nrAgents = nrAgents;
		
	}

}
