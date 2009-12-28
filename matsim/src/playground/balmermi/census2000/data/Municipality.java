/* *********************************************************************** *
 * project: org.matsim.*
 * Municipality.java
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

package playground.balmermi.census2000.data;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.world.Zone;


public class Municipality implements Comparable<Municipality> {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected final Zone zone;
	protected int k_id;
	protected double income; // average monthly income
	protected int reg_type; // degree of urbanization
	protected double fuelcost; // per liter
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Municipality(Zone zone) {
		this.zone = zone;
	}
	
	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public int compareTo(Municipality other) {
		return ((IdImpl)this.zone.getId()).compareTo((IdImpl)other.zone.getId());
	}

	//////////////////////////////////////////////////////////////////////

	public final Id getId() {
		return this.zone.getId();
	}
	
	public final double getIncome() {
		return this.income;
	}

	public final int getRegType() {
		return this.reg_type;
	}
	
	public final int getCantonId() {
		return this.k_id;
	}
	
	public final double getFuelCost() {
		return this.fuelcost;
	}
	
	public final Zone getZone() {
		return this.zone;
	}
	
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[m_id=" + this.getId() + "]" +
			"[k_id=" + this.k_id + "]" +
			"[income=" + this.income + "]" +
			"[reg_type=" + this.reg_type + "]" +
			"[fuelcost=" + this.fuelcost + "]";
	}
}
