/* *********************************************************************** *
 * project: org.matsim.*
 * SeeCasino.java
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import processing.core.PConstants;
import processing.core.PVector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class SeeCasino implements VisDebuggerAdditionalDrawer, LinkEnterEventHandler, LinkLeaveEventHandler{
	

	private final Map<Id,Double> agents = new HashMap<Id,Double>(4000);
	
	private final double tx = 713229.46-15;
	private final double ty = 6571899.87+15;

	
	private final Id<Link> enter0 = Id.create("sim2d_4_rev_-12468", Link.class);
	private final Id<Link> enter1 = Id.create("sim2d_0_rev_-12724", Link.class);
	private final Id<Link> leave = Id.create("sim2d_0_-12622", Link.class);
	
	private final Id<Link> leave1 = Id.create("sim2d_4_-12468", Link.class);
	private final Id<Link> leave2 = Id.create("sim2d_0_-12724", Link.class);
	double avgQueueTime = 0;
	int count = 0;
	
	int inside = 0;
	
	private final static Polygon P;
	static {
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize("/Users/laemmel/devel/fzj/gis/seecasino.shp");
		Iterator<SimpleFeature> it = reader.getFeatureSet().iterator();
		Geometry g = (Geometry) it.next().getDefaultGeometry();
		while (it.hasNext()) {
			Geometry gg = (Geometry)it.next().getDefaultGeometry();
			g = g.union(gg);
		}
		P = (Polygon) g;
	}
	
	
	@Override
	public void draw(EventsBasedVisDebugger p) {
		p.strokeWeight(1);
//		p.fill(255,0,0,255);
		p.fill(0);
		p.stroke(0,0);
		p.beginShape();
		for (Coordinate c : P.getExteriorRing().getCoordinates()) {
			p.vertex((float)(c.x+p.offsetX), (float)-(c.y+p.offsetY));
		}
		p.endShape();
		
		p.strokeWeight((float) (2/p.zoomer.getZoomScale()));
		p.fill(0,0,0,235);
		p.stroke(222,222,222,255);
		int round = 1;
		p.rect((float)(this.tx+p.offsetX), (float)-(this.ty+p.offsetY), 70+round, round+5+round+5+round+5,round);
		
		
	}
	@Override
	public void drawText(EventsBasedVisDebugger p) {
		float ts = (float) (5*p.zoomer.getZoomScale());
		p.textSize(ts);
		PVector cv = p.zoomer.getCoordToDisp(new PVector((float)(this.tx+p.offsetX+1),(float)-(this.ty-5+.5+p.offsetY)));
		p.fill(255);
		p.textAlign(PConstants.LEFT);
		String tm = Time.writeTime(this.avgQueueTime, Time.TIMEFORMAT_HHMMSS);
		float offset = p.textWidth("avg. queuing time: ");
		p.text("avg. queuing time: ", cv.x, cv.y);
		p.text(tm, cv.x+offset, cv.y);
		p.text("#agents queuing: ", cv.x, cv.y + ts+ 1);
		p.text(this.agents.size(), cv.x+offset, cv.y + ts+ 1);
		p.text("#agents inside: ", cv.x, cv.y + ts+ 1 + ts + 1);
		p.text(this.inside, cv.x+offset, cv.y + ts+ 1 + ts +1);
	}
	@Override
	public void reset(int iteration) {
		this.count = 0;
		this.avgQueueTime = 0;
		this.agents.clear();
		
	}
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(this.leave)) {
			Double a = this.agents.remove(event.getDriverId());
			double tm = event.getTime()-a;
			this.avgQueueTime = this.count/(this.count+1.) * this.avgQueueTime + 1/(this.count+1.) * tm;
			this.count++;
		} else if (event.getLinkId().equals(this.leave1)||event.getLinkId().equals(this.leave2)) {
			this.inside--;
		}
		
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().equals(this.enter0)||event.getLinkId().equals(this.enter1)) {
			this.agents.put(event.getDriverId(), event.getTime());
			this.inside++;
		}
		
			

	}


}
