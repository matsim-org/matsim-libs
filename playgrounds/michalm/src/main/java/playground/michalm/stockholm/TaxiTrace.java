/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.stockholm;

import java.util.Date;

import org.matsim.api.core.v01.Coord;

public class TaxiTrace {
	final String taxiId;
	final Date time;
	final Coord coord;
	final boolean hired;

	public TaxiTrace(String taxiId, Date time, Coord coord, boolean hired) {
		this.taxiId = taxiId;
		this.time = time;
		this.coord = coord;
		this.hired = hired;
	}
}
