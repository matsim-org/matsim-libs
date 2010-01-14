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
package org.matsim.vis.otfvis.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

import org.matsim.vis.otfvis.opengl.gl.Point3f;

public class ZoomEntry  implements Serializable{
	private static final long serialVersionUID = 1L;

	private Point3f zoomstart;
  private BufferedImage snap;
  private String name;
	
	public ZoomEntry() {
		
	}
	
	public ZoomEntry(BufferedImage snap, Point3f zoomstart, String name) {
		super();
		this.snap = snap;
		this.zoomstart = zoomstart;
		this.name = name;
	}

	private void writeObject( java.io.ObjectOutputStream s ) throws IOException {
		s.writeUTF(this.name);
		s.writeFloat(this.zoomstart.x);
		s.writeFloat(this.zoomstart.y);
		s.writeFloat(this.zoomstart.z);
		ImageIO.write(this.snap, "jpg", s);
	}


	private void readObject( java.io.ObjectInputStream s ) throws IOException {
		this.name = s.readUTF();
		this.zoomstart = new Point3f(s.readFloat(),s.readFloat(),s.readFloat());
		this.snap = ImageIO.read(s);
	}
	
	 public Point3f getZoomstart() {
	    return this.zoomstart;
	  }
	  public BufferedImage getSnap() {
	    return this.snap;
	  }
	  public String getName() {
	    return this.name;
	  }

}