/* *********************************************************************** *
 * project: org.matsim.*
 * Overlay.java
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

package org.matsim.utils.vis.kml;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.utils.vis.kml.KMLWriter.XMLNS;

/**
 * @author dgrether
 *
 */
public abstract class Overlay extends Feature {
	/**
	 * the color, in hex: alpha blue green red
	 */
  private String color;
  /**
   * This element defines the stacking order for the images in overlapping overlays. Overlays with higher <drawOrder> values are drawn on top of overlays with lower <drawOrder> values.
   */
  private Integer draworder;
  /**
   * Defines the image associated with the Overlay.
   */
	private Icon icon;

	/**
	 *
	 * @param id
	 */
	public Overlay(final String id) {
		super(id);
	}

	/**
	 * @param id
	 * @param name
	 * @param description
	 * @param lookAt
	 * @param styleUrl
	 * @param visibility
	 * @param region
	 * @param timePrimitive
	 */
	public Overlay(final String id, final String name, final String description, final String address, final LookAt lookAt,
			final String styleUrl, final boolean visibility, final Region region,
			final TimePrimitive timePrimitive) {
		super(id, name, description, address, lookAt, styleUrl, visibility, region,
				timePrimitive);
	}

	@Override
	protected void writeObject(final BufferedWriter out, final XMLNS version, final int offset, final String offsetString) throws IOException {

		super.writeObject(out, version, offset + 1, offsetString);

		if (this.color != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<color>");
			out.write(this.color);
			out.write("</color>");
			out.newLine();
		}
		if (this.draworder != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<drawOrder>");
			out.write(this.draworder.toString());
			out.write("</drawOrder>");
			out.newLine();
		}
		if (this.icon != null) {
		  this.icon.writeObject(out, version, offset + 1, offsetString);
		}
  }
	/**
	 *
	 * @param color the color, in hex: alpha blue green red
	 */
	public void setColor(final String color) {
		this.color = color;
	}
	/**
	 *
	 * @param order This element defines the stacking order for the images in overlapping overlays. Overlays with higher <drawOrder> values are drawn on top of overlays with lower <drawOrder> values.
	 */
	public void setDrawOrder(final int order) {
		this.draworder = Integer.valueOf(order);
	}
	/**
	 *
	 * @param icon Defines the image associated with the Overlay.
	 */
	public void setIcon(final Icon icon) {
		this.icon = icon;
	}

}
