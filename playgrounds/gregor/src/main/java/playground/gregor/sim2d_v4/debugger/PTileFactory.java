/* *********************************************************************** *
 * project: org.matsim.*
 * PTileFactory.java
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

package playground.gregor.sim2d_v4.debugger;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import processing.core.PImage;

import com.vividsolutions.jts.geom.Coordinate;

public class PTileFactory {
	private static final Logger log = Logger.getLogger(PTileFactory.class);

	private static final int MAX_CACHE = 4096; // 256*258*24/8/1024/1024*4096 ~ 768MB 

	private final Map<String,PTile> tiles = new ConcurrentHashMap<String,PTile>((int) (MAX_CACHE*1/.75+.5));
	private final LinkedList<String> cacheOrder = new LinkedList<String>();

	private int cached = 0;
	
	private final Loader loader = new Loader();
	private final Thread loaderT = new Thread(this.loader);

	final VisDebugger visDebugger;

	private final MathTransform transform;

	private final double offsetY;

	private final double offsetX;

	public PTileFactory(VisDebugger visDebugger, String crs, double x, double y) {
		this.loaderT.start();
		this.visDebugger = visDebugger;
		this.offsetX = x;
		this.offsetY = y;
		
		
		CoordinateReferenceSystem sourceCRS = MGC.getCRS(crs);
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:4326");

		try {
			this.transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
	}


	public PImage getTile(final int itpx, final int itpy, final double zoom) {
		String key = getKey(itpx, itpy, zoom);//TODO use url as key
		PTile pTile = this.tiles.get(key);

		if (pTile == null) {
			pTile = loadTile(itpx,itpy,zoom);
			if (this.cached >= MAX_CACHE) {
				reviseCache();
			}
			this.tiles.put(key, pTile);
			this.cacheOrder.add(key);
			this.cached++;
		}
		synchronized (pTile) {
			PImage tile = pTile.getPImage();
			if (tile != null) {
				return tile;
			}
		}
		
		return null;
	}

	private void reviseCache() {
		log.info("removing 10% oldest tiles form cache");
		int rm = (int) (MAX_CACHE*.1); //we remove the 10% oldest tiles from cache
		for (int i = 0; i < rm; i++) {
			String key = this.cacheOrder.removeFirst();
			this.tiles.remove(key);
		}
		this.cached -= rm;
		log.info("done.");

	}


	private PTile loadTile(int itpx, int itpy, double zoom) {
		String url = getTileUrl(itpx, itpy, zoom);
		PTile pTile = new PTile(url);
		this.loader.addTile(pTile);
		return pTile;
	}

	private String getKey(final int itpx, final int itpy, final double zoom) {
		StringBuffer bf = new StringBuffer();
		bf.append(itpx);
		bf.append('_');
		bf.append(itpy);
		bf.append('_');
		bf.append(zoom);
		return bf.toString();
	}

	private static final class PTile {

		PImage pImage = null;
		private final String url;

		public PTile(String url) {
			this.url = url;
		}

		public PImage getPImage() {
			return this.pImage;
		}

		public void setPImage(PImage pImage) {
			this.pImage = pImage;
		}

		public String getUrl() {
			return this.url;
		}
	}

	private final class Loader implements Runnable {



		private final BlockingDeque<PTile> tiles = new LinkedBlockingDeque<PTileFactory.PTile>();


		@Override
		public void run() {
			while (true) {
				PTile pTile;
				try {
					pTile = this.tiles.takeFirst();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				synchronized (pTile) {
					String url = pTile.getUrl();
					PImage img = PTileFactory.this.visDebugger.loadImage(url,"png");
					pTile.setPImage(img);
				}				
			}
		}

		public void addTile(PTile pTile) {
			try {
				this.tiles.putFirst(pTile);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	private String getTileUrl(int x, int y, double zoom) {
		//HACK HACK
		double xc = this.offsetX + x*256./zoom;
		double yc = this.offsetY + y*256./zoom;
		double xc1 = this.offsetX + (x+1)*256./zoom;
		double yc1 = this.offsetY + (y+1)*256./zoom;

		Coordinate src = new Coordinate(xc,yc);
		Coordinate dest = new Coordinate();
		Coordinate src1 = new Coordinate(xc1,yc1);
		Coordinate dest1 = new Coordinate();
		try {
			JTS.transform(src, dest, this.transform);
			JTS.transform(src1, dest1, this.transform);
		} catch (TransformException e) {
			e.printStackTrace();
		}

		String url = "http://localhost:8080/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=hh&styles=&bbox=" +
				dest.x +
				"," +
				dest.y +
				"," +
				dest1.x +
				"," +
				dest1.y +
				"&width=256&height=256&srs=EPSG:4326&format=image/png";


		return url;
	}


	
//	public static TileFactory getWMSTileFactory(String baseURL, String layer,String fromCRS, double offsetX, double offsetY) {
//
//		WMSService service = new WMSService(baseURL, layer);
//		CoordinateReferenceSystem sourceCRS = MGC.getCRS(fromCRS);
//		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:4326");
//
//		MathTransform transform;
//		try {
//			transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
//		} catch (FactoryException e) {
//			throw new RuntimeException(e);
//		}
//
//		return new WMSTileFactory(service,25,transform,offsetX, offsetY);
//
//	}
//
//
//	private static final class WMSTileFactory extends DefaultTileFactory {
//		public WMSTileFactory(final WMSService wms, final int maxZoom, final MathTransform transform, final double offsetX, final double offsetY) {
//			super(new TileFactoryInfo(0, maxZoom, maxZoom, 
//					256, true, true, // tile size and x/y orientation is r2l & t2b
//					"","x","y","zoom") {
//				@Override
//				public String getTileUrl(int x, int y, int zoom) {
//					//HACK HACK
//					double xc = offsetX + x*256./zoom;
//					double yc = offsetY + y*256./zoom;
//					double xc1 = offsetX + (x+1)*256./zoom;
//					double yc1 = offsetY + (y+1)*256./zoom;
//
//					Coordinate src = new Coordinate(xc,yc);
//					Coordinate dest = new Coordinate();
//					Coordinate src1 = new Coordinate(xc1,yc1);
//					Coordinate dest1 = new Coordinate();
//					try {
//						JTS.transform(src, dest, transform);
//						JTS.transform(src1, dest1, transform);
//					} catch (TransformException e) {
//						e.printStackTrace();
//					}
//
//					if (dest.x > 8) {
//						System.out.println("stop!!!");
//					}
//
//					String url = "http://localhost:8080/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ch&styles=&bbox=" +
//							dest.x +
//							"," +
//							dest.y +
//							"," +
//							dest1.x +
//							"," +
//							dest1.y +
//							"&width=256&height=256&srs=EPSG:4326&format=image/png";					
//
//					return url;
//				}
//
//			});
//		}
//	}
}
