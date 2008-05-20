/* *********************************************************************** *
 * project: org.matsim.*
 * TXTExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class TXTExtractor {

	/**
	 * 
	 */
	public TXTExtractor() {

	}

	/**
	 * @param args0
	 *            inputTXTFilename
	 * @param args1
	 *            outputTXTFilename
	 * @param args2
	 *            the String, man is looking for
	 */
	public static void main(String[] args) {
		try {
			BufferedReader reader = IOUtils.getBufferedReader(args[0]);
			BufferedWriter writer = IOUtils.getBufferedWriter(args[1]);
			String line = "";
			do {
				line = reader.readLine();
				if (line != null)
					if (line.contains(args[2]))
						writer.write(line + "\n");
			} while (line != null);
			reader.close();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
