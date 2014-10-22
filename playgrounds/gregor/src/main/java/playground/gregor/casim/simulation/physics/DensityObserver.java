/* *********************************************************************** *
 * project: org.matsim.*
 * DensityObserver.java
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

package playground.gregor.casim.simulation.physics;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;

public class DensityObserver implements LinkEnterEventHandler, LinkLeaveEventHandler{

	private final Map<Id<Link>,LinkInfo> lis = new HashMap<Id<Link>,LinkInfo>();
	
	public DensityObserver(EventsManager em) {
		em.addHandler(this);
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		LinkInfo li = this.lis.get(event.getLinkId());
		if (li != null) {
			li.onLink--;
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		LinkInfo li = this.lis.get(event.getLinkId());
		if (li != null) {
			li.onLink++;
			if (!li.connected ) {
				li.connect();
				li.connected = true;
			}
		}
	}
	
	public double getRho(Id<Link> l) {
		LinkInfo li = this.lis.get(l);
		if (li.onLink < 0) {
			return 0;
		}
		double rho = li.onLink/li.area;
		
//		if (l.toString().equals("1")) {
//			return 0.5*rho+0.5*getRho(Id.createLinkId("2"));
//		}
//		if (l.toString().equals("3")) {
//			return 0.5*rho+0.5*getRho(Id.createLinkId("2"));
//		}
		
//		double denom = 1;
//		
//		LinkInfo dsLi = this.lis.get(li.dsId);
//		if (dsLi != null && dsLi.onLink > 0) {
//			double dsRho = dsLi.onLink/dsLi.area;
//			rho += dsRho;
//			denom++;
//		}		
//
//		LinkInfo usLi = this.lis.get(li.usId);
//		if (usLi != null && usLi.onLink > 0) {
//			double usRho = usLi.onLink/usLi.area;
//			rho += usRho;
//			denom++;
//		}		
//		
//		rho /= denom;
		
		return rho;
	}
	

	public void registerCALink(CALinkDynamic caL) {
		LinkInfo li = new LinkInfo(caL.getWidth()*caL.getLink().getLength(),caL.getLink());
		this.lis.put(caL.getDownstreamLink().getId(), li);
		if (caL.getUpstreamLink() != null) {
			this.lis.put(caL.getUpstreamLink().getId(), li);
		}
	}
	
	private static final class LinkInfo {

		public boolean connected = false;

		private final double area;

		int onLink = 0;

		private Link link;

		private Id<Link> dsId;

		private Id<Link> usId;
		
		public LinkInfo(double area, Link link) {
			this.area = area;
			this.link = link;
		}
		
		//experimental
		public void connect(){
			for (Link l : link.getToNode().getOutLinks().values()) {
				if (l.getToNode() == link.getFromNode()) {
					continue;
				}
				if (l.getId().toString().contains("ex")) {
					continue;
				}
				this.dsId = l.getId();
			}
			for (Link l : link.getFromNode().getOutLinks().values()) {
				if (l.getToNode() == link.getToNode()) {
					continue;
				}
				if (l.getId().toString().contains("ex")) {
					continue;
				}
				this.usId = l.getId();
			}
		}
		
		
	}

}
