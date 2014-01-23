/* *********************************************************************** *
 * project: org.matsim.*
 * TempDirectoryUtil.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.matsim4urbansim.utils;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4urbansim.config.M4UConfigUtils;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.utils.io.Paths;
import org.matsim.core.config.Config;



/**
 * @author thomas
 *
 */
public class OPUSDirectoryUtil {
	private static final Logger log = Logger.getLogger(OPUSDirectoryUtil.class);

	
	public static void setTmpDirectories(Config c){
		UrbanSimParameterConfigModuleV3 module = (UrbanSimParameterConfigModuleV3) c.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		String tmp = Paths.checkPathEnding(System.getProperty("java.io.tmpdir"));
		module.setOpusHome(tmp);
		module.setOpusDataPath(tmp + "data/");
		module.setMATSim4Opus(tmp + "opus_matsim/");
		module.setMATSim4OpusConfig(tmp + "opus_matsim/matsim_config/");
		module.setMATSim4OpusOutput(tmp + "opus_matsim/output/");
		module.setMATSim4OpusTemp(tmp + "opus_matsim/tmp/");
		module.setMATSim4OpusBackup(tmp + "opus_matsim/backup/");
	}
	
	/**
	 * create new temp directories. these will be deleted after each test run.
	 * @param config TODO
	 */
	public static void createOPUSDirectories(Config config){
		log.info("Creating temp directories");
		
		UrbanSimParameterConfigModuleV3 module = M4UConfigUtils.getUrbanSimParameterConfigAndPossiblyConvert(config);
		// set temp directory as opus_home
//		InternalConstants.setOpusHomeDirectory(System.getProperty("java.io.tmpdir"));

		File tempFile = new File(module.getOpusHome());
		tempFile.mkdirs();
		
		tempFile = new File(module.getMATSim4Opus());
		tempFile.mkdirs();
		
		tempFile = new File(module.getMATSim4OpusOutput());
		tempFile.mkdirs();
		
		tempFile = new File(module.getMATSim4OpusTemp());
		tempFile.mkdirs();
		
		tempFile = new File(module.getMATSim4OpusConfig());
		tempFile.mkdirs();
		log.info("Finished creating temp directories");
	}
	
	/**
	 * Removes the output directory for the UrbanSim data
	 * if doesn't existed before the test run. 
	 * @param config TODO
	 */
	public static void cleaningUpOPUSDirectories(Config config){
		log.info("Removing temp directories");
		UrbanSimParameterConfigModuleV3 module = M4UConfigUtils.getUrbanSimParameterConfigAndPossiblyConvert(config);
		File tempFile;// = new File(InternalConstants.getOPUS_HOME());
//		used only for tests. At least on my System this call leads to a null-pointer-exception
//		because there are some other directories in the temp-directory. Especially some hidden ones.
//		\\ DR, jul'13
//		if(tempFile.exists())
//			deleteDirectory(tempFile);
		tempFile = new File(module.getMATSim4Opus());
		if(tempFile.exists())
			org.matsim.contrib.matrixbasedptrouter.utils.TempDirectoryUtil.deleteDirectory(tempFile);
		tempFile = new File(module.getMATSim4OpusOutput());
		if(tempFile.exists())
			org.matsim.contrib.matrixbasedptrouter.utils.TempDirectoryUtil.deleteDirectory(tempFile);
		tempFile = new File(module.getMATSim4OpusTemp());
		if(tempFile.exists())
			org.matsim.contrib.matrixbasedptrouter.utils.TempDirectoryUtil.deleteDirectory(tempFile);
		tempFile = new File(module.getMATSim4OpusConfig());
		if(tempFile.exists())
			org.matsim.contrib.matrixbasedptrouter.utils.TempDirectoryUtil.deleteDirectory(tempFile);	
		log.info("Finished removing temp directories");
	}

}

