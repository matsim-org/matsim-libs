/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneLayerKMLWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.spatial;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.awt.Color;

import net.opengis.kml._2.BoundaryType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LinearRingType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PolyStyleType;
import net.opengis.kml._2.PolygonType;
import net.opengis.kml._2.StyleType;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.spatial.io.ColorUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.vis.kml.KMZWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class ZoneLayerKMLWriter {

	private ObjectFactory objectFactory = new ObjectFactory();
	
	private CoordinateTransformation transform = new CH1903LV03toWGS84();
	
	public void write(GeometryLayer zoneLayer, String filename, TObjectDoubleHashMap<Geometry> geoValues) {
		
		KMZWriter kmzWriter = new KMZWriter(filename);
		DocumentType documentType = objectFactory.createDocumentType();
		FolderType polyFolderType = objectFactory.createFolderType();
		
		double minVal = Double.MAX_VALUE;
		double maxVal = Double.MIN_VALUE;
		TObjectDoubleIterator<Geometry> it = geoValues.iterator();
		for(int i = 0; i < geoValues.size(); i++) {
			it.advance();
			minVal = Math.min(minVal, it.value());
			maxVal = Math.max(maxVal, it.value());
		}
		TDoubleObjectHashMap<StyleType> styles = new TDoubleObjectHashMap<StyleType>();
		for(int i = 0; i < 100; i++) {
			PolyStyleType polyStyleType = objectFactory.createPolyStyleType();
			Color c = ColorUtils.getGRBColor(i/(double)100);
			
			polyStyleType.setColor(new byte[]{(byte)200, (byte)c.getBlue(), (byte)c.getGreen(), (byte)c.getRed()});
			polyStyleType.setOutline(false);
			
			StyleType styleType = objectFactory.createStyleType();
			styleType.setPolyStyle(polyStyleType);
			styleType.setId(Integer.toString((int)(i)));
			documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
			styles.put(i, styleType);
		}
		
		
		polyFolderType.setName("Zonelayer");
		
		for(Geometry geometry : zoneLayer.getZones()) {
			LinearRingType polygon = objectFactory.createLinearRingType();
			
			for(Coordinate coordinate : geometry.getCoordinates()) {
				polygon.getCoordinates().add(makeCoordinateString(coordinate));
			}
			polygon.getCoordinates().add(makeCoordinateString(geometry.getCoordinates()[0]));
				
			BoundaryType boundaryType = objectFactory.createBoundaryType();
			boundaryType.setLinearRing(polygon);
			
			PolygonType polygonType = objectFactory.createPolygonType();
			polygonType.setOuterBoundaryIs(boundaryType);
			
			PlacemarkType polyPlacemarkType = objectFactory.createPlacemarkType();
			polyPlacemarkType.setAbstractGeometryGroup(objectFactory.createPolygon(polygonType));
			
			double val = geoValues.get(geometry) - minVal;
			val = val/(maxVal - minVal) * 99;
			polyPlacemarkType.setStyleUrl(styles.get((int)val).getId());
			
			polyFolderType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(polyPlacemarkType));
		}
		
		documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(polyFolderType));
		
		
		KmlType kmlType = objectFactory.createKmlType();
		kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
		
		kmzWriter.writeMainKml(kmlType);
		kmzWriter.close();
	}
	
	private String makeCoordinateString(Coordinate coordinate) {
		StringBuilder builder = new StringBuilder(50);
		Coord c = new CoordImpl(coordinate.x, coordinate.y);
		c = transform.transform(c);
		builder.append(String.valueOf(c.getX()));
		builder.append(",");
		builder.append(String.valueOf(c.getY()));
		
		return builder.toString();
	}
}
