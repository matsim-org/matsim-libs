package org.matsim.contrib.common.zones;

import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;

import javax.annotation.Nullable;
import java.util.List;

public interface Zone extends BasicLocation, Identifiable<Zone> {
	@Nullable
	PreparedPolygon getPreparedGeometry();

	Coord getCentroid();
	String getType();

}


///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package org.matsim.contrib.zone;
//
//import org.locationtech.jts.geom.MultiPolygon;
//import org.matsim.api.core.v01.BasicLocation;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Identifiable;
//import org.matsim.core.utils.geometry.geotools.MGC;
//
//public class Zone implements BasicLocation, Identifiable<Zone> {
//	private final Id<Zone> id;
//	private final String type;
//
//	private MultiPolygon multiPolygon;
//	private Coord centroid;
//
//	public Zone(Id<Zone> id, String type) {
//		this.id = id;
//		this.type = type;
//	}
//
//	public Zone(Id<Zone> id, String type, Coord centroid) {
//		this.id = id;
//		this.type = type;
//		this.centroid = centroid;
//	}
//
//	public Zone(Id<Zone> id, String type, MultiPolygon multiPolygon) {
//		this.id = id;
//		this.type = type;
//
//		this.multiPolygon = multiPolygon;
//		centroid = MGC.point2Coord(multiPolygon.getCentroid());
//	}
//
//	@Override
//	public Id<Zone> getId() {
//		return id;
//	}
//
//	@Override
//	public Coord getCoord() {
//		return centroid;
//	}
//
//	public void setCoord(Coord coord) {
//		this.centroid = coord;
//	}
//
//	public String getType() {
//		return type;
//	}
//
//	public MultiPolygon getMultiPolygon() {
//		return multiPolygon;
//	}
//
//	public void setMultiPolygon(MultiPolygon multiPolygon) {
//		this.multiPolygon = multiPolygon;
//		centroid = MGC.point2Coord(multiPolygon.getCentroid());
//	}
//}

