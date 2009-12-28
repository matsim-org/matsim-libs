/* *********************************************************************** *
 * project: org.matsim.*
 * KMLEgoNetWriter.java
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
package playground.johannes.socialnetworks.graph.spatial.io;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LineStringType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.StyleType;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.kml.KMZWriter;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;

/**
 * @author illenberger
 *
 */
public class KMLEgoNetWriter extends KMLWriter {

	public KMLEgoNetWriter() {
		super();
		// TODO Auto-generated constructor stub
	}

	//	private KMLObjectStyle<Graph, Vertex> vertexStyle;
//	
//	private KMLObjectStyle<Graph, Edge> edgeStyle;
//	
//	private KMLObjectDescriptor<Vertex> vertexDescriptor;
//	
//	private KMLObjectDescriptor<Edge> edgeDescriptor;
//	
//	private ObjectFactory objectFactory;
	
	public void write(SpatialSparseGraph graph, Set<? extends SpatialSparseVertex> egos, int radius, String filename) throws IOException {
		KMZWriter kmzWriter = new KMZWriter(filename);
		kmzWriter.addNonKMLFile(MatsimResource.getAsInputStream("icon18.png"), "node.png");
		
		DocumentType document = objectFactory.createDocumentType();
		
		FolderType graphFolder = objectFactory.createFolderType();
		graphFolder.setName("Egocentric networks");

		if(getVertexStyle() == null)
			setVertexStyle(new DefaultVertexStyle());
		/*
		 * get vertex styles
		 */
		for(StyleType styleType : getVertexStyle().getObjectStyle(graph)) {
			document.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
		}
		
		if(getEdgeStyle() == null)
			setEdgeStyle(new DefaultEdgeStyle());
		/*
		 * get edge styles
		 */
		for (StyleType styleType : getEdgeStyle().getObjectStyle(graph)) {
			document.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
		}
		
		for(SpatialSparseVertex ego : egos) {
			FolderType egoNetFolder = objectFactory.createFolderType();
			egoNetFolder.setName(getVertexDescriptor().getName(ego));
			
			expand(ego, new HashSet<SpatialSparseVertex>(), egoNetFolder, 0, radius);
			
			graphFolder.getAbstractFeatureGroup().add(objectFactory.createFolder(egoNetFolder));
		}
		
		document.getAbstractFeatureGroup().add(objectFactory.createFolder(graphFolder));
		
		KmlType kml = objectFactory.createKmlType();
		kml.setAbstractFeatureGroup(objectFactory.createDocument(document));
		
		kmzWriter.writeMainKml(kml);
		kmzWriter.close();
	}
	
	private void expand(SpatialSparseVertex v, Set<SpatialSparseVertex> expandedVertices, FolderType egoNetFolder, int currentRadius, int maxRadius) {
		/*
		 * draw vertex
		 */
		PointType point = objectFactory.createPointType();
		Coord coord = v.getCoordinate();
		if(getCoordinateTransformation() != null)
			coord = getCoordinateTransformation().transform(v.getCoordinate());
		point.getCoordinates().add(String.format("%1$s,%2$s", Double.toString(coord.getX()), Double.toString(coord.getY())));
		/*
		 * create placemark
		 */
		PlacemarkType placemark = objectFactory.createPlacemarkType();
		
		placemark.setAbstractGeometryGroup(objectFactory.createPoint(point));
		placemark.setStyleUrl(getVertexStyle().getObjectSytleId(v));
		if(getVertexDescriptor() != null) {
			placemark.setDescription(getVertexDescriptor().getDescription(v));
			if(isDrawNames())
				placemark.setName(getVertexDescriptor().getName(v));
		}
		/*
		 * add placemark to vertex folder
		 */
		egoNetFolder.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemark));
		
		expandedVertices.add(v);
		
		if(currentRadius < maxRadius) {
			for(SpatialSparseEdge e : v.getEdges()) {
				SpatialSparseVertex v2 = e.getOpposite(v);
				if(!expandedVertices.contains(v2)) {
					LineStringType lineString = objectFactory.createLineStringType();
//					SpatialVertex v1 = e.getVertices().getFirst();
//					SpatialVertex v2 = e.getVertices().getSecond();
					Coord c1 = v.getCoordinate();
					Coord c2 = v2.getCoordinate();
					if (getCoordinateTransformation() != null) {
						c1 = getCoordinateTransformation().transform(c1);
						c2 = getCoordinateTransformation().transform(c2);
					}
					lineString.getCoordinates().add(makeCoordinateString(c1));
					lineString.getCoordinates().add(makeCoordinateString(c2));
					/*
					 * create a placemark
					 */
					PlacemarkType placemark2 = objectFactory.createPlacemarkType();
					
					placemark2.setAbstractGeometryGroup(objectFactory.createLineString(lineString));
					placemark2.setStyleUrl(getEdgeStyle().getObjectSytleId(e));
					if(getEdgeDescriptor() != null) {
						placemark2.setDescription(getEdgeDescriptor().getDescription(e));
						if(isDrawNames())
							placemark2.setName(getEdgeDescriptor().getName(e));
					}
					/*
					 * add placemark to edge folder
					 */
					egoNetFolder.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemark2));
					
					expand(v2, expandedVertices, egoNetFolder, currentRadius + 1, maxRadius);
				}
			}
		}
	}
}
