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
		}
	}
	
	public double getRho(Id<Link> l) {
		LinkInfo li = this.lis.get(l);
		if (li.onLink < 0) {
			return 0;
		}
		double rho = li.onLink/li.area;
		
		return rho;
	}
	

	public void registerCALink(CALinkDynamic caL) {
		LinkInfo li = new LinkInfo(caL.getWidth()*caL.getLink().getLength());
		this.lis.put(caL.getDownstreamLink().getId(), li);
		if (caL.getUpstreamLink() != null) {
			this.lis.put(caL.getUpstreamLink().getId(), li);
		}
	}
	
	private static final class LinkInfo {

		private final double area;

		int onLink = 0;
		
		public LinkInfo(double area) {
			this.area = area;
		}
		
		
		
	}

}
