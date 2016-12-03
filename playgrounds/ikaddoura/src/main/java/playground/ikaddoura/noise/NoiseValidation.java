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

package playground.ikaddoura.noise;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

/**
* @author ikaddoura
*/

public class NoiseValidation {
		
	private static final Logger log = Logger.getLogger(NoiseValidation.class);

	// input
	private final String validationFile = "../../../shared-svn/studies/countries/de/berlin_noise/Fassadenpegel/FP_gesamt_Atom_repaired.txt";	
	
//	private final String processedImmissionFile = "../../../runs-svn/berlin_internalizationCar/output/baseCase_2/noiseAnalysis_2016-11-09_rpGap100/analysis_it.100/immissions/immission_processed.csv";
//	private final String processedImmissionFile = "../../../runs-svn/berlin_internalizationCar/output/berlin-noise-analysis_2016-11-14/noiseAnalysis_gap50-2/analysis_it.100/immissions/immission_processed.csv";
//	private final String processedImmissionFile = "../../../runs-svn/berlin_internalizationCar/output/baseCase_2/noiseAnalysis_2016-11-14_rpGap10_tiergarten/analysis_it.100/immissions/immission_processed.csv";
//	private final String processedImmissionFile = "../../../runs-svn/berlin_internalizationCar/output/baseCase_2/noiseAnalysis_2016-11-14_rpGap5_schoeneberg/analysis_it.100/immissions/immission_processed.csv";
	private final String processedImmissionFile = "../../../runs-svn/berlin_internalizationCar/output/baseCase_2/noiseAnalysis_2016-11-14_rpGap5_wilmersdorf/analysis_it.100/immissions/immission_processed.csv";

	// output
//	private final String outputDirectory = "../../../runs-svn/berlin_internalizationCar/output/baseCase_2/noiseAnalysis_2016-11-09_rpGap100/analysis_it.100/immissions/";
//	private final String outputDirectory = "../../../runs-svn/berlin_internalizationCar/output/berlin-noise-analysis_2016-11-14/noiseAnalysis_gap50-2/analysis_it.100/immissions/";
//	private final String outputDirectory = "../../../runs-svn/berlin_internalizationCar/output/baseCase_2/noiseAnalysis_2016-11-14_rpGap5_schoeneberg/analysis_it.100/immissions/";
	private final String outputDirectory = "../../../runs-svn/berlin_internalizationCar/output/baseCase_2/noiseAnalysis_2016-11-14_rpGap5_wilmersdorf/analysis_it.100/immissions/";

	public static void main(String[] args) throws IOException {
		NoiseValidation noiseValidation = new NoiseValidation();
		noiseValidation.run();
	}

	private void run() throws IOException {
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// read file
		final Map<String, Tuple<Coord, Double>> validationPoints = readCSVFile(validationFile, ",", -1, 0, 1, 4);
		final Map<String, Tuple<Coord, Double>> simulationPoints = readCSVFile(processedImmissionFile, ";", 0, 1, 2, 27);
		
		
		String soldnerBerlinWKT = "PROJCS[\"DHDN / Soldner Berlin\","
				   + "GEOGCS[\"DHDN\",DATUM[\"Deutsches_Hauptdreiecksnetz\","
				   + "SPHEROID[\"Bessel 1841\",6377397.155,299.1528128,AUTHORITY[\"EPSG\",\"7004\"]],"
				   + "AUTHORITY[\"EPSG\",\"6314\"]],"
				   + "PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],"
				   + "UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],"
				   + "AUTHORITY[\"EPSG\",\"4314\"]],"
				   + "UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],"
				   + "PROJECTION[\"Cassini_Soldner\"],PARAMETER[\"latitude_of_origin\",52.41864827777778],"
				   + "PARAMETER[\"central_meridian\",13.62720366666667],PARAMETER[\"false_easting\",40000],"
				   + "PARAMETER[\"false_northing\",10000],"
				   + "AUTHORITY[\"EPSG\",\"3068\"],AXIS[\"y\",EAST],AXIS[\"x\",NORTH]]";
		
		log.info("Transforming coordinates...");
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(soldnerBerlinWKT, TransformationFactory.DHDN_GK4);
		
		for (String valPointId : validationPoints.keySet()) {
			Coord oldCoord = validationPoints.get(valPointId).getFirst();
			Coord transformedCoord = ct.transform(oldCoord);
			
			validationPoints.put(valPointId, new Tuple<Coord, Double>(transformedCoord, validationPoints.get(valPointId).getSecond()));
		}
		log.info("Transforming coordinates... Done.");	
		
		log.info("Mapping receiver points to nearest validation point...");	
		
		QuadTree<String> qt = new QuadTree<>(4555654, 5792443, 4638397, 5845987);		
		for (String valPointId : validationPoints.keySet()) {
			qt.put(validationPoints.get(valPointId).getFirst().getX(), validationPoints.get(valPointId).getFirst().getY(), valPointId);
		}

		Map<String, String> simPointId2valPointId = new HashMap<>();
		for (String simPointId : simulationPoints.keySet()) {
			String closestValidationPointId = qt.getClosest(simulationPoints.get(simPointId).getFirst().getX(), simulationPoints.get(simPointId).getFirst().getY());
			simPointId2valPointId.put(simPointId, closestValidationPointId);
		}
		
		log.info("Mapping receiver points to nearest validation point... Done.");
		
		log.info("Writing output...");
			
		String fileName = this.outputDirectory + "validation.csv";
		
		File file = new File(outputDirectory);
		file.mkdirs();
		
		try ( BufferedWriter bw = IOUtils.getBufferedWriter(fileName) ) {
			bw.write("Receiver Point Id (Sim) ; x (Sim) ; y (Sim) ; L_den (Sim) ; Closest Validation Point Id (Val) ; x (Val) ; y (Val) ; L_den (Val) ; Error (Sim-Val) ; Distance (Sim-Val)");
			bw.newLine();
			
			for (String simPointId : simulationPoints.keySet()) {
				String valPointId = simPointId2valPointId.get(simPointId);
				
				Point p1 = new Point((int) simulationPoints.get(simPointId).getFirst().getX(), (int) simulationPoints.get(simPointId).getFirst().getY());
				Point p2 = new Point((int) validationPoints.get(valPointId).getFirst().getX(), (int) validationPoints.get(valPointId).getFirst().getY());
				double distance = p1.distance(p2);

				bw.write(simPointId + " ; " + simulationPoints.get(simPointId).getFirst().getX() + " ; " + simulationPoints.get(simPointId).getFirst().getY() + " ; " + simulationPoints.get(simPointId).getSecond()
						+ " ; " + valPointId + " ; " + validationPoints.get(valPointId).getFirst().getX() + " ; " + validationPoints.get(valPointId).getFirst().getY() + " ; " + validationPoints.get(valPointId).getSecond()
						+ " ; " + (simulationPoints.get(simPointId).getSecond() - validationPoints.get(valPointId).getSecond()) + " ; " + distance);
				bw.newLine();
			}
				
			bw.close();
			
			log.info("Writing output... Done. Output written to " + outputDirectory);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<String, Tuple<Coord, Double>> readCSVFile(String file, String separator, int idColumn, int xCoordColumn, int yCoordColumn, int valueColumn) throws IOException {
		
		Map<String, Tuple<Coord, Double>> id2pointInfo = new HashMap<>();
		
		BufferedReader br = IOUtils.getBufferedReader(file);
		String line = null;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String[] headers = line.split(separator);
		if (idColumn >= 0) log.info("id: " + headers[idColumn]);
		log.info("xCoord: " + headers[xCoordColumn]);
		log.info("yCoord: " + headers[yCoordColumn]);
		log.info("value: " + headers[valueColumn]);
		
		int lineCounter = 0;

		while( (line = br.readLine()) != null) {

			if (lineCounter % 1000000 == 0.) {
				log.info("# " + lineCounter);
			}

			String[] columns = line.split(separator);
			if (line.isEmpty() || line.equals("") || columns.length != headers.length) {
				log.warn("Skipping line " + lineCounter + ". Line is empty or the columns are inconsistent with the headers: [" + line.toString() + "]");
			
			} else {
				String id = null;
				double x = 0;
				double y = 0;
				double value = 0.;

				for (int column = 0; column < columns.length; column++){					
					if (column == idColumn) {
						id = columns[column];
					} else if (column == xCoordColumn) {
						x = Double.valueOf(columns[column]);
					} else if (column == yCoordColumn) {
						y = Double.valueOf(columns[column]);
					} else if (column == valueColumn) {
						value = Double.valueOf(columns[column]);
					}
				}
				if (idColumn >= 0) {
					id2pointInfo.put(id, new Tuple<Coord, Double>(new Coord(x,y), value));
				} else {
					id2pointInfo.put(String.valueOf(lineCounter), new Tuple<Coord, Double>(new Coord(x,y), value));
				}
				
				lineCounter++;
			}			
		}
		
		log.info("Done. Number of read lines: " + lineCounter);
		
		return id2pointInfo;
	}

}

