/* *********************************************************************** *
 * project: org.matsim.*
 * WGS84ToMercator.java
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

package org.matsim.vis.otfvis.utils;

import org.jxmapviewer.viewer.util.MercatorUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

public class WGS84ToMercator {

	public static class Project implements CoordinateTransformation {

		private double radius;

		public Project(int zoom) {
			int tileSize = 256;
			int circumference = widthOfWorldInPixels(zoom, tileSize);
		    this.radius = circumference / (2* Math.PI);
		}

		@Override
		public Coord transform(Coord coord) {
			Coord otherResult; 
			double elevation;
			try{
				elevation = coord.getZ();
				otherResult = new Coord((double) MercatorUtils.longToX(coord.getX(), radius), (double) MercatorUtils.latToY(coord.getY(), radius), elevation);
			} catch (Exception e){
				otherResult = new Coord((double) MercatorUtils.longToX(coord.getX(), radius), (double) MercatorUtils.latToY(coord.getY(), radius));
			}
			return otherResult;
		}

		private int widthOfWorldInPixels(int zoom, int TILE_SIZE) {
	        int tiles = (int)Math.pow(2 , zoom);
	        int circumference = TILE_SIZE * tiles;
	        return circumference;
	    }
		
	}


	public static class Deproject implements CoordinateTransformation {

		private double radius;
		
		public Deproject(int zoom) {
			int tileSize = 256;
			int circumference = widthOfWorldInPixels(zoom, tileSize);
		    this.radius = circumference / (2* Math.PI);
		}

		@Override
		public Coord transform(Coord coord) {
			Coord otherResult; 
			double elevation;
			try{
				elevation = coord.getZ();
				otherResult = new Coord(MercatorUtils.xToLong((int) coord.getX(), radius), MercatorUtils.yToLat((int) coord.getY(), radius), elevation);
			} catch (Exception e){
				otherResult = new Coord(MercatorUtils.xToLong((int) coord.getX(), radius), MercatorUtils.yToLat((int) coord.getY(), radius));
			}
			return otherResult;
		}

		private int widthOfWorldInPixels(int zoom, int TILE_SIZE) {
	        int tiles = (int)Math.pow(2 , zoom);
	        int circumference = TILE_SIZE * tiles;
	        return circumference;
	    }
		
	}
	
}
