/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.southAfrica.analysis;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author droeder
 *
 */
public class CopyHistogramms2Folder {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(CopyHistogramms2Folder.class);

	/**
	 * 
	 * @param args OutputDir RunId lastIter
	 */
	public static void main(String[] args) {
		
		
		
		OutputDirectoryHierarchy dir = new OutputDirectoryHierarchy(args[0] + "/" + args[1] + "/", 
				args[1], true, true);
		
		String car, bus, taxi;
		car = dir.getOutputPath() + "/histogrammCar/";
		bus = dir.getOutputPath() + "/histogrammBus/";
		taxi = dir.getOutputPath() + "/histogrammTaxi/";
		new File(car).mkdirs();
		new File(bus).mkdirs();
		new File(taxi).mkdirs();
		log.info("copying histogramm-files...");
		for(int i = 0; i < Integer.parseInt(args[2]); i+=10){
			IOUtils.copyFile(new File(dir.getIterationFilename(i, "legHistogram_bus.png")), 
					new File(bus + args[1] +"." + i + "." +"legHistogram_bus.png"));
			IOUtils.copyFile(new File(dir.getIterationFilename(i, "legHistogram_car.png")), 
					new File(car + args[1] +"." + i + "." +"legHistogram_car.png"));
			IOUtils.copyFile(new File(dir.getIterationFilename(i, "legHistogram_taxi.png")), 
					new File(taxi + args[1] +"." + i + "." +"legHistogram_taxi.png"));
		}
		log.info("finished...");
	}
}

