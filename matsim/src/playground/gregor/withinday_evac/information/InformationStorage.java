/* *********************************************************************** *
 * project: org.matsim.*
 * InformationStorage.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.withinday_evac.information;

import java.util.Collection;
import java.util.PriorityQueue;

public class InformationStorage {
	
	private final PriorityQueue<InformationEntity> infos;
	private double lastUpdate = 0.;
	
	public InformationStorage() {
		this.infos = new PriorityQueue<InformationEntity>();
	}
	
	public void addInformationEntity(InformationEntity ie) {
		this.infos.add(ie);
	}
	
	private synchronized void update(double now) {
		
		if (this.infos.size() == 0) {
			this.lastUpdate = now;
			return;
		}
		while (this.infos.peek().getEndTime() < now) {
			this.infos.poll();
		}
		this.lastUpdate = now;
	}
	
	public Collection<InformationEntity> getInformation(double now) {
		if (now > this.lastUpdate) {
			update(now);
		}
		return this.infos;
	}

}
