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

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.spatial.io.ColorUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.vis.kml.KMZWriter;

/**
 * @author illenberger
 *
 */
public class SpatialGridKMLWriter {

	private ObjectFactory objectFactory = new ObjectFactory();
	
	private CoordinateTransformation coordTransform;
	
	public CoordinateTransformation getCoordTransform() {
		return coordTransform;
	}

	public void setCoordTransform(CoordinateTransformation coordTransform) {
		this.coordTransform = coordTransform;
	}

	public void write(SpatialGrid<Double> grid, String filename) {
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
//		
//		minVal = 100;
//		maxVal = 250;
		double binSize = 1.0;
		double numBins = (int) Math.ceil((maxVal - minVal)/binSize);
		
		System.out.println("num bins = " + numBins);
		
		StyleType[] polySytleTypes = new StyleType[(int) numBins];
		for(int i = 0; i < numBins; i++) {
			PolyStyleType polyStyleType = objectFactory.createPolyStyleType();
			Color c = ColorUtils.getGRBColor(i/(double)numBins);
			
			polyStyleType.setColor(new byte[]{(byte)100, (byte)c.getBlue(), (byte)c.getGreen(), (byte)c.getRed()});
			polyStyleType.setOutline(false);
			
			StyleType styleType = objectFactory.createStyleType();
			styleType.setPolyStyle(polyStyleType);
			styleType.setId(Integer.toString((int)(i * binSize)));
			documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
			polySytleTypes[i] = styleType;
		}
		
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
					int bin = (int)Math.floor((val - minVal)/binSize);
					bin = (int) Math.min(bin, maxVal-minVal) - 1;
					bin = (int) Math.max(bin, 0);
					
					polyPlacemarkType.setStyleUrl(polySytleTypes[bin].getId());
					polyPlacemarkType.setName(String.valueOf(val));
				}
				
				
				polyFolderType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(polyPlacemarkType));
				
				PointType pointType = objectFactory.createPointType();
				Coord c1 = makeCoordinate(grid, row, col);
				Coord c2 = makeCoordinate(grid, row+1, col+1);
				c1.setX(c1.getX() + (c2.getX() - c1.getX())/2.0);
				c1.setY(c1.getY() + (c2.getY() - c1.getY())/2.0);
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
	
	private Coord makeCoordinate(SpatialGrid<?> grid, int row, int col) {
		double x = grid.getXmin() + (row * grid.getResolution());
		double y = grid.getYmin() + (col * grid.getResolution());
		Coord c = new CoordImpl(x, y);
		if(coordTransform != null)
			c = coordTransform.transform(c);
		return c;
	}
	private String makeCoordinateString(Coord coord) {
		StringBuffer buffer = new StringBuffer(50);
		buffer.append(Double.toString(coord.getX()));
		buffer.append(",");
		buffer.append(Double.toString(coord.getY()));
		return buffer.toString();
	}
	
	public static void main(String[] args) {
		SpatialGrid<Double> grid = SpatialGrid.readFromFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/popdensity/popdensity.1000.xml");
		SpatialGridKMLWriter writer = new SpatialGridKMLWriter();
		writer.setCoordTransform(new CH1903LV03toWGS84());
		writer.write(grid, "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/popdensity/popdensity.1000.kmz");
	}
}
