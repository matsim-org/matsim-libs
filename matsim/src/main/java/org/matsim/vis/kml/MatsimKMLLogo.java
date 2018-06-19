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

package org.matsim.vis.kml;

import java.io.IOException;

import org.matsim.core.gbl.MatsimResource;

import net.opengis.kml.v_2_2_0.LinkType;
import net.opengis.kml.v_2_2_0.ObjectFactory;
import net.opengis.kml.v_2_2_0.ScreenOverlayType;
import net.opengis.kml.v_2_2_0.UnitsEnumType;
import net.opengis.kml.v_2_2_0.Vec2Type;

/**
 * A ScreenOverlay representing the MATSim logo
 * 
 * @author dgrether
 */
public class MatsimKMLLogo {
	/**
	 * Writes the logo file to the kmz and creates the ScreenOverlay with the logo
	 * @param writer
	 * @return ScreenOverlay containing the MATSim logo to be added to the KMZ file
	 * @throws IOException if the file with the logo could not be read and written
	 */
	public static ScreenOverlayType writeMatsimKMLLogo(final KMZWriter writer) throws IOException {
		
		writer.addNonKMLFile(MatsimResource.getAsInputStream("matsim_logo_transparent.png"), "matsimLogo.png");
		
		ObjectFactory kmlObjectFactory = new ObjectFactory();

		ScreenOverlayType matsimLogo = kmlObjectFactory.createScreenOverlayType();
		
		LinkType icon = kmlObjectFactory.createLinkType();
		icon.setHref("./matsimLogo.png");
		matsimLogo.setIcon(icon);
		matsimLogo.setName("Matsim Logo");
		Vec2Type overlayXY = kmlObjectFactory.createVec2Type();
		overlayXY.setX(1.0);
		overlayXY.setY(-0.7);
		overlayXY.setXunits(UnitsEnumType.FRACTION);
		overlayXY.setYunits(UnitsEnumType.FRACTION);
		matsimLogo.setOverlayXY(overlayXY);
		Vec2Type screenXY = kmlObjectFactory.createVec2Type();
		screenXY.setX(0.85);
		screenXY.setY(25.0);
		screenXY.setXunits(UnitsEnumType.FRACTION);
		screenXY.setYunits(UnitsEnumType.PIXELS);
		matsimLogo.setScreenXY(screenXY);
		Vec2Type sizeXY = kmlObjectFactory.createVec2Type();
		sizeXY.setX(0.14);
		sizeXY.setY(0.0);
		sizeXY.setXunits(UnitsEnumType.FRACTION);
		sizeXY.setYunits(UnitsEnumType.FRACTION);
		matsimLogo.setSize(sizeXY);
		matsimLogo.setDrawOrder(Integer.MAX_VALUE);
		
		return matsimLogo;
	}

}
