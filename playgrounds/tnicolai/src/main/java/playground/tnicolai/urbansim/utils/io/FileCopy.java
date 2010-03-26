/* *********************************************************************** *
 * project: org.matsim.*
 * FileCopy.java
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
package playground.tnicolai.urbansim.utils.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * @author thomas
 *
 */
public class FileCopy {
	
	// logger
	private static final Logger log = Logger.getLogger(FileCopy.class);
	
	/**
	 * 
	 * @param sourceFile
	 * @param outputFile
	 * @return 
	 * @throws IOException
	 * @throws Exception
	 */
	public static boolean fileCopy(File sourceFile, File outputFile) throws IOException, Exception{

			log.info("Copying generated outputPlans file " + sourceFile.getCanonicalPath() + " to " + outputFile.getCanonicalPath());
			return writeBinaryFile(readBinaryFile(sourceFile), outputFile);
	}
	
	/** 
	* reads a binary input file into a byte array
	* 
	* @param fileName  binary input file
	* @return          byte[] or null
	*/ 
	public static byte[] readBinaryFile(File sourceFile) { 
      byte[] result = null; 
      try { 
         BufferedInputStream input;   
         input = new BufferedInputStream(new FileInputStream(sourceFile)); 
         int num = input.available(); 
         result = new byte[num]; 
         input.read(result, 0, num); 
         input.close();      
      } catch(Exception e) { 
         e.printStackTrace(); 
         result = null; 
      } 
      return result; 
   }
	
	/** 
	* writes a byte array into a given output file 
    * 
    * @param data     output data
    * @param outputFile output file 
    * @return         true if successful
    */ 
	public static boolean writeBinaryFile(byte[] data, File outputFile) { 
		boolean result = true; 
 
		try { 
			BufferedOutputStream output;   
			output = new BufferedOutputStream(new FileOutputStream(outputFile)); 
			output.write(data, 0, data.length); 
			output.close();      
		} catch(Exception e) { 
			e.printStackTrace(); 
			result = false; 
		} 
		return result; 
	} 
	
}

