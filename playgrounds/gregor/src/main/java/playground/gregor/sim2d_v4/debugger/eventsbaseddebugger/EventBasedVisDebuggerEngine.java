/* *********************************************************************** *
 * project: org.matsim.*
 * EventDecoderEngine.java
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2d_v4.events.Sim2DAgentConstructEvent;
import playground.gregor.sim2d_v4.events.Sim2DAgentConstructEventHandler;
import playground.gregor.sim2d_v4.events.Sim2DAgentDestructEvent;
import playground.gregor.sim2d_v4.events.Sim2DAgentDestructEventHandler;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v4.events.debug.NeighborsEvent;
import playground.gregor.sim2d_v4.events.debug.NeighborsEventHandler;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class EventBasedVisDebuggerEngine implements XYVxVyEventsHandler, Sim2DAgentConstructEventHandler, Sim2DAgentDestructEventHandler, NeighborsEventHandler{

	double time;
	private final EventsBasedVisDebugger vis;

	private final Map<Id,CircleProperty> circleProperties = new HashMap<Id,CircleProperty>();
	private final Scenario sc;

	private long lastUpdate = -1;
	private final double dT;
	private final KeyControl keyControl;

	public EventBasedVisDebuggerEngine(Scenario sc) {
		this.sc = sc;
		this.dT = sc.getScenarioElement(Sim2DScenario.class).getSim2DConfig().getTimeStepSize();
		this.vis = new EventsBasedVisDebugger(sc);
		this.keyControl = new KeyControl(this.vis.zoomer);
		this.vis.addKeyListener(this.keyControl);
		init();
	}
	
	public void addAdditionalDrawer(VisDebuggerAdditionalDrawer drawer) {
		this.vis.addAdditionalDrawer(drawer);
	}
	
	private void init() {

		//Links
		LineProperty lp = new LineProperty();
		lp.r = 0; lp.g = 0; lp.b = 0; lp.a = 255;
		lp.minScale = 10;

		Sim2DScenario s2dsc = this.sc.getScenarioElement(Sim2DScenario.class);
		for (Sim2DEnvironment env : s2dsc.getSim2DEnvironments()) {
			Network eNet = env.getEnvironmentNetwork();

			Set<String> handled = new HashSet<String>();
			for (Link l : eNet.getLinks().values()) {
				String revKey = l.getToNode().getId() + "_" + l.getFromNode().getId();
				if (handled.contains(revKey)) {
					continue;
				} else {
					String key = l.getFromNode().getId() + "_" + l.getToNode().getId();
					handled.add(key);
				}

				Coord c0 = l.getFromNode().getCoord();
				Coord c1 = l.getToNode().getCoord();
//				this.vis.addDashedLineStatic(c0.getX(), c0.getY(), c1.getX(), c1.getY(), lp.r,lp.g,lp.b,lp.a, lp.minScale,.1,.9);

			}
		}
		for (Sim2DEnvironment env : s2dsc.getSim2DEnvironments()) {
			for (Section sec : env.getSections().values()) {
				int[] open = sec.getOpenings();
				Arrays.sort(open);
				int oct = 0;
				int nextOpen = open[oct];
				for (int i = 0; i < sec.getPolygon().getExteriorRing().getNumPoints()-1; i++) {
					if (i == nextOpen) {
						Coordinate c0 = sec.getPolygon().getExteriorRing().getCoordinateN(i);
						Coordinate c1 = sec.getPolygon().getExteriorRing().getCoordinateN(i+1);
						if (c0.x < c1.x) {
							this.vis.addDashedLineStatic(c0.x, c0.y, c1.x, c1.y, lp.r,lp.g,lp.b,lp.a, 20,.5,.2);
						} else {
							this.vis.addDashedLineStatic(c1.x, c1.y, c0.x, c0.y, lp.r,lp.g,lp.b,lp.a, 20,.5,.2);
						}
						if (oct < open.length-1) {
							oct++;
							nextOpen = open[oct];
						}
						continue;
					}
					Coordinate c0 = sec.getPolygon().getExteriorRing().getCoordinateN(i);
					Coordinate c1 = sec.getPolygon().getExteriorRing().getCoordinateN(i+1);
					this.vis.addLineStatic(c0.x, c0.y, c1.x, c1.y, lp.r,lp.g,lp.b,lp.a, 0);
				}
				Polygon p = (Polygon) sec.getPolygon().buffer(-.1);
				Coordinate[] coords = p.getExteriorRing().getCoordinates();
				double [] x = new double [coords.length];
				double [] y = new double [coords.length];
				for (int i = 0; i < coords.length; i++) {
					x[i] = coords[i].x;
					y[i] = coords[i].y;
				}
				this.vis.addPolygonStatic(x, y, 255, 255, 255, 255, 0);
			}
		}

	}

	@Override
	public void reset(int iteration) {
		this.time = -1;

	}

	@Override
	public void handleEvent(XYVxVyEventImpl event) {
		if (event.getTime() > this.time) {
			update(this.time);
			this.time = event.getTime();
		}
		CircleProperty cp = this.circleProperties.get(event.getPersonId());
		this.vis.addCircle(event.getX(),event.getY(),cp.rr,cp.r,cp.g,cp.b,cp.a,cp.minScale,cp.fill);
		this.vis.addText(event.getX(),event.getY(), event.getPersonId().toString(), 100);
	}

	private void update(double time2) {
		this.keyControl.awaitPause();
		long timel = System.currentTimeMillis();

		long last = this.lastUpdate ;
		long diff = timel - last;
		if (diff < this.dT*1000/this.keyControl.getSpeedup()) {
			long wait = (long) (this.dT *1000/this.keyControl.getSpeedup()-diff);
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.vis.update(this.time);
		this.lastUpdate = System.currentTimeMillis();

	}

	private static final class CircleProperty {
		boolean fill = true;
		float rr;
		int r,g,b,a, minScale = 0;
	}

	private static final class LineProperty {
		int r,g,b,a, minScale = 0;
	}

	@Override
	public void handleEvent(Sim2DAgentConstructEvent event) {
		Sim2DAgent a = event.getSim2DAgent();
		CircleProperty cp = new CircleProperty();
		cp.rr = (float) a.getRadius();
		int nr = a.hashCode()%3*255;
		int r,g,b;
		if (nr > 2*255) {
			r= nr-2*255;
			g =0;
			b=64;
		} else if (nr > 255) {
			r=0;
			g=nr-255;
			b=64;
		} else {
			r=64;
			g=0;
			b=nr;
		}
		cp.r = r;
		cp.g = g;
		cp.b = b;
		cp.a = 222;

//		cp.fill = false;
//		cp.r = 0;
//		cp.g = 0;
//		cp.b = 0;
//		cp.a = 255;
		this.circleProperties.put(a.getId(), cp);
	}

	@Override
	public void handleEvent(Sim2DAgentDestructEvent event) {
		this.circleProperties.remove(event.getSim2DAgent().getId());
	}

	@Override
	public void handleEvent(NeighborsEvent event) {
		List<Tuple<Double, Sim2DAgent>> n = event.getNeighbors();
		Sim2DAgent a = event.getAgent();
		for (Tuple<Double, Sim2DAgent> o : n) {
			Sim2DAgent neighbor = o.getSecond();
			double x0 = a.getPos()[0];
			double y0 = a.getPos()[1];
			double x1 = neighbor.getPos()[0];
			double y1 = neighbor.getPos()[1];
			double dx = x1 - x0;
			double dy = y1 - y0;
			double l = Math.sqrt(dx*dx+dy*dy);
			dx /= l;
			dy /= l;
			
			this.vis.addLine(x0+dx*a.getRadius(), y0+dy*a.getRadius(), x1-dx*neighbor.getRadius(), y1-dy*neighbor.getRadius(), 0, 0, 0, 128, 0);
			double tan = dx/dy;
			double atan = Math.atan(tan);
			if (atan >0) {
				atan -= Math.PI/2;
			} else {
				atan += Math.PI/2;
			}
			
			double offsetX = dy * .075;
			double offsetY = -dx * .075;
			if (dx > 0) {
				offsetX *= -1;
				offsetY *= -1;
			}
			
			double dist =l;
			int tmp = (int)dist;
			int tmp2 = (int)((dist-tmp)*100+.5);
			String fill = tmp2 < 10 ? "0" : "";
			this.vis.addText((x0+x1)/2+offsetX, (y0+y1)/2+offsetY, tmp+"."+fill+tmp2+" m", 100,(float)atan);
			
			
		}
	}



}
