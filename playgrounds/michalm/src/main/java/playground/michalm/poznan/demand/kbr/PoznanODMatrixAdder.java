/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.poznan.demand.kbr;

import java.io.*;
import java.lang.reflect.Array;
import java.util.Map;

import playground.michalm.poznan.demand.kbr.PoznanLanduseDemandGeneration.ActivityPair;
import playground.michalm.util.visum.VisumMatrixReader;

public class PoznanODMatrixAdder {
	private static String dir = "D:\\eTaxi\\Poznan_MATSim\\";
	private static String odMatrixFilePrefix = dir + "odMatricesByType\\";
	private static String put2PrtRatiosFile = dir + "PuT_PrT_ratios";

	private static Map<ActivityPair, Double> prtCoeffs;
	private static double[][][] totalODMatrices;

	public static void main(String[] args) {
		prtCoeffs = PoznanLanduseDemandGeneration.readPrtCoeffs(put2PrtRatiosFile);

		totalODMatrices = new double[24][][];

		for (ActivityPair ap : ActivityPair.values()) {
			readMatrix(odMatrixFilePrefix, ap);
		}

		writeMatricesByHour(odMatrixFilePrefix);
	}

	private static void readMatrix(String filePrefix, ActivityPair actPair) {
		double flowCoeff = prtCoeffs.get(actPair);

		for (int i = 0; i < 24; i++) {
			String odMatrixFile = filePrefix + actPair.name() + "_" + i + "-" + (i + 1);

			System.out.println("readMatrix: " + odMatrixFile);

			double[][] odMatrix = VisumMatrixReader.readMatrix(odMatrixFile);

			if (totalODMatrices[i] == null) {
				totalODMatrices[i] = (double[][])Array.newInstance(//
						double.class, odMatrix.length, odMatrix[0].length);
			}

			for (int j = 0; j < odMatrix.length; j++) {
				for (int k = 0; k < odMatrix[j].length; k++) {
					totalODMatrices[i][j][k] += flowCoeff * odMatrix[j][k];
				}
			}
		}
	}

	private static void writeMatricesByHour(String filePrefix) {
		for (int i = 0; i < 24; i++) {
			String odMatrixFile = filePrefix + "_" + i + "-" + (i + 1);

			System.out.println("writeMatrix: " + odMatrixFile);

			writeMatrix(totalODMatrices[i], odMatrixFile);
		}
	}

	private static void writeMatrix(double[][] array, String file) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			for (int i = 0; i < array.length; i++) {
				for (int j = 0; j < array[i].length; j++) {
					writer.write(array[i][j] + "\t");
				}

				writer.newLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
