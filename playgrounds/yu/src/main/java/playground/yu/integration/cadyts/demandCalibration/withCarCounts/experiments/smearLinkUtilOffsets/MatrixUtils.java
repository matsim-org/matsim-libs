/* *********************************************************************** *
 * project: org.matsim.*
 * MatrixUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.smearLinkUtilOffsets;

import java.util.Iterator;
import java.util.logging.Logger;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import playground.yu.utils.io.SimpleWriter;

public class MatrixUtils {
	private static String MATRIX_UTILS = MatrixUtils.class.getName();

	public static void writeMatrix(Matrix matrix, String outputFilename) {
		Logger.getLogger(MATRIX_UTILS).info(
				"write Matrix into\t" + outputFilename + "\tBEGAN!");
		SimpleWriter writer = new SimpleWriter(outputFilename);
		writer.writeln("rowIdx\tcolIdx\tvalue");
		for (Iterator<MatrixEntry> it = matrix.iterator(); it.hasNext();) {
			writer.writeln(MatrixEntryToString(it.next()));
			writer.flush();
		}
		writer.close();
		Logger.getLogger(MATRIX_UTILS).info(
				"write Matrix into\t" + outputFilename + "\tENDED!");
	}

	public static String MatrixEntryToString(MatrixEntry entry) {
		StringBuffer sb = new StringBuffer(Integer.toString(entry.row()));
		sb.append('\t');
		sb.append(entry.column());
		sb.append('\t');
		sb.append(entry.get());
		return sb.toString();
	}
}
