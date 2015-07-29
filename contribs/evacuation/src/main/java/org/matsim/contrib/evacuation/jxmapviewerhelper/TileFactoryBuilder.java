/* *********************************************************************** *
 * project: org.matsim.*
 * TileFactoryBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.jxmapviewerhelper;

import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.wms.WMSService;

public abstract class TileFactoryBuilder {
	
	
	public static TileFactory getOsmTileFactory() {
		final int max=17;
		TileFactoryInfo info = new TileFactoryInfo(0, 17, 17,
				256, true, true,
				"http://tile.openstreetmap.org",
				"x","y","z") {
			@Override
			public String getTileUrl(int x, int y, int zoom) {
				zoom = max-zoom;
				String url = this.baseURL +"/"+zoom+"/"+x+"/"+y+".png";
				return url;
			}

		};
		TileFactory tf = new DefaultTileFactory(info);
		return tf;
	}
	
	public static TileFactory getWMSTileFactory(String baseURL, String layer) {
		
		WMSService service = new WMSService(baseURL, layer);
		
		return new WMSTileFactory(service,17);
		
	}

	
	private static final class WMSTileFactory extends DefaultTileFactory {
		public WMSTileFactory(final WMSService wms, final int maxZoom) {
			super(new TileFactoryInfo(0, maxZoom, maxZoom, 
					256, true, true, // tile size and x/y orientation is r2l & t2b
					"","x","y","zoom") {
				@Override
				public String getTileUrl(int x, int y, int zoom) {
					int zz = maxZoom - zoom;
					int z = (int)Math.pow(2,(double)zz-1);
					return wms.toWMSURL(x-z, z-1-y, zz, getTileSize(zoom));
				}

			});
		}
	}
}
