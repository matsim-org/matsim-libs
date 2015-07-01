/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.johannes.gsv.matrices.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import playground.johannes.gsv.zones.KeyMatrix;

/**
 * @author johannes
 *
 */
public class VisumOMatrixReader {

	public static KeyMatrix read(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		
//		int startLine = 8;
		int startLine = 29;
		for(int i = 0; i < startLine; i++) reader.readLine();
		
		KeyMatrix m = new KeyMatrix();
		
		String line;
		while((line = reader.readLine()) != null) {
			if(line.startsWith("*")) {
				break;
			}
			else {
				line = line.trim();
				String[] tokens = line.split("\\s+");
				String i = tokens[0];
				String j = tokens[1];
				Double val = new Double(tokens[2]);
				
				m.set(i, j, val);
			}
		}
		
		
		reader.close();
		
		return m;
	}
	
	public static void main(String[] args) throws IOException {
		VisumOMatrixReader.read("/home/johannes/gsv/prognose-update/iv-2030.txt");
	}
}
