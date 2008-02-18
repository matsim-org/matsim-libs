/* *********************************************************************** *
 * project: org.matsim.*
 * TvehHomeTest.java
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

package playground.yu.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.utils.io.IOUtils;

public class TvehHomeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String origTvehFilename = "../runs/run263/T.veh.gz";
		final String homeTvehFilename = "../runs/run263/homeT.veh.gz";
		BufferedReader br;
		try {
			br = IOUtils.getBufferedReader(origTvehFilename);
			br.
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
