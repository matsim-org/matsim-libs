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

package playground.johannes.gsv.fpd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;

/**
 * @author johannes
 *
 */
public class Iais2matrix {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/fpd/fraunhofer/study/data/raw/24-04-2015/AggregatedTrajectories_all_noTransit_min3D_Flows_daily.csv"));
		String line = reader.readLine();
		
		KeyMatrix m = new KeyMatrix();
		
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(",");
			
			String id_i = tokens[2];
			String id_j = tokens[3];
			double volume = Double.parseDouble(tokens[29]);
			
			m.set(id_i, id_j, volume);
		}
		reader.close();
		
		KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
		writer.write(m, "/home/johannes/gsv/fpd/fraunhofer/study/data/matrix/24-04-2015/iais.3d.xml");
	}

}
