/* *********************************************************************** *
 * project: org.matsim.*
 * Utils3D.java
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

/**
 * 
 */
package playground.southafrica.utilities.coord3D;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.osmtools.srtm.SrtmTile;

/**
 * A number of utilities to deal with 3D networks.
 * 
 * @author jwjoubert
 */
public class Utils3D {
	final private static Logger LOG = Logger.getLogger(Utils3D.class);
	private final static String SRTM_URL_AFRICA = "https://dds.cr.usgs.gov/srtm/version2_1/SRTM3/Africa/";

 	public static String getSrtmTile(Coord c){
		int lon = (int)Math.floor(c.getX());
		int lat = (int)Math.floor(c.getY());
		String lonPrefix = lon < 0 ? "W" : "E";
		String latPrefix = lat < 0 ? "S" : "N";
		return String.format("%s%02d%s%03d", latPrefix, Math.abs(lat), lonPrefix, Math.abs(lon));
 	}

 	/**
 	 * Estimating elevation from the Shuttle Radar Topography Mission (SRTM)
 	 * data hosted by the US Geological Survey.
 	 * 
 	 * @param pathToTiles absolute path where SRTM tiles are cached locally;
 	 * @param c coordinate in the WGS84 (decimal degrees) coordinate reference 
 	 * 		  system (CRS).
 	 * @return
 	 */
 	public static double estimateSrtmElevation(String pathToTiles, Coord c){
 		pathToTiles += pathToTiles.endsWith("/") ? "" : "/";
 		String tileName = getSrtmTile(c);
 		String tileFileName = pathToTiles + tileName + ".hgt";
 		File tileFile = new File(tileFileName);
 		
 		/* Download the tile file if it does not exist. */
 		if(!tileFile.exists()){
 			LOG.warn("Tile " + tileFileName + " is not available locally. Downloading...");
 			Runtime rt = Runtime.getRuntime();
 			String url = SRTM_URL_AFRICA + tileName + ".hgt.zip";
 			try {
				Process p1 = rt.exec("curl -o " + tileFileName + ".zip " + url);
				while(p1.isAlive()){ /* Wait */ }
				Process p2 = rt.exec("unzip " + tileFileName + ".zip -d " + pathToTiles);
				while(p2.isAlive()){ /* Wait */ }
				Process p3 = rt.exec("rm " + tileFileName + ".zip");
				while(p3.isAlive()){ /* Wait */ }
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not download SRTM tile file.");
			}
 		}
 		
 		/* Estimate the elevation. */
		SrtmTile srtmTile = new SrtmTile(tileFile);
 		return srtmTile.getElevation(c.getX(), c.getY());
 	}

}
