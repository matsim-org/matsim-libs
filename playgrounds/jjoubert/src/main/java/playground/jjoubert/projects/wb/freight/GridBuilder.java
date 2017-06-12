/* *********************************************************************** *
 * project: org.matsim.*
 * BuildNationalGrid.java
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
package playground.jjoubert.projects.wb.freight;

import org.apache.log4j.Logger;

import playground.southafrica.utilities.Header;

/**
 * Class to read a shapefile, build a grid, and serialise the grid. This is 
 * because building the grid is computationally very expensive.
 *  
 * @author jwjoubert
 */
public class GridBuilder {
	final private static Logger LOG = Logger.getLogger(GridBuilder.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GridBuilder.class.toString(), args);
		
		Header.printFooter();
	}
	
	public static void buildGrid(String[] args){
		
	}

}
