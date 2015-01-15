/* *********************************************************************** *
 * project: org.matsim.*
 * Tile.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.debugger.eventsbaseddebugger;

import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import processing.core.PImage;

import com.vividsolutions.jts.geom.Coordinate;

public class Tile {

	private final double tx;
	private final double ty;
	private final double bx;
	private final double by;
	private PImage img;
	private final MathTransform transform;

	public Tile(double tx, double ty, double bx, double by,
			MathTransform transform) {
		this.tx = Math.min(tx, bx);
		this.ty = Math.min(ty, by);
		this.bx = Math.max(tx, bx);
		this.by = Math.max(ty, by);
		this.transform = transform;
		// System.out.println(tx + "\t" + bx);
	}

	public double getTx() {
		return this.tx;
	}

	public double getTy() {
		return this.ty;
	}

	public double getBx() {
		return this.bx;
	}

	public double getBy() {
		return this.by;
	}

	public String getUrl() {
		Coordinate src = new Coordinate(this.tx, this.ty);
		Coordinate dest = new Coordinate();
		Coordinate src1 = new Coordinate(this.bx, this.by);
		Coordinate dest1 = new Coordinate();
		try {
			JTS.transform(src, dest, this.transform);
			JTS.transform(src1, dest1, this.transform);
		} catch (TransformException e) {
			e.printStackTrace();
		}

		String url = "http://localhost:8080/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=bwb&styles=&bbox="
				+ dest.x
				+ ","
				+ dest.y
				+ ","
				+ dest1.x
				+ ","
				+ dest1.y
				+ "&width=256&height=256&srs=EPSG:4326&format=image/jpeg&BGCOLOR=0xFFFFFF";

		return url;
	}

	public void setPImage(PImage img) {
		this.img = img;

	}

	public PImage getPImage() {
		return this.img;
	}

}
