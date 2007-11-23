/* *********************************************************************** *
 * project: org.matsim.*
 * ScreenOverlay.java
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
import org.matsim.utils.vis.kml.fields.Vec2Type;

/**
 * @author dgrether
 * @see <a 
 * href="http://code.google.com/apis/kml/documentation/kml_tags_21.html#screenoverlay">
 * KML-ScreenOverlay [code.google.com]</a>
 */
public class ScreenOverlay extends Overlay {

	/**
	 * Specifies a point on (or outside of) the overlay image that is mapped to
	 * the screen coordinate (<screenXY>). It requires x and y values, and the
	 * units for those values. The x and y values can be specified in three
	 * different ways: as pixels ("pixels"), as fractions of the image
	 * ("fraction"), or as inset pixels ("insetPixels"), which is an offset in
	 * pixels from the upper right corner of the image. The x and y positions can
	 * be specified in different ways—for example, x can be in pixels and y can be
	 * a fraction. The origin of the coordinate system is in the lower left corner
	 * of the image.
	 * 
	 * x - Either the number of pixels, a fractional component of the image, or a
	 * pixel inset indicating the x component of a point on the overlay image.
	 * 
	 * y - Either the number of pixels, a fractional component of the image, or a
	 * pixel inset indicating the y component of a point on the overlay image.
	 * 
	 * xunits - Units in which the x value is specified. Default="fraction". A
	 * value of "fraction" indicates the x value is a fraction of the image. A
	 * value of "pixels" indicates the x value in pixels. A value of "insetPixels"
	 * indicates the indent from the right edge of the image.
	 * 
	 * yunits - Units in which the y value is specified. Default="fraction". A
	 * value of "fraction" indicates the y value is a fraction of the image. A
	 * value of "pixels" indicates the y value in pixels. A value of "insetPixels"
	 * indicates the indent from the top edge of the image.
	 */
	private Vec2Type overlayXY;

	/**
	 * Specifies a point relative to the screen origin that the overlay image is
	 * mapped to. The x and y values can be specified in three different ways: as
	 * pixels ("pixels"), as fractions of the screen ("fraction"), or as inset
	 * pixels ("insetPixels"), which is an offset in pixels from the upper right
	 * corner of the screen. The x and y positions can be specified in different
	 * ways—for example, x can be in pixels and y can be a fraction. The origin of
	 * the coordinate system is in the lower left corner of the screen.
	 * 
	 * x - Either the number of pixels, a fractional component of the screen, or a
	 * pixel inset indicating the x component of a point on the screen.
	 * 
	 * y - Either the number of pixels, a fractional component of the screen, or a
	 * pixel inset indicating the y component of a point on the screen.
	 * 
	 * xunits - Units in which the x value is specified. Default="fraction". A
	 * value of "fraction" indicates the x value is a fraction of the screen. A
	 * value of "pixels" indicates the x value in pixels. A value of "insetPixels"
	 * indicates the indent from the right edge of the screen.
	 * 
	 * yunits - Units in which the y value is specified. Default=fraction. A value
	 * of fraction indicates the y value is a fraction of the screen. A value of
	 * "pixels" indicates the y value in pixels. A value of "insetPixels"
	 * indicates the indent from the top edge of the screen.
	 * 
	 */
	private Vec2Type screenXY;

	/**
	 * Point relative to the screen about which the screen overlay is rotated.
	 */
	private Vec2Type rotationXY;

	/**
	 * Specifies the size of the image for the screen overlay, as follows:
	 * 
	 * A value of −1 indicates to use the native dimension A value of 0 indicates
	 * to maintain the aspect ratio A value of n sets the value of the dimension
	 */
	private Vec2Type size;
	/**
	 * Indicates the angle of rotation of the parent object. A value of 0 means no
	 * rotation. The value is an angle in degrees counterclockwise starting from
	 * north. Use ±180 to indicate the rotation of the parent object from 0. The
	 * center of the <rotation>, if not (.5,.5), is specified in <rotationXY>.
	 */
	private Float rotation;

	/**
	 * @param id
	 */
	public ScreenOverlay(String id) {
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
	public ScreenOverlay(String id, String name, String description, String address,
			LookAt lookAt, String styleUrl, boolean visibility, Region region,
			TimePrimitive timePrimitive) {

		super(id, name, description, address, lookAt, styleUrl, visibility, region,
				timePrimitive);
	}

	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset,
			String offsetString) throws IOException {
		out.write(Object.getOffset(offset, offsetString));
		out.write("<ScreenOverlay>");
		out.newLine();
		
		super.writeObject(out, version, offset + 1, offsetString);

		if (this.overlayXY != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<overlayXY ");
			out.write(this.overlayXY.toString());
			out.write("/>");
			out.newLine();
		}
		if (this.screenXY != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<screenXY ");
			out.write(this.screenXY.toString());
			out.write("/>");
			out.newLine();
		}
		if (this.rotationXY != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<rotationXY ");
			out.write(this.rotationXY.toString());
			out.write("/>");
			out.newLine();
		}
		if (this.size != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<size ");
			out.write(this.size.toString());
			out.write("/>");
			out.newLine();
		}
		if (this.rotation != null) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<rotation>");
			out.write(rotation.toString());
			out.write("</rotation>");
			out.newLine();
		}

		out.write(Object.getOffset(offset, offsetString));
		out.write("</ScreenOverlay>");
		out.newLine();

	}
	/**
	 * Sets the overlay xy vector of this screenoverlay
	 * @param overlayxy
	 */
	public void setOverlayXY(Vec2Type overlayxy) {
		this.overlayXY = overlayxy;
	}
	/**
	 * sets the screen xy vector of this ScreenOverlay
	 * @param screenxy 
	 */
	public void setScreenXY(Vec2Type screenxy) {
		this.screenXY = screenxy;
	}

	/**
	 * @param rotationXY Point relative to the screen about which the screen overlay is rotated.
	 */
	public void setRotationXY(Vec2Type rotationXY) {
		this.rotationXY = rotationXY;
	}
	
	/**
	 * Specifies the size of the image for the screen overlay, as follows:
	 * <ul>
	 * <li>A value of −1 indicates to use the native dimension</li>
	 * <li>A value of 0 indicates to maintain the aspect ratio</li>
	 * <li>A value of n sets the value of the dimension</li>
	 * </ul>
	 * The size can be specified individually for the x- and y-direction 
	 *
	 * @param size
	 */
	public void setSize(Vec2Type size) {
		this.size = size;
	}
	
	/**
	 * Indicates the angle of rotation of the parent object. A value of 0 means no
	 * rotation. The value is an angle in degrees counterclockwise starting from
	 * north. Use ±180 to indicate the rotation of the parent object from 0. The
	 * center of the rotation if not (.5,.5), is specified
	 * in {@link #setRotationXY(Vec2Type)}.
	 *
	 * @param rotation
	 */
	public void setRotation(Float rotation) {
		this.rotation = rotation;
	}
}
