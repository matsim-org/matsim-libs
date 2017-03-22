/* *********************************************************************** *
 * project: org.matsim.*
 * PointListAlgorithmRunner.java
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
package playground.southafrica.freight.digicore.algorithms.djcluster.pointList;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.freight.digicore.io.algorithms.PointListAlgorithm;
import playground.southafrica.utilities.Header;

/**
 * Implementation class to read a vehicles container file, but instead of adding 
 * the vehicles to an instantiated container, their activities are processed and
 * mapped into maps of a demarcated shapefile.
 * 
 * @author jwjoubert
 */
public class RunPointListAlgorithm {
	final private static Logger LOG = Logger.getLogger(RunPointListAlgorithm.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RunPointListAlgorithm.class.toString(), args);
		String vehiclescontainer = args[0];
		String shapefile = args[1];
		int idField = Integer.parseInt(args[2]);
		
		LOG.info("Initialising the container...");
		DigicoreVehicles vehicles = new DigicoreVehicles();
		DigicoreVehiclesReader dvr = new DigicoreVehiclesReader(vehicles);
		dvr.clearAlgorithms();
		PointListAlgorithm pla = null;
		LOG.info("Initialising the algorithm...");
		try {
			pla = new PointListAlgorithm(shapefile, idField);
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.info("Setting the root...");
		pla.setRoot(new File(vehiclescontainer).getParent());
		LOG.info("Adding the algorithm...");
		dvr.addAlgorithm(pla);
		LOG.info("Reading the file...");
		dvr.readFile(vehiclescontainer);
		
		pla.printCounter();
		pla.printNumberOfEmptyMaps();
		
		Header.printFooter();
	}

}
