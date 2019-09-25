
/* *********************************************************************** *
 * project: org.matsim.*
 * FakeFacility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.pt.router;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.Facility;

public final class FakeFacility implements Facility {
	private Coord coord;
	public FakeFacility( Coord coord ) { this.coord = coord ; }
	@Override public Coord getCoord() {
		return this.coord ;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Id getLinkId() {
		throw new RuntimeException("not implemented") ;
	}

	@Override public Id<Facility> getId(){
		throw new RuntimeException( "not implemented" );
	}
}
