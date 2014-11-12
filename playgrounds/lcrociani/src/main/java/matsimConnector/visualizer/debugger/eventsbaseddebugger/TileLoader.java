/* *********************************************************************** *
 * project: org.matsim.*
 * TileLoader.java
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

import java.io.File;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import processing.core.PApplet;
import processing.core.PImage;

public class TileLoader implements Runnable{

	
	
	private final BlockingDeque<Tile> tiles = new LinkedBlockingDeque<Tile>();
	private final PApplet p;

	public TileLoader(PApplet p) {
		this.p = p;

	}

	@Override
	public void run() {
		while (true) {
			Tile pTile;
			try {
				pTile = this.tiles.takeFirst();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			String url;
			synchronized (pTile) {
				url = pTile.getUrl();
			}
			
			String path = "/Users/laemmel/tmp/cache/"+url + ".png";
			if (new File(path).isFile()) {
				url = path;
			}
			PImage img = this.p.loadImage(url,"png");
			synchronized (pTile) {
				pTile.setPImage(img);
			}
			if (!new File(path).isFile()) {
				img.save(path);
			}
		}
	}

	public void addTile(Tile pTile) {
		try {
			this.tiles.putFirst(pTile);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	public void loadDirectly(Tile t, String path) {
		PImage img = this.p.loadImage(path,"png");
		t.setPImage(img);
	}

}
