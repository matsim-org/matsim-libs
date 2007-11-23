/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimKMLLogo.java
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

package org.matsim.utils.vis.matsimkml;

import java.io.IOException;

import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.ScreenOverlay;
import org.matsim.utils.vis.kml.fields.Vec2Type;


/**
 * A ScreenOverlay representing the MATSim logo
 * @author dgrether
 *
 */
public class MatsimKMLLogo extends ScreenOverlay {
	/**
	 * Writes the logo file to the kmz and creates the ScreenOverlay with the logo
	 * @param writer
	 * @throws IOException if the file with the logo could not be read and written
	 */
	public MatsimKMLLogo(final KMZWriter writer) throws IOException {
		super("matsimlogo");
		writer.addNonKMLFile("res/matsim_logo_transparent_small.png", "matsimLogo.png");
		Icon icon = new Icon("./matsimLogo.png");
// Icon("http://code.google.com/apis/kml/documentation/top_left.jpg");
    this.setIcon(icon);
    this.setName("Matsim Logo");
    // place the image bottom left
    Vec2Type overlayXY = new Vec2Type(1.0d, 0.0d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
    Vec2Type screenXY = new Vec2Type(0.85d, 0.05d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
    this.setOverlayXY(overlayXY);
    this.setScreenXY(screenXY);
    this.setDrawOrder(Integer.MAX_VALUE);
	}

}
