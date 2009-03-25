/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationAreaLink.java
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

package playground.gregor.sims.evacbase;

import org.matsim.core.basic.v01.IdImpl;

/**
 * Simple evacuation link description, according to the evacuation area xml file.
 *
 * @author glaemmel
 */
public class EvacuationAreaLink implements Comparable<EvacuationAreaLink> {
	private IdImpl id;
	private double deadline;

	public EvacuationAreaLink(IdImpl id, double deadline){
		this.id = id;
		this.deadline = deadline;
	}
	public EvacuationAreaLink(String id, double deadline){
		this.id = new IdImpl(id);
		this.deadline = deadline;
	}

	public IdImpl getId() { return this.id; }

	public double getDeadline() { return this.deadline; }

	public void setId(IdImpl id) { this.id = id; }

	public void setDeadline(double deadline) { this.deadline = deadline; }

	//compare operator for sorting the EvacuationAreaLinks according to their deadline
	public int compareTo(EvacuationAreaLink o) {
		if (this.deadline < o.getDeadline()) return -1;
		if (this.deadline > o.getDeadline()) return 1;
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof EvacuationAreaLink) {
			return compareTo((EvacuationAreaLink)o) == 0;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

}
