/* *********************************************************************** *
 * project: org.matsim.*
 * QSimDrawer.java
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

public class QSimDrawer implements VisDebuggerAdditionalDrawer, LinkEnterEventHandler, LinkLeaveEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler{

	List<LinkInfo> links = new ArrayList<LinkInfo>();
	Map<Id,LinkInfo> map = new HashMap<Id,LinkInfo>();

	public QSimDrawer(Scenario sc) {
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
			dx /= 2*length;
			dy /= 2*length;
			dx *= .71/4;
			dy *= .71/4;
			LinkInfo info = new LinkInfo();
			info.lines = new WeightedLine[lanes+1];
			info.cap = (int) (l.getLength()/2);
			for (int i = 0; i <= lanes; i++) {
				WeightedLine line = new WeightedLine();
//				int c1 = MatsimRandom.getRandom().nextInt(lanes);
//				int c1 = MatsimRandom.getRandom().nextInt(lanes);
				final double ii = i+5;
				line.x0 = (float) (l.getFromNode().getCoord().getX() - offsetX + ii*dy);
				line.y0 = (float) (l.getFromNode().getCoord().getY() - offsetY - ii*dx);
				line.x1 = (float) (l.getToNode().getCoord().getX() - offsetX + ii*dy);
				line.y1 = (float) (l.getToNode().getCoord().getY() - offsetY - ii*dx);
				line.r = 255;
				line.a = 32;
				line.weight = i/4.f+1;
				info.lines[i] = line;
				if (i == 0) {
					line.r = 255;
					line.g = 255;
					line.b = 255;
					line.a = 255;
					line.weight = 1;
				}
			}
			Text t = new Text();
			t.x = (float) (l.getCoord().getX() - offsetX + 4*lanes*dy);
			t.y = (float) (l.getCoord().getY() - offsetY - 4*lanes*dx);
			info.t = t;
			t.minScale = 8;
			t.r = t.g = t.b = 222;
			t.text = "0";
			this.links.add(info);
			this.map.put(l.getId(), info);
		}
	}


	@Override
	public void draw(VisDebugger p) {
		for (LinkInfo l : this.links) {
			synchronized (l) {
				if (l.onLink == 0) {
					final WeightedLine ll = l.lines[0];
					p.drawWeightedLine(ll);
					continue;
				}
				int r = l.onLinkR > 0 ? 255 : 0;
				int g = l.onLinkG > 0 ? 255 : 0;
				final int length = l.lines.length;
				int draw = length * l.onLink/l.cap+2;
				for (int i = 1; i < draw && i < length; i++) {
					final WeightedLine ll = l.lines[i];
					ll.r = r;
					ll.g = g;
					p.drawWeightedLine(ll);
				}
				p.drawText(l.t);
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
			info.t.text = Integer.toString(info.onLink);
		}		
	}

	private static final class LinkInfo {
		public int onLinkR = 0;
		public int onLinkG = 0;
		WeightedLine [] lines;
		Text t;
		int onLink;
		int cap;
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
			info.t.text = Integer.toString(info.onLink);
		}
	}
}
