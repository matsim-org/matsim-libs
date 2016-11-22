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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.accessibility.osm.AccessibilityOsmUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;

/**
 * @author gthunig, dziemke
 *
 * This class searches for data about refugee initiatives on the berlin.de website and creates facilities based on them and
 * then writes them into a facilities file.
 */
public class CreateFacilitiesFromBerlinRefugeeInitiatives {
    private static final Logger LOG = Logger.getLogger(CreateFacilitiesFromBerlinRefugeeInitiatives.class);

    public static void main(String[] args) {
    	
    	String urlString = "https://www.berlin.de/fluechtlinge/berlin-engagiert-sich/berliner-initiativen/";
        String facilitiesFile = "../../../shared-svn/projects/accessibility_berlin/refugee/initiatives.xml";
        String facilitiesFileDescription = "Refugee-relevant facilities in Berlin";
    	
    	String inputCRS = "EPSG:4326"; // WGS84
		String outputCRS = "EPSG:31468"; // = DHDN GK4
    	
    	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
    	
    	ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities(facilitiesFileDescription);
    	
        try {
            URL url = new URL(urlString);
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                LOG.info("Reading " + url);

                String line;
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains("mapOptions.markerList.push({")) {
                            String title = null;
                            Coord coord = null;
                            String[] lineArray;
                            line = bufferedReader.readLine();
                            if (line.contains("coordinates")) {
                                lineArray = line.split("'");
                                String[] coordStrings = lineArray[1].split(",");
                                double lat = Double.parseDouble(coordStrings[0]);
                                double lon = Double.parseDouble(coordStrings[1]);
                                coord = CoordUtils.createCoord(lon, lat);
                            }
                            bufferedReader.readLine();
                            bufferedReader.readLine();
                            bufferedReader.readLine();
                            bufferedReader.readLine();
                            line = bufferedReader.readLine();
                            if (line.contains("title")) {
                                lineArray = line.split("'");
                                title = lineArray[1];
                            }
                            if (title != null && coord != null) {
                            	System.out.println("title = " + title + " -- coord = " + coord);
                            	ActivityFacility activityFacility = createFacility(title, coord, facilitiesFile, ct);
                            	activityFacilities.addActivityFacility(activityFacility);
                            }
                        }
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        writeFacilitiesFile(activityFacilities, facilitiesFile);
    }

    private static ActivityFacility createFacility(String title, Coord coord, String outputFile, CoordinateTransformation ct) {
    		String name = AccessibilityOsmUtils.simplifyString(title);
    	
        	Id<ActivityFacility> id = Id.create(name , ActivityFacility.class);
    		Coord transformedCoord = ct.transform(coord);
    		System.out.println("transformedCoord = " + transformedCoord + " -- coord = " + coord);
        	
        	ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
    		ActivityFacility activityFacility = activityFacilitiesFactory.createActivityFacility(id, transformedCoord);
    		ActivityOption activityOption = activityFacilitiesFactory.createActivityOption("Refugee_Initiative");
    		activityFacility.addActivityOption(activityOption);
        return activityFacility;
    }

    
    private static void writeFacilitiesFile(ActivityFacilities activityFacilities, String facilitiesOutputFile) {
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(activityFacilities);
		facilitiesWriter.write(facilitiesOutputFile);
		LOG.info("Facility file written.");
	}
}