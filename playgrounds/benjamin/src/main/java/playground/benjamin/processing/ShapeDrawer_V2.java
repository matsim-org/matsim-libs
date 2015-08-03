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
//import java.awt.Polygon;
//import java.util.Collection;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.utils.gis.ShapeFileReader;
//import org.opengis.feature.simple.SimpleFeature;
//import org.opengis.geometry.BoundingBox;
//
//import processing.core.PApplet;
//import processing.core.PShape;
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
//public class ShapeDrawer_V2 extends PApplet {
//	private static Logger logger = Logger.getLogger(ShapeDrawer_V2.class);
//
//	PShape network;
//	PShape munich;
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
//		size(1000, 1000, P2D);
////		background(0, 0, 0);
//		
////		Collection<SimpleFeature> networkShape = readShape(networkShapeFileName);
//		Collection<SimpleFeature> munichShape = readShape(munichShapeFileName);
//		
////		defineBoundingBox(networkShape);
//		defineBoundingBox(munichShape);
//		
////		network = getPShapes(networkShape);
//		munich = getPShapes(munichShape);
//	}
//
//	@Override
//	public void draw(){
//		background(0, 0, 0);
//		
////		shape(network);
//		shape(munich);
//		
////		PShape testChild = munich.getChild(0);
////		if(mousePressed){
////			Polygon p = getPolygon(testChild); 
////
////			if(p.contains(mouseX, mouseY)){
////				testChild.fill(150, 50, 50);
////				testChild.noStroke();
////			}
////			shape(testChild);
////		}
////		testChild.noFill();
//
//		for(PShape childShape : munich.getChildren()){
//			if(mousePressed){
//				Polygon p = getPolygon(childShape); 
//
//				if(p.contains(mouseX, mouseY)){
//					childShape.fill(150, 50, 50);
//					childShape.noStroke();
//				}
//				shape(childShape);
//			}
//			childShape.noFill();
//		}
//	}
//
//	private Polygon getPolygon(PShape shape) {
//		int vertexCount = shape.getVertexCount();
//		
//		int[] xCoords = new int[vertexCount];
//		int[] yCoords = new int[vertexCount];
//		
//		for(int i=0; i<vertexCount; i++){
//			xCoords[i] = (int) shape.getVertex(i).x;
//			yCoords[i] = (int) shape.getVertex(i).y;
//		}
//		return new Polygon(xCoords, yCoords, vertexCount);
//	}
//
//	private PShape getPShapes(Collection<SimpleFeature> featureCollection) {
//		PShape shapes = createShape(GROUP);
//		
//		for(SimpleFeature ft : featureCollection){
//			PShape featureShape = getPShape(ft);
//			shapes.addChild(featureShape);
//		}
//		logger.info("Number of children in parent PShape: " + shapes.getChildren().length);
//		return shapes;
//	}
//	
//	private PShape getPShape(SimpleFeature ft) {
//		PShape featureShape;
//		GeometryCollection gc = (GeometryCollection) ft.getDefaultGeometry();
//		PVector[] scrCoords = getScreenCoords(gc);
//
//		if(gc instanceof MultiLineString){
//			featureShape = createShape(LINES);
//			for(PVector scrCoord : scrCoords){
//				featureShape.vertex(scrCoord.x, scrCoord.y);
//			}
//			featureShape.noFill();
//			featureShape.stroke(150, 50, 50);
//			featureShape.strokeWeight(1);
//			featureShape.end();
//		} else if(gc instanceof MultiPolygon){
//			featureShape = createShape();
//			for(PVector scrCoord : scrCoords){
//				featureShape.vertex(scrCoord.x, scrCoord.y);
//			}
//			featureShape.noFill();
//			featureShape.stroke(255, 255, 255);
//			featureShape.strokeWeight(5);
//			featureShape.end(CLOSE);
//		} else {
//			throw new RuntimeException("Unsupported GeometryType: " + gc.getGeometryType());
//		}
////		logger.info("Number of points in GeometryCollection: " + gc.getNumPoints());
////		logger.info("Length of scrCoords array: " + scrCoords.length);
////		logger.info("Number of vertices in PShape: " + featureShape.getVertexCount());
//		return featureShape;
//	}
//
//	private PVector[] getScreenCoords(GeometryCollection gc) {
//		Coordinate[] coords = gc.getCoordinates();
//		PVector[] scrCoords = new PVector[coords.length];
//		
//		for(int i=0; i<coords.length; i++){
//			PVector coord = new PVector((float) coords[i].x, (float) coords[i].y);
//			PVector scrCoord = geoToScreen(coord);
//			scrCoords[i] = scrCoord;
//		}
//		return scrCoords;
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
//		PApplet.main(new String[] {"--present", "playground.benjamin.processing.ShapeDrawer_V2"});
//	}
//}
