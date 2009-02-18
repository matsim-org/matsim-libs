/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.population;

import org.matsim.basic.v01.BasicVehicleImpl;
import org.matsim.basic.v01.BasicVehicleType;
import org.matsim.interfaces.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class VehicleImpl extends BasicVehicleImpl implements Vehicle {

	private BasicVehicleType type;

	/**
	 * @param id
	 * @param type
	 */
	public VehicleImpl(Id id, BasicVehicleType type) {
		super(id, type.getTypeId());
		this.type = type;
	}

	public BasicVehicleType getType() {
		return this.type;
	}

}
