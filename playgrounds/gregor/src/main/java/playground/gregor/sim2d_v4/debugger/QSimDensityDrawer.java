/* *********************************************************************** *
 * project: org.matsim.*
 * QSimFibonacciPulser.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.NetworkImpl;

import playground.gregor.sim2d_v4.debugger.VisDebugger.Text;
import playground.gregor.sim2d_v4.debugger.VisDebugger.WeightedLine;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;

public class QSimDensityDrawer implements VisDebuggerAdditionalDrawer, AgentDepartureEventHandler, AgentArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler{

	List<LinkInfo> links = new ArrayList<LinkInfo>();
	Map<Id,LinkInfo> map = new HashMap<Id,LinkInfo>();

	private double dens = 0;
	
	public QSimDensityDrawer(Scenario sc) {
		double offsetX = sc.getScenarioElement(Sim2DScenario.class).getSim2DConfig().getOffsetX();
		double offsetY = sc.getScenarioElement(Sim2DScenario.class).getSim2DConfig().getOffsetY();
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (l.getAllowedModes().contains("walk2d") || l.getCapacity() == 2340){
				continue;
			}

			double width = l.getCapacity()/sc.getNetwork().getCapacityPeriod()/1.3;

			double dx = l.getToNode().getCoord().getX() - l.getFromNode().getCoord().getX();
			double dy = l.getToNode().getCoord().getY() - l.getFromNode().getCoord().getY();
			double length = Math.sqrt(dx*dx + dy*dy);
			dx /= length;
			dy /= length;
			LinkInfo info = new LinkInfo();
			info.cap = (l.getLength()/((NetworkImpl)sc.getNetwork()).getEffectiveCellSize())*l.getNumberOfLanes();
			WeightedLine ll = new WeightedLine();

			ll.r=0;
			ll.g=0;
			ll.b=0;
			ll.a=255;
			ll.x0 = (float) ((l.getFromNode().getCoord().getX() - offsetX )+dy*width/2);
			ll.x1 = (float) ((l.getToNode().getCoord().getX() - offsetX )+dy*width/2);
			ll.y0 = (float) ((l.getFromNode().getCoord().getY() - offsetY )-dx*width/2);
			ll.y1 = (float) ((l.getToNode().getCoord().getY() - offsetY )-dx*width/2);
			ll.weight = (float) width;

			info.w = ll;

			Text t = new Text();
			t.x = (float) (l.getCoord().getX() - offsetX + 1.5*width*dy);
			t.y = (float) (l.getCoord().getY() - offsetY - 1.5*width*dx);
			info.t = t;
			t.minScale = 8;
			t.r = t.g = t.b = 0;
			t.text = "0";

			this.links.add(info);
			this.map.put(l.getId(), info);
		}
	}


	@Override
	public void draw(VisDebugger p) {
		
		double dens = 0;
		double cnt = 0;
		for (LinkInfo l : this.links) {
			synchronized (l) {
				if (l.onLink>0) {
					dens += l.onLink/l.cap;
					cnt++;
				}
				updateColor(l);
				final WeightedLine ll = l.w;
				p.drawWeightedLine(ll);
				p.drawText(l.t);
			}
		}
		this.dens = 5.4*dens/cnt;
//		drawLegend(p);
	}


	private void drawLegend(VisDebugger p) {
		final int tlx = 10;
		final float width = 20;
		final float height = 200;
		final int tly = (int) (p.getHeight()- height - 10);
		int ts = 20;
		p.textSize(ts);
		
		p.fill(222, 222, 222, 235);
		p.strokeWeight(1);
		p.stroke(255,255,255,255);
		float hw = p.textWidth("p/m");
		p.textSize(ts/2);
		p.rect(tlx-5, tly-5-2*ts, hw + 10+p.textWidth('2'), height+2*ts+10,5);
		p.textSize(ts);
		
		p.fill(0);

//		float ht = p.
		p.text("p/m", tlx, tly-ts);
		p.textSize(ts/2);
		p.text("2",tlx+hw,tly-ts/2-ts);
		p.textSize(ts);
		
		final float densCoeff = height/5.4f;
		int c0 = p.color(0, 255, 0, 128);
		int c1 = p.color(0, 255, 0, 255);
		int c2 = p.color(255, 255, 0, 255);
		int c3 = p.color(255, 0, 0, 255);
		int c4 = p.color(128, 0, 128, 255);
		setGradient(tlx,tly,width,.25f*densCoeff,c0,c1,p);
		setGradient(tlx,(int)(tly+.25f*densCoeff),width,.75f*densCoeff,c1,c2,p);
		setGradient(tlx,(int)(tly+densCoeff),width,densCoeff,c2,c3,p);
		setGradient(tlx,(int)(tly+2*densCoeff),width,3.4f*densCoeff,c3,c4,p);
		
//		p.line(tlx, tly, tlx+width+20, tly);
//		p.line(tlx, tly+.25f*densCoeff, tlx+width+20, tly+.25f*densCoeff);
//		p.line(tlx, tly+densCoeff, tlx+width+20, tly+densCoeff);
//		p.line(tlx, tly+2*densCoeff, tlx+width+20, tly+2*densCoeff);
//		p.line(tlx, tly+5.4f*densCoeff, tlx+width+20, tly+5.4f*densCoeff);
		
		
//		p.text('0', tlx+width/2, tly+10);
		
		float tw = p.textWidth('1');
		p.text('0', tlx+width+tw/2, tly+ts/2);
		p.text('1', tlx+width+tw/2, tly+densCoeff+ts/2);
		p.text('2', tlx+width+tw/2, tly+2*densCoeff+ts/2);
		p.text('3', tlx+width+tw/2, tly+3*densCoeff+ts/2);
		p.text('4', tlx+width+tw/2, tly+4*densCoeff+ts/2);
		p.text("5", tlx+width+tw/2, tly+5*densCoeff+ts/2);
		
		p.stroke(0);
		p.strokeWeight(2);
		p.line(tlx,(float) (tly+this.dens*densCoeff), tlx+width,(float) (tly+this.dens*densCoeff));
	}

	void setGradient(int x, int y, float w, float h, int c1, int c2, VisDebugger p ) {

		p.noFill();
		p.strokeWeight(1);
		for (int i = y; i <= y+h; i++) {
			float inter = VisDebugger.map(i, y, y+h, 0, 1);
			int c = p.lerpColor(c1, c2, inter);
			p.stroke(c);
			p.line(x, i, x+w, i);
		}
	}


	private void updateColor(LinkInfo l) {
//		if (l.onLink > l.cap) {
//			l.cap = l.onLink;
//		}
		double density =5.4*l.onLink/l.cap;
		if (density == 0) {
			l.w.r = 0;
			l.w.g = 0;
			l.w.b = 0;
			l.w.a = 255;
		} else if (density < 0.25) {
			l.w.a = (int) (128 + 128 * (density/0.25));
			l.w.r = 0;
			l.w.g = 255;
			l.w.b = 0;
		} else if (density < 1) {
			l.w.a = 255;
			l.w.g = 255;
			l.w.b = 0;
			l.w.r = (int) (255 * ((density-.25)/0.75));
		} else if (density < 2) {
			l.w.a = 255;
			l.w.g = 255 - (int) (255 * ((density-1)));;
			l.w.b = 0;
			l.w.r = 255;			
		} else {
			l.w.a = 255;
			l.w.g = 0;
			l.w.b = (int) (128 * (density-2)/3.4);
			l.w.r = 255 - l.w.b;
		}

	}


	private static final class LinkInfo {
		public double cap;
		WeightedLine w;
		public int onLink;
		Text t;
	}





	@Override
	public void reset(int iteration) {
		for (LinkInfo li :this.links) {
			li.onLink = 0;
		}

	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink--;
			//			if (event.getPersonId().toString().contains("g")){
			//				info.onLinkG--;
			//			} else {
			//				info.onLinkR--;
			//			}
			info.t.text = Integer.toString(info.onLink);
		}

	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink++;
			//			if (event.getPersonId().toString().contains("g")){
			//				info.onLinkG++;
			//			} else {
			//				info.onLinkR ++;
			//			}
			info.t.text = Integer.toString(info.onLink);
		}		
	}


	@Override
	public void handleEvent(AgentDepartureEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink++;
//						if (event.getPersonId().toString().contains("g")){
			//				info.onLinkG++;
			//			} else {
			//				info.onLinkR ++;
			//			}
			info.t.text = Integer.toString(info.onLink);
		}		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink--;
			//			if (event.getPersonId().toString().contains("g")){
			//				info.onLinkG--;
			//			} else {
			//				info.onLinkR--;
			//			}
			info.t.text = Integer.toString(info.onLink);
		}
	}


}
