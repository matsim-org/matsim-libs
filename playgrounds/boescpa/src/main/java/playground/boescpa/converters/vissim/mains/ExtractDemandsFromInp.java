/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim.mains;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Extracts demands from INP-files and writes it to txt-files.
 *
 * @author boescpa
 */
public class ExtractDemandsFromInp {

	public static void main(String[] args) {
		for (int i = 0; i < 24; i++) {
			String path2InpFile = args[0] + i + ".inp";
			String path2NewInpFile = args[1] + i + ".txt";
			final BufferedReader in = IOUtils.getBufferedReader(path2InpFile);
			final BufferedWriter out = IOUtils.getBufferedWriter(path2NewInpFile);
			final Counter c = new Counter("line ");

			try {
				String line = in.readLine();
				c.incCounter();
				while (line != null && !line.equals("-- Routing Decisions: --")) {
					line = in.readLine();
					c.incCounter();
				}
				while (line != null && !line.equals("-- Priority Rules: --")) {
					out.write(line);
					out.newLine();
					line = in.readLine();
					c.incCounter();
				}
				c.printCounter();
				in.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}



}
