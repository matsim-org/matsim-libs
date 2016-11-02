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
package playground.dziemke.accessibility.input;

import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author dziemke
 */
public class CreateFacilitiiesFileFromKODI {
	private final static Logger LOG = Logger.getLogger(CreateFacilitiiesFileFromKODI.class);

	public enum Option {

		HOSPITALS_FROM_FILE (Group.FROM_FILE),
		SCHOOLS_FROM_FILE (Group.FROM_FILE),

		SCHOOLS_FROM_URL (Group.FROM_URL);

		private Group group;

		Option(Group group) {
			this.group = group;
		}

		public boolean isInGroup(Group group) {
			return this.group == group;
		}

		public enum Group {
			FROM_FILE,
			FROM_URL
		}
	}

	// pattern: Option -> { input, output, facilitiesFileDescription, activityType}
	private static final Map<Option, String[]> OPTION_MAP = ImmutableMap.<Option, String[]>builder()
		.put(Option.HOSPITALS_FROM_FILE, new String[] {
			"../../../shared-svn/projects/maxess/data/nairobi/kodi/health/hospitals/kenya_hospitals_detail.csv",
			"../../../shared-svn/projects/maxess/data/nairobi/kodi/health/hospitals/facilities.xml",
			"Hospitals in Kenya",
			"Hospital"
		})
		.put(Option.SCHOOLS_FROM_URL, new String[] {
			"https://www.opendata.go.ke/api/views/p452-xb7c/rows.csv",
			"../../../shared-svn/projects/maxess/data/nairobi/kodi/schools/primary_from_url/facilities.xml",
			"Primary Schools in Kenya",
			"School"
		}) // Public Schools
		.put(Option.SCHOOLS_FROM_FILE, new String[] {
			"../../../shared-svn/projects/maxess/data/nairobi/kodi/schools/primary_public/Public_Primary_School_listed_by_2007.csv",
			"../../../shared-svn/projects/maxess/data/nairobi/kodi/schools/primary_from_file/facilities.xml",
			"Primary Schools in Kenya",
			"School"
		}) // not sure about output
		.build();

	public static void main(String[] args) {
		Option option = Option.HOSPITALS_FROM_FILE;

		String inputCRS = "EPSG:4326";
		String outputCRS = "EPSG:21037";
		String headOfCoordColumn = "Geolocation";
		String separator = ",";

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);

		if (option.isInGroup(Option.Group.FROM_URL)) {
			// Option 1: Get input data from URL
			try {
				BufferedReader reader = getBufferedReaderFromCsvUrl(OPTION_MAP.get(option)[0]);
				ActivityFacilities activityFacilities =
						createActivityFaciltiesFromBufferedReader(reader, OPTION_MAP.get(option)[2], headOfCoordColumn, ct,
								separator, OPTION_MAP.get(option)[3]);
				writeFacilitiesFile(activityFacilities, OPTION_MAP.get(option)[1]);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// Option 2: Get input data from locally stored CSV file
			ActivityFacilities activityFacilities = createActivityFaciltiesFromFile(OPTION_MAP.get(option)[0],
					OPTION_MAP.get(option)[2], headOfCoordColumn, ct, separator, OPTION_MAP.get(option)[3]);
			//writeFacilitiesFile(activityFacilities, OPTION_MAP.get(option)[1]);
		}
	}

	private static BufferedReader getBufferedReaderFromCsvUrl(String url) throws IOException {
		InputStream inputStream = getInputStreamFromCsvUrl(url);
		return new BufferedReader(new InputStreamReader(inputStream));
	}

	private static InputStream getInputStreamFromCsvUrl(String url) throws IOException {
		return new URL(url).openStream();
	}

	private static ActivityFacilities createActivityFaciltiesFromBufferedReader(BufferedReader bufferedReader, String facilitiesFileDescription,
			String headOfCoordColumn, CoordinateTransformation ct, String separator, String activityType) {
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities(facilitiesFileDescription);
		int lineCount = 0;
		try {
			// Header
			String currentLine = bufferedReader.readLine();
			Map<String,Integer> columnHeads = createColumnHeadsMap(currentLine, separator);
			lineCount++;

			// Lines with data
			while ((currentLine = bufferedReader.readLine()) != null) {
				// This quite complicated expression does the following:
				// Split on the comma only if that comma has zero, or an even number of quotes ahead of it.
				// See: http://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
				String[] lineEntries = currentLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				lineCount++;

				ActivityFacility activityFacility = createFacility(lineCount, lineEntries, columnHeads, headOfCoordColumn, ct, separator, activityType);
				activityFacilities.addActivityFacility(activityFacility);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		LOG.info("All activity facilities created.");
		return activityFacilities;
	}

	private static ActivityFacilities createActivityFaciltiesFromFile(String csvFileName,
																	 String facilitiesFileDescription,
																	 String headOfCoordColumn,
																	 CoordinateTransformation ct,
																	 String separator,
																	 String activityType) {
		BufferedReader bufferedReader = IOUtils.getBufferedReader(csvFileName);
		return createActivityFaciltiesFromBufferedReader(bufferedReader, facilitiesFileDescription, headOfCoordColumn, ct, separator, activityType);
	}

	private static Map<String,Integer> createColumnHeadsMap(String headLine, String separator) {
		String[] header = headLine.split(separator);
		Map<String,Integer> columnHeads = new LinkedHashMap<>(header.length);
		for (int i=0; i<header.length; i++) {
			columnHeads.put(header[i],i);
		}
		LOG.info("Map that relates all column head to their position created.");
		return columnHeads;
	}


	private static ActivityFacility createFacility(int lineCount, String[] lineEntries, Map<String,Integer> columnHeads,
			String headOfCoordColumn, CoordinateTransformation ct, String separator, String activityType) {
		Id<ActivityFacility> id = Id.create(lineCount , ActivityFacility.class);

		String location = lineEntries[columnHeads.get(headOfCoordColumn)].replaceAll("[()\"]","");
		String[] coordinates = location.split(separator, -1);
		Coord coord = CoordUtils.createCoord(Double.parseDouble(coordinates[1]), Double.parseDouble(coordinates[0]));
		Coord transformedCoord = ct.transform(coord);

		ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
		ActivityFacility activityFacility = activityFacilitiesFactory.createActivityFacility(id, transformedCoord);
		ActivityOption activityOption = activityFacilitiesFactory.createActivityOption(activityType);
		activityFacility.addActivityOption(activityOption);
		return activityFacility;
	}


	private static void writeFacilitiesFile(ActivityFacilities activityFacilities, String facilitiesOutputFile) {
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(activityFacilities);
		facilitiesWriter.write(facilitiesOutputFile);
		LOG.info("Facility file written.");
	}
}