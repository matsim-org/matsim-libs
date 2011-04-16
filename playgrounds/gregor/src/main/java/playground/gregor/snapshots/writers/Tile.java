/* *********************************************************************** *
 * project: org.matsim.*
 * Tile.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.snapshots.writers;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;

public class Tile { // implements Comparable<Tile> {

	public static final int LENGTH = 256;
	
//	int zoomLevel;
	private TextureData tx;
	private Texture tex;
	
	public float tX;
	public float tY;

	public float sX;

	public float sY;

	public double zoom;

//	private double time;
	public String id;

	public int x;

	public int y;

	public double key;

	public boolean locked;

//	public int compareTo(Tile o) {
//		if (this.getTime() > o.getTime()) {
//			return -1;
//		} else if (this.getTime() < o.getTime()){
//			return 1;
//		}
//		return 0;
//	}

	public void setTx(TextureData tx) {
		this.tx = tx;
	}

	public TextureData getTx() {
		return this.tx;
	}

	public void setTex(Texture tex) {
		this.tex = tex;
	}

	public Texture getTex() {
		return this.tex;
	}

//	public void setTime(double time) {
//		this.time = time;
//	}
//
//	public double getTime() {
//		return this.time;
//	}
	
}
