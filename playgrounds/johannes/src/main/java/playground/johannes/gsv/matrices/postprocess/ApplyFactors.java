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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;

/**
 * @author johannes
 *
 */
public class ApplyFactors {

	private static final double scaleFactor = 11.8;

	private static final double diagonalFactor = 1.3;

	private static final Logger logger = Logger.getLogger(ApplyFactors.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String baseDir = "/home/johannes/sge/prj/synpop/run/791/output";//args[0];
//		String outDir = "/home/johannes/sge/prj/synpop/run/791/output/scaled";//args[1];
		String baseDir = args[0];
		String outDir = args[1];
		
		/*
		 * avr day all purposes
		 */
		String file = String.format("%s/miv.xml", baseDir);
		KeyMatrix m = loadMatrix(file);

		MatrixOperations.applyFactor(m, scaleFactor);
		MatrixOperations.applyDiagonalFactor(m, diagonalFactor);

		writeMatrix(m, String.format("%s/miv.xml", outDir));

		double sum = MatrixOperations.sum(m);

		MatrixOperations.symetrize(m);
		writeMatrix(m, String.format("%s/miv.sym.xml", outDir));
		/*
		 * purpose matrices
		 */
		String[] types = new String[] { "work", "buisiness", "shop", "edu", "leisure", "vacations_short", "vacations_long", "wecommuter"};
		for(String type : types) {
			logger.info(String.format("Processing matrix %s...", type));

			file = String.format("%s/miv.%s.xml", baseDir, type);
			m = loadMatrix(file);

			MatrixOperations.applyFactor(m, scaleFactor);
			MatrixOperations.applyDiagonalFactor(m, diagonalFactor);

			double sum2 = MatrixOperations.sum(m);
			logger.info(String.format("Trip share: %s", sum2/sum));

			file = String.format("%s/miv.%s.xml", outDir, type);
			writeMatrix(m, file);

			MatrixOperations.symetrize(m);
			writeMatrix(m, String.format("%s/miv.%s.sym.xml", outDir, type));
		}
//		System.exit(-1);
		/*
		 * days
		 */
		Map<String, Double> factors = new HashMap<>();
		factors.put(CommonKeys.MONDAY, 1.02);
		factors.put(CommonKeys.FRIDAY, 1.15);
		factors.put(CommonKeys.SATURDAY, 0.95);
		factors.put(CommonKeys.SUNDAY, 0.67);
		factors.put("dimido", 1.07);
		factors.put("wkday", 1.076);

		double wkdayf = 0;
		String[] days = new String[] {CommonKeys.MONDAY, CommonKeys.FRIDAY, CommonKeys.SATURDAY, CommonKeys.SUNDAY, "dimido", "wkday"};
		for(String day : days) {
			logger.info(String.format("Processing matrix %s...", day));

			file = String.format("%s/miv.%s.xml", baseDir, day);
			m = loadMatrix(file);

			MatrixOperations.applyFactor(m, scaleFactor);
			MatrixOperations.applyDiagonalFactor(m, diagonalFactor);

			double sum2 = MatrixOperations.sum(m);
			logger.info(String.format("Trip share: %s", sum2/sum));

			double f = sum/sum2 * factors.get(day);
			MatrixOperations.applyFactor(m, f);
			logger.info(String.format("Scale factor: %s", f));

			logger.info(String.format("Trip share: %s", MatrixOperations.sum(m)/sum));

			file = String.format("%s/miv.%s.xml", outDir, day);
			writeMatrix(m, file);

			MatrixOperations.symetrize(m);
			writeMatrix(m, String.format("%s/miv.%s.sym.xml", outDir, day));

			if(day.equalsIgnoreCase("wkday")) {
				wkdayf = f;
			}
		}

//		double wkdayf = sum/wkdaySum * 1.076;
//		String[] types = new String[] { "work", "buisiness", "shop", "edu", "leisure", "vacations_short", "vacations_long", "wecommuter"};
		for(String type : types) {
			/*
			 * purpose per wkday
			 */
			logger.info(String.format("Processing matrix wkday.%s...", type));

			file = String.format("%s/miv.wkday.%s.xml", baseDir, type);
			m = loadMatrix(file);

			MatrixOperations.applyFactor(m, scaleFactor);
			MatrixOperations.applyDiagonalFactor(m, diagonalFactor);

//			sum2 = MatrixOperations.sum(m);
//			logger.info(String.format("Trip share: wkday.%s", sum2/sum));

			MatrixOperations.applyFactor(m, wkdayf);
			logger.info(String.format("Scale factor: wkday", wkdayf));

//			logger.info(String.format("Trip share: wkday.%s", MatrixOperations.sum(m)/sum));
			file = String.format("%s/miv.wkday.%s.xml", outDir, type);
			writeMatrix(m, file);

			MatrixOperations.symetrize(m);
			writeMatrix(m, String.format("%s/miv.wkday.%s.sym.xml", outDir, type));
		}

		/*
		 * season
		 */
		factors = new HashMap<>();
		factors.put("summer", 0.998);
		factors.put("winter", 1.002);
		String[] seasons = new String[] {"summer", "winter"};
		for(String season : seasons) {
			logger.info(String.format("Processing matrix %s...", season));
			file = String.format("%s/miv.%s.xml", baseDir, season);
			m = loadMatrix(file);

			MatrixOperations.applyFactor(m, scaleFactor);
			MatrixOperations.applyDiagonalFactor(m, diagonalFactor);

			double sum2 = MatrixOperations.sum(m);
			logger.info(String.format("Trip share: %s", sum2/sum));

			double f = sum/sum2 * factors.get(season);
			MatrixOperations.applyFactor(m, f);
			logger.info(String.format("Scale factor: %s", f));

			logger.info(String.format("Trip share: %s", MatrixOperations.sum(m)/sum));

			file = String.format("%s/miv.%s.xml", outDir, season);
			writeMatrix(m, file);

			MatrixOperations.symetrize(m);
			writeMatrix(m, String.format("%s/miv.%s.sym.xml", outDir, season));
		}
		/*
		 * from/to home
		 */
		logger.info("Processing matrix fromHome...");
		m = loadMatrix(String.format("%s/miv.fromHome.xml", baseDir));
		MatrixOperations.applyFactor(m, scaleFactor);
		MatrixOperations.applyDiagonalFactor(m, diagonalFactor);
		logger.info(String.format("Trip share: %s", MatrixOperations.sum(m)/sum));
		writeMatrix(m, String.format("%s/miv.fromHome.xml", outDir));

		logger.info("Processing matrix toHome...");
		m = loadMatrix(String.format("%s/miv.toHome.xml", baseDir));
		MatrixOperations.applyFactor(m, scaleFactor);
		MatrixOperations.applyDiagonalFactor(m, diagonalFactor);
		logger.info(String.format("Trip share: %s", MatrixOperations.sum(m)/sum));
		writeMatrix(m, String.format("%s/miv.toHome.xml", outDir));

		logger.info("Done.");
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
