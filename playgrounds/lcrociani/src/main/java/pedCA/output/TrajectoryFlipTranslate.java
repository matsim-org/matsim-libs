/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryFlipTranslate.java
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
 * *********************************************************************** */

package pedCA.output;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.utils.misc.StringUtils;

public class TrajectoryFlipTranslate {
	
	private final String inFile;
	private final String outFile;
	private final double trX;
	private final double trY;

	public TrajectoryFlipTranslate(String inFile, String outFile, double translateX, double translateY){
		this.inFile = inFile;
		this.outFile = outFile;
		this.trX = translateX;
		this.trY = translateY;
	}
	
	public void run(){
		try {
			BufferedReader inReader = new BufferedReader(new FileReader(new File(this.inFile)));
			BufferedWriter outWriter = new BufferedWriter(new FileWriter(new File(this.outFile)));
			
			String l = inReader.readLine();
			while (l != null) {
				
				
				String[] expl = StringUtils.explode(l, AgentTracker.SEPERATOR);
				if (expl.length != 5 || l.startsWith("#")) {
					outWriter.append(l);
					outWriter.append('\n');
				} else {
					String ft = flipTranslate(expl);
					outWriter.append(ft);
					outWriter.append('\n');
				}
				l = inReader.readLine();
			}
			inReader.close();
			outWriter.close();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	private String flipTranslate(String[] expl) {
		double x = Double.parseDouble(expl[2]);
		double y = Double.parseDouble(expl[3]);
		x += this.trX;
		y *= -1;
		y += this.trY;
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < 2; i++) {
			buf.append(expl[i]);
			buf.append(AgentTracker.SEPERATOR);
		}
		buf.append(x);
		buf.append(AgentTracker.SEPERATOR);
		buf.append(y);
		buf.append(AgentTracker.SEPERATOR);
		buf.append(expl[4]);
		return buf.toString();
	}

}
