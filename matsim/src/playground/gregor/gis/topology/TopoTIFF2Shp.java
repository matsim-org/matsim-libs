/* *********************************************************************** *
 * project: org.matsim.*
 * TopoTIFF2Shp.java
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

package playground.gregor.gis.topology;

import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

public class TopoTIFF2Shp {

	public static void main(final String [] args) {
		String tiff = "../../Desktop/padang_200cm_dom1.tif";
		Iterator readers = ImageIO.getImageReadersByFormatName("tiff");
		ImageReader reader = (ImageReader)readers.next();
		
	}
	
}
