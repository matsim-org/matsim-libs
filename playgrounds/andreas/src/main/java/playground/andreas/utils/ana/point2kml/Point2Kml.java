/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.utils.ana.point2kml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

/**
 * Simple xy point to kml writer
 * 
 * @author aneumann
 *
 */
public class Point2Kml {
	
	public static void writeXYPoints2Kml(CoordinateTransformation coordTransform, String inputDataFile, String kmlOutFile){
		
		try {
			List<XYPointData> xyPointDataList = ReadXYPoints.readXYPointData(inputDataFile);
			
			final Kml kml = new Kml();
			Document doc = kml.createAndSetDocument().withName("XYPoints").withOpen(Boolean.TRUE);
			
			for (XYPointData xyPointData : xyPointDataList) {
				
				Coord coord = coordTransform.transform(xyPointData.getCoord());
				
				Placemark placemark = new Placemark();
				
				placemark.setId(xyPointData.getHeadline());
				placemark.setName(xyPointData.getHeadline());
				placemark.setDescription(xyPointData.getDescription());
				placemark.createAndSetPoint().addToCoordinates(coord.getX(), coord.getY());
				doc.addToFeature(placemark);
			}
			kml.marshal(new File(kmlOutFile));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}	
	
	public static void main(String[] args) {
		CoordinateTransformation coordTransform = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);
		String inputDataFile = "f:/temp/input.csv";
		String kmlOutFile = "f:/temp/kmlOut.kmz";
		Point2Kml.writeXYPoints2Kml(coordTransform, inputDataFile, kmlOutFile);
	}

}
