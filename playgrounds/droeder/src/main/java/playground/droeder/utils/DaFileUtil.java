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
package playground.droeder.utils;

import java.io.File;

import org.apache.log4j.Logger;

/**
 * @author droeder
 *
 */
public class DaFileUtil {

	private static final Logger log = Logger.getLogger(DaFileUtil.class);

	private DaFileUtil() {
	}

	/**
	 * @param outputdirectory
	 * @return
	 */
	public static String checkFileEnding(String outputdirectory) {
		String fileSeparator = System.getProperty("file.separator");
		String toReplace;
		if(fileSeparator.equals("\\")){
			toReplace = "/";
		}else{
			toReplace = "\\";
		}
		String s = outputdirectory;
		s = s.replace(toReplace, fileSeparator);
		s = (s.endsWith(fileSeparator)) ? s :  (s + fileSeparator);
		return s;
	}
	
	public static void checkAndMaybeCreateDirectory(String outputdirectory, boolean overwrite){
		File toTest = new File(outputdirectory);
		if(toTest.isFile()) throw new IllegalArgumentException(outputdirectory + " is a file.");
		if(toTest.exists()){
			if(overwrite){
				toTest.mkdirs();
			}else{
				throw new IllegalArgumentException(outputdirectory + " " +
						"is a directory, but already exists. Overwritting must be allowed explicitly.");
			}
		}else{
			toTest.mkdirs();
		}
		log.info("created directory: " + outputdirectory);
	}
}

