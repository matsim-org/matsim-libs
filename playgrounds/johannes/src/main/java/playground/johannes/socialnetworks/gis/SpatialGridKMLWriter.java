/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.gis;

import java.awt.Color;

import net.opengis.kml._2.BoundaryType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LinearRingType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.PolyStyleType;
import net.opengis.kml._2.PolygonType;
import net.opengis.kml._2.StyleType;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.experimental.max.MaxCore;
import org.matsim.contrib.sna.graph.spatial.io.ColorUtils;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.johannes.socialnetworks.statistics.Normalizer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class SpatialGridKMLWriter {

	private ObjectFactory objectFactory = new ObjectFactory();
	
	private MathTransform transform;
	
	private GeometryFactory geoFactory = new GeometryFactory();
	
	private Discretizer discretizer = new LinearDiscretizer(0.1);
	
//	private Normalizer normalizer;
//	public CoordinateTransformation getCoordTransform() {
//		return coordTransform;
//	}
//
//	public void setCoordTransform(CoordinateTransformation coordTransform) {
//		this.coordTransform = coordTransform;
//	}

	public void write(SpatialGrid<Double> grid, String filename) {
		write(grid, DefaultGeographicCRS.WGS84, filename);
	}
	
	public void write(SpatialGrid<Double> grid, CoordinateReferenceSystem crs, String filename) {
		try {
			transform = CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		KMZWriter kmzWriter = new KMZWriter(filename);
		
		DocumentType documentType = objectFactory.createDocumentType();
		FolderType polyFolderType = objectFactory.createFolderType();
		polyFolderType.setName("Spatial Grid");
		
		FolderType labelFolderType = objectFactory.createFolderType();
		labelFolderType.setName("Labels");
		
		double minVal = Double.MAX_VALUE;
		double maxVal = - Double.MAX_VALUE;
		for(int row = 0; row < grid.getNumRows(); row++) {
			for(int col = 0; col < grid.getNumCols(row); col++) {
				Double val = grid.getValue(row, col);
				if(val != null) {
					minVal = Math.min(minVal, val);
					maxVal = Math.max(maxVal, val);
				}
			}
		}
		
		System.out.println("Min score = " + minVal + ", max socre = "+maxVal);
		double numBins = discretizer.index(maxVal) - discretizer.index(minVal) + 1;
//		double numBins = 0;
		double minBin = discretizer.index(minVal);
		
		System.out.println("num bins = " + numBins);
		
		StyleType[] polySytleTypes = new StyleType[(int) numBins];
		for(int i = 0; i < numBins; i++) {
			PolyStyleType polyStyleType = objectFactory.createPolyStyleType();
			Color c = ColorUtils.getGRBColor(i/(double)numBins);
			
			polyStyleType.setColor(new byte[]{(byte)200, (byte)c.getBlue(), (byte)c.getGreen(), (byte)c.getRed()});
			polyStyleType.setOutline(false);
//			polyStyleType.set
			
			StyleType styleType = objectFactory.createStyleType();
			styleType.setPolyStyle(polyStyleType);
			styleType.setId(Integer.toString((int)(i)));
			documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
			polySytleTypes[i] = styleType;
		}
		
		PolyStyleType contourPolyStyle = objectFactory.createPolyStyleType();
		contourPolyStyle.setColor(new byte[]{(byte)0, (byte)0, (byte)0, (byte)0});
		contourPolyStyle.setOutline(true);
		
		StyleType contourStyle = objectFactory.createStyleType();
		contourStyle.setPolyStyle(contourPolyStyle);
		contourStyle.setId("contourStyle");
		documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(contourStyle));
		
		
		IconStyleType iconStyleType = objectFactory.createIconStyleType();
		iconStyleType.setScale(0.0);
		StyleType lableStyleType = objectFactory.createStyleType();
		lableStyleType.setIconStyle(iconStyleType);
		lableStyleType.setId("label");
		documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(lableStyleType));
		
		for(int row = 0; row < grid.getNumRows(); row++) {
			for(int col = 0; col < grid.getNumCols(row); col++) {
				LinearRingType linearRingType = objectFactory.createLinearRingType();
		
				linearRingType.getCoordinates().add(makeCoordinateString(makeCoordinate(grid, row, col)));
				linearRingType.getCoordinates().add(makeCoordinateString(makeCoordinate(grid, row+1, col)));
				linearRingType.getCoordinates().add(makeCoordinateString(makeCoordinate(grid, row+1, col+1)));
				linearRingType.getCoordinates().add(makeCoordinateString(makeCoordinate(grid, row, col+1)));
				linearRingType.getCoordinates().add(makeCoordinateString(makeCoordinate(grid, row, col)));
				
				BoundaryType boundaryType = objectFactory.createBoundaryType();
				boundaryType.setLinearRing(linearRingType);
				
				PolygonType polygonType = objectFactory.createPolygonType();
				polygonType.setOuterBoundaryIs(boundaryType);
				
				PlacemarkType polyPlacemarkType = objectFactory.createPlacemarkType();
				polyPlacemarkType.setAbstractGeometryGroup(objectFactory.createPolygon(polygonType));
				Double val = grid.getValue(row, col);
				if(val != null) {
					int bin = (int) ((int) discretizer.index(val) - minBin);
//					int bin = (int)Math.floor((val - minVal)/binSize);
//					bin = (int) Math.min(bin, maxVal-minVal) - 1;
//					bin = (int) Math.max(bin, 0);
					
					polyPlacemarkType.setStyleUrl(polySytleTypes[bin].getId());
					polyPlacemarkType.setName(String.valueOf(val));
				} else {
					polyPlacemarkType.setStyleUrl("contourStyle");
					polyPlacemarkType.setName("");
				}
				
				
				polyFolderType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(polyPlacemarkType));
				
				PointType pointType = objectFactory.createPointType();
				Point c1 = makeCoordinate(grid, row, col);
				Point c2 = makeCoordinate(grid, row+1, col+1);
				c1.getCoordinate().x =  (c1.getX() + (c2.getX() - c1.getX())/2.0);
				c1.getCoordinate().y = (c1.getY() + (c2.getY() - c1.getY())/2.0);
				pointType.getCoordinates().add(makeCoordinateString(c1));
				PlacemarkType lablePlacemarkType = objectFactory.createPlacemarkType();
				lablePlacemarkType.setAbstractGeometryGroup(objectFactory.createPoint(pointType));
				lablePlacemarkType.setName(String.valueOf(grid.getValue(row, col)));
				lablePlacemarkType.setStyleUrl(lableStyleType.getId());
				labelFolderType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(lablePlacemarkType));
				
			}
		}
		
		documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(polyFolderType));
		documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(labelFolderType));
		
		KmlType kmlType = objectFactory.createKmlType();
		kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
		
		kmzWriter.writeMainKml(kmlType);
		kmzWriter.close();
	}
	
	private Point makeCoordinate(SpatialGrid<?> grid, int row, int col) {
		double x = grid.getXmin() + (col * grid.getResolution());
		double y = grid.getYmin() + (row * grid.getResolution());
		Point point;
		if(transform != null) {
			double[] points = new double[] { x, y };
			try {
				transform.transform(points, 0, points, 0, 1);
			} catch (TransformException e) {
				e.printStackTrace();
			}
			point = geoFactory.createPoint(new Coordinate(points[0], points[1]));
		} else {
			point = geoFactory.createPoint(new Coordinate(x,y));
		}
			
		return point;
	}
	private String makeCoordinateString(Point point) {
		StringBuffer buffer = new StringBuffer(50);
		buffer.append(Double.toString(point.getX()));
		buffer.append(",");
		buffer.append(Double.toString(point.getY()));
		return buffer.toString();
	}
	
	public static void main(String[] args) {
		SpatialGrid<Double> grid = SpatialGrid.readFromFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/popdensity/popdensity.1000.xml");
		SpatialGridKMLWriter writer = new SpatialGridKMLWriter();
//		writer.setCoordTransform(new CH1903LV03toWGS84());
		writer.write(grid, "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/popdensity/popdensity.1000.kmz");
	}
}
