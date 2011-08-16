package org.matsim.core.utils.geometry.transformations;

import org.jdesktop.swingx.mapviewer.util.MercatorUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
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
		    CoordImpl otherResult = new CoordImpl(MercatorUtils.longToX(coord.getX(), radius), MercatorUtils.latToY(coord.getY(), radius));
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
		    CoordImpl otherResult = new CoordImpl(MercatorUtils.xToLong( (int) coord.getX(), radius), MercatorUtils.yToLat( (int) coord.getY(), radius));
			return otherResult;
			
		}

		private int widthOfWorldInPixels(int zoom, int TILE_SIZE) {
	        int tiles = (int)Math.pow(2 , zoom);
	        int circumference = TILE_SIZE * tiles;
	        return circumference;
	    }
		
	}
	
}
