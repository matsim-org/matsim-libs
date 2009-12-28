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
package playground.yu.utils.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class TXTExtractor {

	private static void printUsage() {
		System.out.println("----------------");
		System.out.println("TXTExtractor:");
		System.out
				.println("Creates a new .txt-file from a old normal .txt-file, only the lines, which contains the specified sequence of char values, should be included");
		System.out.println();
		System.out.println("usage: TXTExtractor args");
		System.out.println(" arg 0: path to the old .txt-file (required)");
		System.out.println(" arg 1: path to the new .txt-file (required)");
		System.out
				.println(" arg 2: the specified sequence of char values (required)");
		System.out.println("----------------");
	}

	/**
	 * @param args0
	 *            inputTXTFilename
	 * @param args1
	 *            outputTXTFilename
	 * @param args2
	 *            the String, man is looking for
	 */
	public static void main(final String[] args) {
		if (args.length < 3)
			printUsage();
		try {
			BufferedReader reader = IOUtils.getBufferedReader(args[0]);
			BufferedWriter writer = IOUtils.getBufferedWriter(args[1]);
			StringBuilder sb = new StringBuilder();
			for (int i = 2; i < args.length; i++)
				sb.append(" " + args[i]);
			String line = "";
			do {
				line = reader.readLine();
				if (line != null)
					if (line.contains(sb))
						writer.write(line + "\n");
			} while (line != null);
			reader.close();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done.");
	}

}
