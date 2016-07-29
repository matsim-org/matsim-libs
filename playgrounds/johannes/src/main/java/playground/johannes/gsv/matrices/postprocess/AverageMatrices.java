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

import org.apache.log4j.Logger;
import playground.johannes.synpop.matrix.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class AverageMatrices {

	private static final Logger logger = Logger.getLogger(AverageMatrices.class);

	public static void main(String[] args) throws IOException {
		String[] dirs = new String[args.length - 1];
		for (int i = 0; i < args.length - 1; i++) {
			dirs[i] = args[i];
		}
		File fileDir = new File(dirs[0]);
		String[] fileNames = fileDir.list();
		String outDir = args[args.length - 1];

		for (String fileName : fileNames) {
			if(fileName.startsWith("car")) {
				Set<NumericMatrix> matrices = new HashSet<>();
				for (String dir : dirs) {
					String file = String.format("%s/%s", dir, fileName);
					logger.info(String.format("Loading matrix %s...", file));
					if(new File(file).exists()) {
						NumericMatrix m = loadMatrix(file);
						matrices.add(m);
					} else {
						logger.info("File not found.");
					}
				}

				logger.info(String.format("Averaging matrix %s...", fileName));
				NumericMatrix avr = new NumericMatrix();
				MatrixOperations.average(matrices, avr);
				logger.info(String.format("Writing matrix %s...", fileName));
				writeMatrix(avr, String.format("%s/%s", outDir, fileName));
			}
		}
	}

	private static NumericMatrix loadMatrix(String file) throws IOException {
		if(file.endsWith(".txt") || file.endsWith("txt.gz")) {
			NumericMatrix m = new NumericMatrix();
			NumericMatrixTxtIO.read(m, file);
			return m;
		} else if(file.endsWith("xml") || file.endsWith("xml.gz")) {
			NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
			reader.setValidating(false);
			reader.readFile(file);
			return reader.getMatrix();
		} else {
			return null;
		}
	}

	private static void writeMatrix(NumericMatrix m, String file) throws IOException {
		if(file.endsWith(".xml") || file.endsWith(".xml.gz")) {
			NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
			writer.write(m, file);
		} else if(file.endsWith(".txt") || file.endsWith(".txt.gz")) {
			NumericMatrixTxtIO.write(m, file);
		} else {
			throw new RuntimeException("Unknown file format.");
		}
	}

}
