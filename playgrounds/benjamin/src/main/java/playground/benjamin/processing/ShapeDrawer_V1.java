///* *********************************************************************** *
// * project: org.matsim.*
// * NetworkDrawer.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2009 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.benjamin.processing;
//
//import java.util.Collection;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.utils.gis.ShapeFileReader;
//import org.opengis.feature.simple.SimpleFeature;
//import org.opengis.geometry.BoundingBox;
//
//import playground.benjamin.utils.FeatureProjector;
//import processing.core.PApplet;
//import processing.core.PVector;
//
//import com.vividsolutions.jts.geom.Coordinate;
//import com.vividsolutions.jts.geom.GeometryCollection;
//import com.vividsolutions.jts.geom.MultiLineString;
//import com.vividsolutions.jts.geom.MultiPolygon;
//
///**
// * @author benjamin
// *
// */
//public class ShapeDrawer_V1 extends PApplet {
//	private static Logger logger = Logger.getLogger(ShapeDrawer_V1.class);
//
//	Collection<SimpleFeature> network;
//	Collection<SimpleFeature> munich;
//	
//	FeatureProjector proj;
//	
//	PVector brCorner;
//	PVector tlCorner;
//
//	@Override
//	public void setup(){
//
//		String networkShapeFileName = "/media/data/2_Workspaces/repos/shared-svn/projects/detailedEval/Net/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes-Lines.shp";
//		String munichShapeFileName = "/media/data/2_Workspaces/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/Landkreise_umMuenchen_Umrisse.shp";
//		
//		size(1000, 1000);
//		background(255, 255, 255);
//		
//		Collection<SimpleFeature> networkShape = readShape(networkShapeFileName);
//		Collection<SimpleFeature> munichShape = readShape(munichShapeFileName);
//		
//		// Transform from "DHDN / 3-degree Gauss zone 4" to "WGS 84"
//		// There is no need to do this, though. "geoToScreen(PVector coord)" defines screen position anyways...
//		proj = new FeatureProjector("EPSG:31464", "EPSG:4326");
//		
//		network = proj.getProjectedFeatures(networkShape);
//		munich = proj.getProjectedFeatures(munichShape);
//		
//		defineBoundingBox(network);
//	}
//
//	@Override
//	public void draw(){
//		noFill();
//		stroke(150, 50, 50);
//		strokeWeight((float) 0.1);
//		drawFeaturesToScreen(network);
//		
//		noFill();
//		stroke(0, 0, 0);
//		strokeWeight(1);
//		drawFeaturesToScreen(munich);
//	}
//	
//	private void drawFeaturesToScreen(Collection<SimpleFeature> featureCollection) {
//		for(SimpleFeature ft : featureCollection){
//			drawFeatureToScreen(ft);
//		}
//	}
//
//	private void drawFeatureToScreen(SimpleFeature ft){
//		GeometryCollection gc = (GeometryCollection) ft.getDefaultGeometry();
//		Coordinate[] coords = gc.getCoordinates();
//		
//		beginShape();
//		for(int i=0; i<coords.length; i++){
//			PVector coord = new PVector((float) coords[i].x, (float) coords[i].y);
//			PVector scrCoord = geoToScreen(coord);
//			vertex(scrCoord.x, scrCoord.y);
//		}
//		if(gc instanceof MultiLineString){
//			endShape();
//		} else if(gc instanceof MultiPolygon){
//			endShape(CLOSE);
//		} else {
//			throw new RuntimeException("Unsupported GeometryType: " + gc.getGeometryType());
//		}
//	}
//	
//	private PVector geoToScreen(PVector coord) {
//		float leftBoarder = (float) (0 + (width * 0.05));
//		float rightBoarder = (float) (width - (width * 0.05));
//		float topBoarder = (float) (0 + (height * 0.05));
//		float bottomBoarder = (float) (height - (height * 0.05));
//		return new PVector(map(coord.x, tlCorner.x, brCorner.x, leftBoarder, rightBoarder),
//                		   map(coord.y, tlCorner.y, brCorner.y, topBoarder, bottomBoarder));
//	}
//	
//	private void defineBoundingBox(Collection<SimpleFeature> features) {
//		
//		for(SimpleFeature ft : features){
//			BoundingBox env = ft.getBounds();
//			float maxX = (float) env.getMaxX();
//			float minX = (float) env.getMinX();
//			float maxY = (float) env.getMaxY();
//			float minY = (float) env.getMinY();
//
//			if(brCorner == null){
//				brCorner = new PVector(maxX, minY);
//			}
//			if(tlCorner == null){
//				tlCorner = new PVector(minX, maxY);
//			} else {
//				if(minX < tlCorner.x){
//					tlCorner.x = minX;
//				}
//				if(maxX > brCorner.x){
//					brCorner.x = maxX;
//				}
//				if(minY < brCorner.y){
//					brCorner.y = minY;
//				}
//				if(maxY > tlCorner.y){
//					tlCorner.y = maxY;
//				}
//			}
//		}
//	}
//
//	Collection<SimpleFeature> readShape(String shapeFile) {
//		return ShapeFileReader.getAllFeatures(shapeFile);
//	}
//
//	public static void main(String[] args) {
//		PApplet.main(new String[] {"--present", "playground.benjamin.processing.ShapeDrawer_V1"});
//	}
//}
