/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleCollator.java
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
package playground.southafrica.freight.digicore.extract.step4_collate;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesWriter;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to collate different {@link DigicoreVehicle} files into a single 
 * {@link DigicoreVehicles} container. This class <i>should be</i> backward
 * compatible and collate older folders into which vehicle files were 
 * extracted.
 * 
 * @author jwjoubert
 */
public class DigicoreVehicleCollator {
	private final static Logger LOG = Logger.getLogger(DigicoreVehicleCollator.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(DigicoreVehicleCollator.class.toGenericString(), args);
		
		String inputFolder = args[0];
		String outputFile = args[1];
		String CRS = args[2];
		
		/* Only delete the original files if it is explicitly instructed. */
		boolean deleteOriginal = false;
		if(args.length == 4){
			try{
				deleteOriginal = Boolean.parseBoolean(args[3]);
			} catch (Exception e){
				throw new RuntimeException("Could not parse 'delete' argument. Input folder will NOT be deleted.");
			}
		}
		
		collate(inputFolder, outputFile, CRS);
		
		if(deleteOriginal){
			FileUtils.delete(new File(inputFolder));
		}
		
		Header.printFooter();
	}
	
	private DigicoreVehicleCollator() {
		/* Hide the constructor. This class should only be called by its Main
		 * method. */
	}
	
	
	/**
	 * Parses all the individual {@link DigicoreVehicle} files and collating 
	 * them into a single {@link DigicoreVehicles} container, which is written
	 * to file in the end.
	 * 
	 * @param inputFolder
	 * @param outputFile
	 * @param crs
	 */
	private static void collate(String inputFolder, String outputFile, String crs){
		LOG.info("Collating the Digicore vehicle files in folder " + inputFolder);
		DigicoreVehicles vehicles = new DigicoreVehicles(crs);
		
		/* Parse the individual vehicle files. */
		List<File> files = FileUtils.sampleFiles(new File(inputFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		for(File file : files){
			DigicoreVehicleReader dvr = new DigicoreVehicleReader();
			dvr.parse(file.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			vehicles.addDigicoreVehicle(dv);
		}
		LOG.info("Done collating the file.");

		/* Write the collated vehicles file. */
		LOG.info("Writing collated vehicles to " + outputFile);
		new DigicoreVehiclesWriter(vehicles).write(outputFile);
		LOG.info("Done writing the collated vehicles.");
	}

}
