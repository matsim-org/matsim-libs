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

import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class P2PkwMatrix {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String baseDir = "/home/johannes/sge/prj/synpop/run/791/output/scaled";
		
		NumericMatrix buisiness = loadMatrix(String.format("%s/miv.buisiness.xml", baseDir));
		MatrixOperations.applyFactor(buisiness, 1/1.2);

		NumericMatrix edu = loadMatrix(String.format("%s/miv.edu.xml", baseDir));
		MatrixOperations.applyFactor(edu, 1/1.7);
		
		NumericMatrix leisure = loadMatrix(String.format("%s/miv.leisure.xml", baseDir));
		MatrixOperations.applyFactor(leisure, 1/1.9);
		
		NumericMatrix shop = loadMatrix(String.format("%s/miv.shop.xml", baseDir));
		MatrixOperations.applyFactor(shop, 1/1.5);
		
		NumericMatrix vacationsLong = loadMatrix(String.format("%s/miv.vacations_long.xml", baseDir));
		MatrixOperations.applyFactor(vacationsLong, 1/1.9);
		
		NumericMatrix vacationsShort = loadMatrix(String.format("%s/miv.vacations_short.xml", baseDir));
		MatrixOperations.applyFactor(vacationsShort, 1/1.9);
		
		NumericMatrix work = loadMatrix(String.format("%s/miv.work.xml", baseDir));
		MatrixOperations.applyFactor(work, 1/1.2);
		
		Set<NumericMatrix> matrices = new HashSet<>();
		matrices.add(buisiness);
		matrices.add(edu);
		matrices.add(leisure);
		matrices.add(shop);
		matrices.add(vacationsShort);
		matrices.add(vacationsLong);
		matrices.add(work);
		
		NumericMatrix sum = new NumericMatrix();
		MatrixOperations.accumulate(matrices, sum);
		writeMatrix(sum, String.format("%s/pkw.xml", baseDir));
	}

	private static NumericMatrix loadMatrix(String file) {
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(file);
		return reader.getMatrix();
	}
	
	private static void writeMatrix(NumericMatrix m, String file) {
		NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		writer.write(m, file);
	}
}
