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

import org.matsim.gbl.MatsimResource;
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
		writer.addNonKMLFile(MatsimResource.getAsInputStream("matsim_logo_transparent.png"), "matsimLogo.png");
		Icon icon = new Icon("./matsimLogo.png");
    this.setIcon(icon);
    this.setName("Matsim Logo");
    // place the image bottom left
    Vec2Type overlayXY = new Vec2Type(1.0, -0.7, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
    Vec2Type screenXY = new Vec2Type(0.85, 25, Vec2Type.Units.fraction, Vec2Type.Units.pixels);
    Vec2Type sizeXY = new Vec2Type(0.14, 0.00, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
    this.setOverlayXY(overlayXY);
    this.setScreenXY(screenXY);
    this.setSize(sizeXY);
    this.setDrawOrder(Integer.MAX_VALUE);
	}

}
