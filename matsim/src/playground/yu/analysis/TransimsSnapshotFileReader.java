/* *********************************************************************** *
 * project: org.matsim.*
 * transimsSnapshotFileReader.java
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

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

public class TransimsSnapshotFileReader {

	private BufferedReader infile = null;
	public TransimsSnapshotFileReader(final String filename){
		try {
			this.infile = IOUtils.getBufferedReader(filename);
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}		
	}
	
	public String [] readLine() {
		String [] tokline = null;
		try {
			String line = this.infile.readLine();
			if (line == null){
				this.infile.close();
			} else {
				tokline = StringUtils.explode(line, '\t', 16);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return tokline;
	}
}


