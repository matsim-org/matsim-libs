/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.accessibility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.CSVWriter;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author dziemke
 */
public class CalculateCompositeAccessibility {

	private static final Logger LOG = Logger.getLogger(CalculateCompositeAccessibility.class);

	private static int globalLineCount = -1;
	private static Map<Tuple<String, Integer>, String[]> accessibilityMaps = new HashMap<>();
	private static String[] header = null;
	private static List<Integer> columnsToModify = new ArrayList<>();

	
	public static void main(String[] args) {
		String directoryRoot = "../../../shared-svn/projects/maxess/data/nmb/output/17compRestricted500/";
		String[] activityTypes = {FacilityTypes.SHOPPING, FacilityTypes.LEISURE, FacilityTypes.OTHER, FacilityTypes.EDUCATION};

		setHeader(directoryRoot, activityTypes);
		collectData(directoryRoot, activityTypes);
		writeFile(directoryRoot, activityTypes);
	}

	
	private static void setHeader(String directoryRoot, String[] activityTypes) {
		String inputFile = directoryRoot + activityTypes[0] + "/accessibilities.csv";
		FileReader fileReader = null;
		BufferedReader bufferedReader;
	
		try {
			fileReader = new FileReader(inputFile);
			bufferedReader = new BufferedReader(fileReader);
	
			String line = bufferedReader.readLine();
			header = line.split(",");
			LOG.info("Header has " + header.length + " fields.");
			
			if (!header[0].equals(Labels.X_COORDINATE)) {
				fileReader.close();
				throw new RuntimeException("Column is not the expected one!");
			}
			if (!header[1].equals(Labels.Y_COORDINATE)) {
				fileReader.close();
				throw new RuntimeException("Column is not the expected one!");
			}
			for (int i = 2; i < header.length; i++) {
				if (header[i].contains("access"))
					columnsToModify.add(i);
			}
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		try {
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private static void collectData(String directoryRoot, String[] activityTypes) {
		for (String type : activityTypes) {

			String inputFile = directoryRoot + type + "/accessibilities.csv";
			FileReader fileReader = null;
			BufferedReader bufferedReader;

			try {
				fileReader = new FileReader(inputFile);
				bufferedReader = new BufferedReader(fileReader);

				String line = null;

				// Header
				line = bufferedReader.readLine();
				String[] currentHeader = line.split(",");
				for (int i = 0; i < header.length; i++) {
					if (!header[i].equals(currentHeader[i])) {
						throw new RuntimeException("Headers do not match!");
					}
				}
				
				// Other lines
				int lineCount = 1;	
				while ((line = bufferedReader.readLine()) != null) {
					String[] entry = line.split(",");
					
					if (line != null && !line.equals("")) {
						accessibilityMaps.put(new Tuple<String, Integer>(type, lineCount), entry);
						lineCount++;
					}
				}
				if (globalLineCount == -1) {
					globalLineCount = lineCount;
					LOG.info("Overall line count set to " + globalLineCount + ".");
				} else {
					if (globalLineCount != lineCount) {
						fileReader.close();
						throw new RuntimeException("Files must be of equal lenghts!");
					}
				}
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			try {
				fileReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private static void writeFile(String directoryRoot, String[] activityTypes) {
		String directory = directoryRoot + "composite/";
		new File(directory).mkdir();
		CSVWriter writer = new CSVWriter(directory + "accessibilities.csv");
		LOG.info("Start writing output file.");

		for (int i = 0; i < header.length; i++) {
			writer.writeField(header[i]);
		}
		writer.writeNewLine();

		for (int i = 1 ; i < globalLineCount; i++) {
			String[] resultingLine = new String[header.length];
			String[] line = accessibilityMaps.get(new Tuple<String, Integer>(activityTypes[0], i));
			for (int j = 0; j < line.length ; j++) {
				if (!columnsToModify.contains(j)) {
					resultingLine[j] = line[j];
				} else {
					double value = 0.;
					for (String type : activityTypes) {
						String[] typeSpecificLine = accessibilityMaps.get(new Tuple<String, Integer>(type, i));
						value += Double.parseDouble(typeSpecificLine[j]);
					}
					Double result = value / activityTypes.length;
					resultingLine[j] = result.toString();
				}
			}
			for (int k = 0; k < line.length; k++) {
				writer.writeField(resultingLine[k]);
			}
			writer.writeNewLine();
		}
		writer.close();
		LOG.info("Finished writing output file.");
	}
}