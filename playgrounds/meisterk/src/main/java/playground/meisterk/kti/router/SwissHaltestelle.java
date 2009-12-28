/* *********************************************************************** *
 * project: org.matsim.*
 * SwissHaltestelle.java
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

package playground.meisterk.kti.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class SwissHaltestelle {

	private final Id id;
	private final Coord coord;

	/*package*/ SwissHaltestelle(Id id, Coord coord) {
		super();
		this.id = id;
		this.coord = coord;
	}

	public Id getId() {
		return id;
	}

	public Coord getCoord() {
		return coord;
	}

}
