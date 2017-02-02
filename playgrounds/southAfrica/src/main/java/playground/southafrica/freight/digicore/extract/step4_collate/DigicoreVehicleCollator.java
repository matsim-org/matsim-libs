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
	 * A class to collate multiple {@link DigicoreVehicle} XML-files into a 
	 * single {@link DigicoreVehicles} container.
	 * 
	 * @param args the following arguments are required, and in the following 
	 * 		  order:
	 * 		<ol>
	 * 			<li><code>inputFolder</code> containing the <code>xml/</code>
	 * 				folder, which has the {@link DigicoreVehicle} files, as well 
	 * 				as the <code>Vehicles/</code> folder, which contains the 
	 * 				original GPS records for each individual vehicle.
	 * 			<li><code>outputFile</code> to which the {@link DigicoreVehicles}
	 * 				container will be written. This needs to have an <code>.xml</code>
	 * 				or <code>.xml.gz</code> extension.
	 * 			<li><code>CRS</code> the Coordinate Reference System (CRS) used
	 * 				by/in the individual {@link DigicoreVehicle} files.
	 * 			<li><code>descr</code> describing the container contents.
	 * 		<ol> 
	 */
	public static void main(String[] args) {
		Header.printHeader(DigicoreVehicleCollator.class.toGenericString(), args);
		
		String inputFolder = args[0];
		inputFolder += inputFolder.endsWith("/") ? "" : "/";
		
		String outputFile = args[1];
		String CRS = args[2];
		String descr = args[3];
		
		
		/* Only delete the original files if it is explicitly instructed. */
		boolean deleteOriginal = false;
		if(args.length == 5){
			try{
				deleteOriginal = Boolean.parseBoolean(args[4]);
			} catch (Exception e){
				throw new RuntimeException("Could not parse 'delete' argument. Input folder will NOT be deleted.");
			}
		}
		
		String xmlFolder = inputFolder + "xml/";
		String vehiclesfolder = inputFolder + "Vehicles/";
		
		collate(xmlFolder, outputFile, CRS, descr);
		
		if(deleteOriginal){
			FileUtils.delete(new File(xmlFolder));
			FileUtils.delete(new File(vehiclesfolder));
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
	private static void collate(String inputFolder, String outputFile, String crs, String descr){
		LOG.info("Collating the Digicore vehicle files in folder " + inputFolder);
		DigicoreVehicles vehicles = new DigicoreVehicles(crs);
		vehicles.setDescription(descr);
		
		/* Parse the individual vehicle files. */
		List<File> files = FileUtils.sampleFiles(new File(inputFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		DigicoreVehicleReader dvr = null; 
		for(File file : files){
			dvr = new DigicoreVehicleReader();
			dvr.readFile(file.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			vehicles.addDigicoreVehicle(dv);
			dvr = null;
		}
		LOG.info("Done collating the file.");

		/* Write the collated vehicles file. */
		LOG.info("Writing collated vehicles to " + outputFile);
		new DigicoreVehiclesWriter(vehicles).write(outputFile);
		LOG.info("Done writing the collated vehicles.");
	}

}
