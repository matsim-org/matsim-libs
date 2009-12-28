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
public class LlhExtractor {
	private static String[] extract(String line, String[] strs) {
		line = line.split(strs[0])[1];
		String[] toReturn = new String[2];
		// System.out.println("strs[1] =\t" + strs[1]);
		String[] splits = line.split(strs[1]);// " "
		// System.out.println("splits =\t(1)\t" + line.split(strs[1])[0]);
		// System.out.println("\t(2)\t" + line.split(strs[1])[1]);
		toReturn[0] = splits[0];
		toReturn[1] = splits[2].split(strs[2])[0];// ";"
		return toReturn;
	}

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
	 *            outputTXTFilenameA
	 * @param args2
	 *            outputTXTFilenameB
	 */
	public static void main(final String[] args) {
		if (args.length < 3)
			printUsage();
		try {
			BufferedReader reader = IOUtils.getBufferedReader(args[0]);
			BufferedWriter writerA = IOUtils.getBufferedWriter(args[1]);
			BufferedWriter writerB = IOUtils.getBufferedWriter(args[2]);
			String line = "";
			String[] strs = "log-likelihood is \t \t;".split("\t");
			// for (int i = 0; i < 3; i++)
			// System.out.println("strs[" + i + "] =\t" + strs[i]);
			String[] strs2write;
			do {
				line = reader.readLine();
				if (line != null) {
					strs2write = extract(line, strs);
					writerA.write(strs2write[0] + "\n");
					writerB.write(strs2write[1] + "\n");
				}
			} while (line != null);
			reader.close();
			writerA.close();
			writerB.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done.");
	}

}
