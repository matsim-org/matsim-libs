/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneLayerKMLWriter.java
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
package playground.johannes.socialnetworks.gis.io;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

import org.geotools.feature.Feature;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.spatial.io.Colorizable;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 * 
 */
public class FeatureKMLWriter {

	private Colorizable colorizable = new DefaultColorizer();

	private boolean drawContours;

	private final ObjectFactory objectFactory = new ObjectFactory();
	
	private int alpha = 200;
	
	private MathTransform transform;

	public void write(Set<Geometry> features, String filename) {
		CoordinateReferenceSystem sourceCRS = CRSUtils.getCRS(features.iterator().next().getSRID());
		try {
			transform = CRS.findMathTransform(sourceCRS, DefaultGeographicCRS.WGS84);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		KMZWriter kmzWriter = new KMZWriter(filename);

		DocumentType documentType = objectFactory.createDocumentType();
		FolderType polyFolderType = objectFactory.createFolderType();
		polyFolderType.setName("Zones");

		FolderType labelFolderType = objectFactory.createFolderType();
		labelFolderType.setName("Labels");
		/*
		 * Create a polygon style type for each color.
		 */
		Set<Color> colors = getColors(features);
		Map<Color, StyleType> styleTypes = new HashMap<Color, StyleType>();
		for(Color color : colors) {
			PolyStyleType polyStyleType = objectFactory.createPolyStyleType();
			polyStyleType.setColor(new byte[]{(byte) alpha, (byte)color.getBlue(), (byte)color.getGreen(), (byte)color.getRed()});
			polyStyleType.setOutline(drawContours);
			
			StyleType styleType = objectFactory.createStyleType();
			styleType.setPolyStyle(polyStyleType);
			styleType.setId(String.valueOf(color.hashCode()));
			documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
			
			styleTypes.put(color, styleType);
		}
		/*
		 * Create default style type.
		 */
		PolyStyleType polyStyleType = objectFactory.createPolyStyleType();
		polyStyleType.setFill(false);
		polyStyleType.setOutline(true);
		
		StyleType contourStyle = objectFactory.createStyleType();
		contourStyle.setPolyStyle(polyStyleType);
		contourStyle.setId("contourStyle");
		documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(contourStyle));
//		/*
//		 * Create style type for the lable. 
//		 */
////		IconStyleType iconStyleType = objectFactory.createIconStyleType();
////		iconStyleType.setScale(0.0);
//		StyleType lableStyleType = objectFactory.createStyleType();
////		lableStyleType.setIconStyle(iconStyleType);
//		lableStyleType.setId("label");
//		documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(lableStyleType));
		/*
		 * Draw polygons.
		 */
		for(Geometry feature : features) {
			LinearRingType linearRingType = objectFactory.createLinearRingType();
			
			Coordinate[] coordinates = feature.getCoordinates();
			for(Coordinate coord : coordinates) {
				linearRingType.getCoordinates().add(makeCoordinateString(coord));
			}
			linearRingType.getCoordinates().add(makeCoordinateString(coordinates[0]));
			
			BoundaryType boundaryType = objectFactory.createBoundaryType();
			boundaryType.setLinearRing(linearRingType);
			
			PolygonType polygonType = objectFactory.createPolygonType();
			polygonType.setOuterBoundaryIs(boundaryType);
			
			PlacemarkType polyPlacemarkType = objectFactory.createPlacemarkType();
			polyPlacemarkType.setAbstractGeometryGroup(objectFactory.createPolygon(polygonType));
			
			Color color = colorizable.getColor(feature);
			if(color != null) {		
				polyPlacemarkType.setStyleUrl(styleTypes.get(color).getId());
			} else {
				polyPlacemarkType.setStyleUrl(contourStyle.getId());
			}
			polyFolderType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(polyPlacemarkType));
		}
		
		documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(polyFolderType));
		documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(labelFolderType));
		
		KmlType kmlType = objectFactory.createKmlType();
		kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
		
		kmzWriter.writeMainKml(kmlType);
		kmzWriter.close();
	}

	private Set<Color> getColors(Set<Geometry> features) {
		Set<Color> colors = new HashSet<Color>();
		for (Geometry feature : features) {
			Color color = colorizable.getColor(feature);
			if (color != null)
				colors.add(color);
		}
		return colors;

	}
	
	private String makeCoordinateString(Coordinate coordinate) {
		if(transform != null) {
			double[] points = new double[]{coordinate.x, coordinate.y};
			try {
				transform.transform(points, 0, points, 0, 1);
			} catch (TransformException e) {
				e.printStackTrace();
				return null;
			}
			
			StringBuffer buffer = new StringBuffer(50);
			buffer.append(Double.toString(points[0]));
			buffer.append(",");
			buffer.append(Double.toString(points[1]));
			return buffer.toString();
		} else {
			StringBuffer buffer = new StringBuffer(50);
			buffer.append(Double.toString(coordinate.x));
			buffer.append(",");
			buffer.append(Double.toString(coordinate.y));
			return coordinate.toString();
		}
	}
	
	private class DefaultColorizer implements Colorizable {

		@Override
		public Color getColor(Object object) {
			return null;
		}
		
	}

	public static void main(String args[]) throws IOException {
		ZoneLayer zoneLayer = ZoneLayerSHP.read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1G08.shp");
		zoneLayer.overwriteCRS(CRSUtils.getCRS(21781));
		
		FeatureKMLWriter writer = new FeatureKMLWriter();
		
		Set<Geometry> features = new HashSet<Geometry>();
		for(Zone zone : zoneLayer.getZones()) {
			features.add(zone.getGeometry());
		}
		writer.write(features, "/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1G08.kmz");
		
	}
}
