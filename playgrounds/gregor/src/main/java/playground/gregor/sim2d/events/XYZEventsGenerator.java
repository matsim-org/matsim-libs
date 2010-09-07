/* *********************************************************************** *
 * project: org.matsim.*
 * XYZEventsGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d.events;

import playground.gregor.sim2d.simulation.Agent2D;
import playground.gregor.sim2d.simulation.Floor;
import playground.gregor.sim2d.simulation.Force;
import playground.gregor.sim2d.simulation.Network2D;

public class XYZEventsGenerator {
	private static final double TWO_PI = 2 * Math.PI;
	private static final double PI_HALF =  Math.PI / 2;
	private XYZEventsManager xyzEventsManager;
	private Network2D network2d;
	
	private final double interval = 1;    
	private double oldtime = 0;

	public XYZEventsGenerator(XYZEventsManager xyzEventsManager){
		this.xyzEventsManager = xyzEventsManager;
		
	}
	
	public void setNetwork2D(Network2D network2d) {
		this.network2d = network2d;
	}
	
	public void generateEvents(double time){
		if (time < this.oldtime + interval) {
			return;
		}
		this.oldtime = time;
		for (Floor floor : this.network2d.getFloors()) {
			draw(floor,time);
		}		
	}


	private void draw(Floor floor, double time) {
		for (Agent2D agent : floor.getAgents()) {
			Force f = floor.getAgentForce(agent);
			double alpha = getPhaseAngle(f);
			alpha /= TWO_PI;
			alpha *= 360;
			XYZEvent e = new XYZEvent(agent.getId(),agent.getPosition(),alpha,time);
			this.xyzEventsManager.processXYZEvent(e);
		}
	}
	
	private double getPhaseAngle(Force f) {
		double alpha = 0.0;
		if (f.getFx() > 0) {
			alpha = Math.atan(f.getFy()/f.getFx());
		} else if (f.getFx() < 0) {
			alpha = Math.PI + Math.atan(f.getFy()/f.getFx());
		} else { // i.e. DX==0
			if (f.getFy() > 0) {
				alpha = PI_HALF;
			} else {
				alpha = -PI_HALF;
			}
		}
		if (alpha < 0.0) alpha += TWO_PI;
		return alpha;
	}

}
