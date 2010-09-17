/* *********************************************************************** *
 * project: org.matsim.*
 * MyKMLNetWriterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.andreas.osmBB.convertCountsData.mappingResultToKml;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;

import playground.andreas.osmBB.convertCountsData.CountStationDataBox;
import playground.andreas.osmBB.convertCountsData.ReadCountStations;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;

public class MappingResults2KMLWriter {

	private static final Logger log = Logger.getLogger(MappingResults2KMLWriter.class);

	private List<CountStationDataBox> countStations;

	public static void main(String[] args) {
		MappingResults2KMLWriter.writeCountsData2Kml("F:/convert/DZS-Koordinaten.csv", "F:/convert/countStations.kmz", new GK4toWGS84());
	}

	public static void writeCountsData2Kml(String countsFilename, String kmzFilename, CoordinateTransformation coordTransform){
		MappingResults2KMLWriter countsData2Kml = new MappingResults2KMLWriter();
		countsData2Kml.readCountStations(countsFilename);
		countsData2Kml.write2Kml(kmzFilename, coordTransform);
	}

	private void readCountStations(String countsFilename) {
		log.info("Reading count stations from " + countsFilename);
		this.countStations = ReadCountStations.readCountStations(countsFilename);		
	}

	public void write2Kml(String kmzFilename, CoordinateTransformation coordTransform) {
		log.info("Converting count stations data to kml...");
		


		// create document
		final Kml kml = new Kml();
		Document doc = kml.createAndSetDocument().withName("Count Stations").withOpen(true);
		
		HashMap<Integer, Folder> folderMap = new HashMap<Integer, Folder>();
		
//		1	genau zugeordnet
//		0	wegen fehlender Beschreibung gar nicht zugeordnet
//		2	bereits vorhanden als andere Zählstelle (short name)
//		3	zwei Zählstellen vorhanden, aber nur eine Kante (nur one way) – es konnte also nur eine Zählstelle eingefügt werden
//		4	keine Möglichkeit einen neuen Punkt auf dem Way für die Zählstelle hinzuzufügen				
		HashMap<Integer, String> iconMap = new HashMap<Integer, String>();
		iconMap.put(new Integer(0), "http://maps.google.com/mapfiles/kml/paddle/red-stars.png");
		iconMap.put(new Integer(1), "http://maps.google.com/mapfiles/kml/paddle/grn-blank.png");
		iconMap.put(new Integer(2), "http://maps.google.com/mapfiles/kml/shapes/arrow-reverse.png");
		iconMap.put(new Integer(3), "http://maps.google.com/mapfiles/kml/shapes/target.png");
		iconMap.put(new Integer(4), "http://maps.google.com/mapfiles/kml/shapes/cross-hairs.png");
		
		
		// convert count stations
		for (CountStationDataBox dataBox : this.countStations) {
			
			if(folderMap.get(new Integer(dataBox.getErrorCodeFromMapping())) == null){
				
				// create new folder for error type
				Folder folder = doc.createAndAddFolder();
			    folder.withName("Error code " + dataBox.getErrorCodeFromMapping()).withOpen(false);
			    folderMap.put(new Integer(dataBox.getErrorCodeFromMapping()), folder);
			    
			    Icon icon = new Icon().withHref(iconMap.get(new Integer(dataBox.getErrorCodeFromMapping())));
			    			    
			    Style style = doc.createAndAddStyle();
			    style.withId("style_" + String.valueOf(dataBox.getErrorCodeFromMapping()))
			    .createAndSetIconStyle().withScale(1.0).withIcon(icon);
			    style.createAndSetLabelStyle().withColor("ff43b3ff").withScale(1.0);
			}
			
			Folder folder = folderMap.get(new Integer(dataBox.getErrorCodeFromMapping()));
			Placemark placemark = folder.createAndAddPlacemark();
			placemark.withName(dataBox.getShortName() + " - " + dataBox.getUnitName());
			placemark.withStyleUrl("#style_" + String.valueOf(dataBox.getErrorCodeFromMapping()));
			placemark.withDescription(dataBox.toString());
			
			Coord coord = coordTransform.transform(dataBox.getCoord());
			placemark.createAndSetPoint().addToCoordinates(coord.getX(), coord.getY());
		}
		
		log.info("Writing kml data to " + kmzFilename);
		try {
			kml.marshal(new File(kmzFilename));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.info("... finished");
	}
}
