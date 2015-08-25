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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.log4j.Logger;


/**
 * @author thomas
 *
 */
class TempDirectoryUtil {


	/**
	 * creates a custom temp directory
	 * @param customDirectory
	 * @return canonical path of the custom temp directory
	 */
	public static String createCustomTempDirectory(String customDirectory){
		try {
			Path tempDirectory = Files.createTempDirectory(customDirectory);
			File tempFile = tempDirectory.toFile();
			return tempFile.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}

