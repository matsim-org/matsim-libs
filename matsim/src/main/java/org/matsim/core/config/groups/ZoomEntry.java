/* *********************************************************************** *
 * project: org.matsim.*
 * ZoomEntry
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
package org.matsim.core.config.groups;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

public final class ZoomEntry  implements Serializable {
	private static final long serialVersionUID = 1L;

	private Rectangle2D zoomstart;
	private BufferedImage snap;
	private String name;

	public ZoomEntry() {

	}

	public ZoomEntry(BufferedImage snap, Rectangle2D zoomstore, String name) {
		super();
		this.snap = snap;
		this.zoomstart = zoomstore;
		this.name = name;
	}

	private void writeObject( java.io.ObjectOutputStream s ) throws IOException {
		s.writeUTF(this.name);
		s.writeDouble(this.zoomstart.getX());
		s.writeDouble(this.zoomstart.getY());
		s.writeDouble(this.zoomstart.getWidth());
		s.writeDouble(this.zoomstart.getHeight());
		ImageIO.write(this.snap, "jpg", s);
	}


	private void readObject( java.io.ObjectInputStream s ) throws IOException {
		this.name = s.readUTF();
		this.zoomstart = new Rectangle2D.Double(s.readDouble(),s.readDouble(),s.readDouble(),s.readDouble());
		this.snap = ImageIO.read(s);
	}

	public Rectangle2D getZoomstart() {
		return this.zoomstart;
	}
	
	public BufferedImage getSnap() {
		return this.snap;
	}
	
	public String getName() {
		return this.name;
	}

}