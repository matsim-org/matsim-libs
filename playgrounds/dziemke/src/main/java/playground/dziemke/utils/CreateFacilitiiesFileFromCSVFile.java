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
package playground.dziemke.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;


/**
 * @author dziemke
 */
public class CreateFacilitiiesFileFromCSVFile {
	static String csvFileName = "../../../shared-svn/projects/maxess/data/nairobi/kodi/Public_Primary_School_listed_by_2007.csv";
	static String attributeCaption = "LANDUSE";
	static String facilitiesOutputFile = "../../../shared-svn/projects/maxess/data/nairobi/kodi/facilities.xml";
	static final String SEPARATOR = ",";
	
	static final String originalCRS = "EPSG:4326";
	static final String workingCRS = "EPSG:21037";
	
	static final String LOCATION = "Geolocation";
	static final String NAME = "Name of School";
	
	
	// Globally used objects
	static ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities("Homes"); // TODO adapt name
	static ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
	static Map<String,Integer> columnHeads;
	
	static CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(originalCRS, workingCRS);


	public static void main(String[] args) {
//		Collection<SimpleFeature> blocks = collectFeatures();	
		readFile();
//		ActivityFacilities activityFacilities = createFacilities(blocks);
		writeFacilitiesFile(activityFacilities);
	}
	


	public static void readFile() {
		int lineCount = 0;

		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(csvFileName);

			// Header
			String currentLine = bufferedReader.readLine();
			String[] header = currentLine.split(SEPARATOR, -1);
			columnHeads = new LinkedHashMap<>(header.length);
			for (int i=0; i<header.length; i++) {
				columnHeads.put(header[i],i);
			}
			lineCount++;

			// Lines with data
			while ((currentLine = bufferedReader.readLine()) != null) {
				
				// This quite complicated expression does the following:
				// Split on the comma only if that comma has zero, or an even number of quotes ahead of it.
				// See: http://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
				String[] entries = currentLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

				lineCount++;


				if (lineCount % 100000 == 0) {
//					log.info(lineCount+ " lines read in so far.");
					Gbl.printMemoryUsage();
				}

				//			System.out.println(entries[13]);
				
				// household id / person id
				System.out.println("name = " + entries[columnHeads.get(NAME)]);
				System.out.println("loc = " + entries[columnHeads.get(LOCATION)]);
//				Id<Person> pid = Id.create(hid+"_"+id, Person.class);
				ActivityFacility activityFacility = createFacility(lineCount, entries);
				activityFacilities.addActivityFacility(activityFacility);
			}
		} catch (IOException e) {
//			log.error(new Exception(e));
		}
	}


	private static ActivityFacility createFacility(int lineCount, String[] entries) {
//		private static void createFacility(String[] entries) {

//			Id<ActivityFacility> id = Id.create(entries[columnHeads.get(NAME)] , ActivityFacility.class);
			Id<ActivityFacility> id = Id.create(lineCount , ActivityFacility.class);

			
			String location = entries[columnHeads.get(LOCATION)].replaceAll("[()\"]","");
			String[] coordinates = location.split(SEPARATOR, -1);
			Coord wgs84Coord = CoordUtils.createCoord(Double.parseDouble(coordinates[1]), Double.parseDouble(coordinates[0]));
			Coord coord = ct.transform(wgs84Coord);
			System.out.println("wgs84 = " + wgs84Coord + " -- coord = " + coord);

			ActivityFacility activityFacility = activityFacilitiesFactory.createActivityFacility(id, coord);
//			String landUseType = (String) feature.getAttribute("LANDUSE");
			ActivityOption activityOption = activityFacilitiesFactory.createActivityOption("Educational");
			activityFacility.addActivityOption(activityOption);
//			activityFacilities.addActivityFacility(activityFacility);
//			i++;
//		}
		return activityFacility;
	}


	private static void writeFacilitiesFile(ActivityFacilities activityFacilities) {
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(activityFacilities);
		facilitiesWriter.write(facilitiesOutputFile);
	}
}