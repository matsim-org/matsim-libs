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

package playground.johannes.gsv.matrices.postprocess;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;

/**
 * @author johannes
 * 
 */
public class AverageMatrices {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] dirs = new String[args.length - 1];
		for (int i = 0; i < args.length - 1; i++) {
			dirs[i] = args[i];
		}
		File fileDir = new File(dirs[0]);
		String[] fileNames = fileDir.list();
		String outDir = args[args.length - 1];

		for (String fileName : fileNames) {
			Set<KeyMatrix> matrices = new HashSet<>();
			for (String dir : dirs) {
				String file = String.format("%s/%s", dir, fileName);

				KeyMatrix m = loadMatrix(file);
				matrices.add(m);

			}

			KeyMatrix avr = MatrixOperations.average(matrices);

			// MatrixOperations.randomize(avr, 0.1);

			writeMatrix(avr, String.format("%s/%s", outDir, fileName));
		}
	}

	private static KeyMatrix loadMatrix(String file) {
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(file);
		return reader.getMatrix();
	}

	private static void writeMatrix(KeyMatrix m, String file) {
		KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
		writer.write(m, file);
	}

}
