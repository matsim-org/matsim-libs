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

import playground.gregor.sim2d_v4.debugger.VisDebugger.Text;
import playground.gregor.sim2d_v4.debugger.VisDebugger.WeightedLine;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;

public class QSimFibonacciPulser implements VisDebuggerAdditionalDrawer, AgentDepartureEventHandler, AgentArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler{

	private final int [] intensities = {1,1,3,5,8,13,21,34};//, 34, 21,13,8,5,3,1,1};
	//	private final int [] intensities = {8,13,21};
	List<LinkInfo> links = new ArrayList<LinkInfo>();
	Map<Id,LinkInfo> map = new HashMap<Id,LinkInfo>();

	public QSimFibonacciPulser(Scenario sc) {
		double offsetX = sc.getScenarioElement(Sim2DScenario.class).getSim2DConfig().getOffsetX();
		double offsetY = sc.getScenarioElement(Sim2DScenario.class).getSim2DConfig().getOffsetY();
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (l.getAllowedModes().contains("walk2d")){
				continue;
			}

			double width = l.getCapacity()/sc.getNetwork().getCapacityPeriod()/1.3;
			int lanes = (int) (width/0.71 + .5);

			double dx = l.getToNode().getCoord().getX() - l.getFromNode().getCoord().getX();
			double dy = l.getToNode().getCoord().getY() - l.getFromNode().getCoord().getY();
			double length = Math.sqrt(dx*dx + dy*dy);
			dx /= length;
			dy /= length;
			LinkInfo info = new LinkInfo();
			info.cap = (int) (l.getLength()*lanes/0.26);
			WeightedLine line = new WeightedLine();
			double ii = width/2;
			line.x0 = (float) (l.getFromNode().getCoord().getX() - offsetX );
			line.y0 = (float) (l.getFromNode().getCoord().getY() - offsetY );
			line.x1 = (float) (l.getToNode().getCoord().getX() - offsetX );
			line.y1 = (float) (l.getToNode().getCoord().getY() - offsetY );
			line.r = 255;
			line.a = 128;
			line.weight = 1;
			info.w = line;
			info.width = width;

			info.x0 = line.x0;
			info.x1 = line.x1;
			info.y1 = line.y1;
			info.y0 = line.y0;
			info.dx = dy;
			info.dy = -dx;


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
		for (LinkInfo l : this.links) {
			synchronized (l) {
				final WeightedLine ll = l.w;
				ll.r=0;
				ll.g=0;
				ll.b=0;
				ll.a=255;
				ll.x0 = (float) (l.x0+l.dx*l.width/2);
				ll.x1 = (float) (l.x1+l.dx*l.width/2);
				ll.y0 = (float) (l.y0+l.dy*l.width/2);
				ll.y1 = (float) (l.y1+l.dy*l.width/2);
				ll.weight = (float) l.width;
				p.drawWeightedLine(ll);
				if (l.onLink == 0) {
					continue;
				}
				ll.a  = 255;
				if (l.onLinkG > l.onLinkR) {
					l.w.g = 255;
					l.w.r = 0;
					l.w.b = 0;
				} else {
					l.w.g = 0;
					l.w.r = 255;
					l.w.b = 0;
				}
				ll.a = 192;
				p.drawWeightedLine(ll);
				ll.a = 255;
				ll.weight = (float) ((this.intensities[(int) l.count])*l.width/34);
				//				for (int i = 0; i < 5; i++) {

				ll.x0 = (float) (l.x0+l.dx*ll.weight);
				ll.x1 = (float) (l.x1+l.dx*ll.weight);
				ll.y0 = (float) (l.y0+l.dy*ll.weight);
				ll.y1 = (float) (l.y1+l.dy*ll.weight);
				p.drawWeightedLine(ll);
				//					ll.weight /=2;
				p.drawText(l.t);
				//				}
				l.count += l.incr ;
				if (l.count >= l.from+l.range) {
					l.incr = -l.incr;
					l.count += l.incr;
				} else if (l.count < l.from) {
					l.incr = -l.incr;
					l.count += l.incr;
				}
			}
		}

	}


	private static final class LinkInfo {
		public double dy;
		public double dx;
		public float y0;
		public float y1;
		public float x1;
		public float x0;
		public int cap;
		public double incr = 0.1;
		public double width;
		//		public double incr;
		public int from=0; //0-5
		public final int range = 4;
		WeightedLine w;
		double count = this.from;
		public int onLink;
		public int onLinkG;
		public int onLinkR;
		Text t;
		public void revise() {
			double load = this.onLink/this.cap;
			int from;
			if (load >= 0.75) {
				from = 3;
			} else if (load >= .5) {
				from =2;
			} else if (load >= .25) {
				from = 1;
			} else {
				from = 0;
			}
			if (from != this.from) {
				this.from = from;
				this.count = from;
				this.incr = 0.1;
			}

		}
	}





	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink--;
			if (event.getPersonId().toString().contains("g")){
				info.onLinkG--;
			} else {
				info.onLinkR--;
			}
			info.revise();
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
			if (event.getPersonId().toString().contains("g")){
				info.onLinkG++;
			} else {
				info.onLinkR ++;
			}
			info.revise();
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
			if (event.getPersonId().toString().contains("g")){
				info.onLinkG++;
			} else {
				info.onLinkR ++;
			}
			info.revise();
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
			if (event.getPersonId().toString().contains("g")){
				info.onLinkG--;
			} else {
				info.onLinkR--;
			}
			info.revise();
			info.t.text = Integer.toString(info.onLink);
		}
	}


}
