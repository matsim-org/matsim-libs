/* *********************************************************************** *
 * project: org.matsim.*
 * TransimsSnapshotFileWriter.java
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

package playground.gregor.demandmodeling;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class CSVFileWriter {
	private BufferedWriter out = null;	
	public CSVFileWriter(final String filename){
		try {
			this.out = IOUtils.getBufferedWriter(filename, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writeLine(final String [] line){
		StringBuffer buff = new StringBuffer();
		buff.append(line[0]);
		for (int i = 1; i < line.length; i++){
			buff.append(",");
			buff.append(line[i]);
		}
		buff.append("\n");
		try {
			this.out.write(buff.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void finish() {
		if (this.out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
