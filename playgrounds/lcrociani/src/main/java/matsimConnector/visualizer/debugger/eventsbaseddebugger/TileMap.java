/* *********************************************************************** *
 * project: org.matsim.*
 * TileMap.java
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

package matsimConnector.visualizer.debugger.eventsbaseddebugger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanListener;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import processing.core.PApplet;
import processing.core.PVector;

public class TileMap implements ZoomPanListener {

	private final int tileSize = 256;

	QuadTree<Tile> currentQuad;
	Map<Double,QuadTree<Tile>> quads = new HashMap<Double, QuadTree<Tile>>();

	Set<Tile> cachedTiles = new HashSet<Tile>();
	private final ZoomPan zoomer;


	double currentZoom = .5;
	private final double offsetY;
	private final double offsetX;
	private boolean viewChanged = true;

	private final TileLoader loader;

	private MathTransform transform;

	private double scale;


	public TileMap(ZoomPan zoomer, PApplet p, double offsetX, double offsetY, String crs) {
		this.zoomer = zoomer;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.loader = new TileLoader(p);
		new Thread(this.loader).start();
		
		CoordinateReferenceSystem sourceCRS = MGC.getCRS(crs);
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:4326");

		try {
			this.transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
		zoomEnded();
	}

	@Override
	public void panEnded() {
		//		System.out.println(this.zoomer.getPanOffset());
		//		this.zoomer.get
		this.viewChanged = true;
		zoomEnded();
	}

	@Override
	public void zoomEnded() {
		this.scale = this.zoomer.getZoomScale();
		double tmp = 1 << (int)(log2(1/this.scale) + 1);
		
		//		if ()
//		tmp /= 2;
		if (this.scale > 4) {
			tmp = .25;
		} else if (this.scale > 2) {
			tmp = .5;
		}
		tmp /= 2;
//		System.out.println(tmp + "   " + this.zoomer.getZoomScale());
		if (tmp != this.currentZoom) {
			QuadTree<Tile> quad = this.quads.get(tmp);
			if (quad == null) {
				quad = new QuadTree<Tile>(10*this.offsetX, 10*this.offsetY, -10*this.offsetX, -10*this.offsetY);
				this.quads.put(tmp, quad);
			}
			this.currentQuad = quad;
			this.currentZoom = tmp;
		}
		this.viewChanged = true;
	}

	public Collection<Tile> getTiles(List<PVector> coords) {

		if (this.viewChanged) {
			this.cachedTiles.clear();
			for (PVector vec : coords) {
				double x = vec.x - this.offsetX;
				double y = -( this.offsetY + vec.y);
//				System.out.println(this.currentZoom);
				double range = this.currentZoom*this.tileSize/2;
				Collection<Tile> tmp = new ArrayList<Tile>();
				 this.currentQuad.getRectangle(x-range, y-range,x+range,y+range,tmp);
//				 System.out.println("one new at:" + x + "  " + y);
				Tile tile = null;
				if (tmp.size() == 0) {
					tile = createAndAddNewTile(x,y);
					this.cachedTiles.add(tile);
//					double cx = (tile.getTx()+tile.getBx())/2;
//					double cy = (tile.getTy()+tile.getBy())/2;
//					double dx = cx-x;
//					double dy = cy-y;
//					double dist = Math.sqrt(dx*dx+dy*dy);
//					System.out.println("dist:" + dist + "  range:" + this.currentZoom*this.tileSize/2);
				} else if (tmp.size() >1){
//					throw new RuntimeException("remove me, after this issue does no longer occur");
					this.cachedTiles.addAll(tmp);
				} else {
					tile = tmp.iterator().next();
					this.cachedTiles.add(tile);
					
				}
			}
			this.viewChanged = false;
		}
//			System.out.println("queried:" + coords.size() + " retrieved:" + this.cachedTiles.size() );
		return this.cachedTiles;
	}

	private Tile createAndAddNewTile(double x, double y) {
		double tx = x - (x%(this.tileSize*this.currentZoom));
		double ty = y - (y%(this.tileSize*this.currentZoom));
		int xSign = 1;
		if (tx < 0) {
			xSign = -1;
		}
		Tile ret = new Tile(tx,ty,tx+this.tileSize*this.currentZoom*xSign,ty+this.tileSize*this.currentZoom,this.transform);
		this.currentQuad.put((ret.getTx()+ret.getBx())/2 , (ret.getTy()+ret.getBy())/2, ret);
		
		this.loader.addTile(ret);
		return ret;
	}

	private double log2(double zoomScale) {

		return Math.log(zoomScale) / Math.log(2);
	}

}
