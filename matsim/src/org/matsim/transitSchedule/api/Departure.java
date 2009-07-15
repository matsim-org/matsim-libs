/* *********************************************************************** *
 * project: org.matsim.*
 * Departure.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.transitSchedule.api;

import org.matsim.api.basic.v01.Identifiable;
import org.matsim.vehicles.BasicVehicle;

/**
 * Describes a single departure along a route in a transit line.
 * 
 * @author mrieser
 */
public interface Departure extends Identifiable {

	public abstract double getDepartureTime();

	/** Stores with which vehicle this heading departs. Note that this information is not (yet) persistent / stored in file! */
	public abstract void setVehicle(final BasicVehicle vehicle);

	public abstract BasicVehicle getVehicle();

}