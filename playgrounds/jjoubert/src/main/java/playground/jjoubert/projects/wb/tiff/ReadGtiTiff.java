/* *********************************************************************** *
 * project: org.matsim.*
 * ReadGtiTiff.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.projects.wb.tiff;

import playground.southafrica.utilities.Header;

/**
 * Class to read the land cover TIFF file provided by GeoTerraImage.
 * 
 * @author jwjoubert
 */
public class ReadGtiTiff {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ReadGtiTiff.class.toString(), args);
		run(args);
		Header.printFooter();
	}
	
	public static void run(String[] args){
		String tiff = args[0];

	}

}
