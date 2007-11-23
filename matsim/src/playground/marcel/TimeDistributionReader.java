/* *********************************************************************** *
 * project: org.matsim.*
 * TimeDistributionReader.java
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

package playground.marcel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class TimeDistributionReader {
	
	public ArrayList<Double> readFile(String filename) {
		BufferedReader infile;
		ArrayList<Double> timeDistro = new ArrayList<Double>(24);
		try {
			if (new File(filename).exists()) {
				infile = new BufferedReader( new FileReader (filename));
			} else if (new File(filename + ".gz").exists()) {
				infile = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename  + ".gz"))));
			} else {
				infile = null;
			}
		
			if (infile == null) {
				throw new FileNotFoundException(filename);
			}
			String line = null;
			while ( (line = infile.readLine()) != null) {
				if (line.trim().length() > 0)
				timeDistro.add(Double.parseDouble(line));
			}
			infile.close();
			return timeDistro;
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

}
