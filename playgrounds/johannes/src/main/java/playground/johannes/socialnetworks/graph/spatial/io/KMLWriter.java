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
package playground.johannes.socialnetworks.graph.spatial.io;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LineStringType;
import net.opengis.kml._2.LineStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.StyleType;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vis.kml.KMZWriter;


/**
 * @author illenberger
 *
 */
public class KMLWriter {
	
	private static final String COMMA = ",";
	
	private CoordinateTransformation transformation;
	
	private boolean drawVertices = true;
	
	private boolean drawEdges = true;
	
	private boolean drawNames = false;
	
	private KMLObjectStyle<Graph, Vertex> vertexStyle;
	
	private KMLObjectStyle<Graph, Edge> edgeStyle;
	
	private KMLObjectDescriptor<Vertex> vertexDescriptor;
	
	private KMLObjectDescriptor<Edge> edgeDescriptor;
	
	protected ObjectFactory objectFactory;
	
	private LinkType vertexIconLink;
	
	public KMLWriter() {
		objectFactory = new ObjectFactory();
		vertexIconLink = objectFactory.createLinkType();
		vertexIconLink.setHref("node.png");
	}
	
	public boolean isDrawVertices() {
		return drawVertices;
	}

	public void setDrawVertices(boolean drawVertices) {
		this.drawVertices = drawVertices;
	}

	public boolean isDrawEdges() {
		return drawEdges;
	}

	public void setDrawEdges(boolean drawEdges) {
		this.drawEdges = drawEdges;
	}

	public boolean isDrawNames() {
		return drawNames;
	}
	
	public void setDrawNames(boolean drawNames) {
		this.drawNames = drawNames;
	}
	
	public KMLObjectStyle<Graph, Vertex> getVertexStyle() {
		return vertexStyle;
	}

	@SuppressWarnings("unchecked")
	public void setVertexStyle(KMLObjectStyle<? extends Graph, ? extends Vertex> vertexStyle) {
		this.vertexStyle = (KMLObjectStyle<Graph, Vertex>) vertexStyle;
	}

	public KMLObjectStyle<Graph, Edge> getEdgeStyle() {
		return edgeStyle;
	}

	@SuppressWarnings("unchecked")
	public void setEdgeStyle(KMLObjectStyle<? extends Graph, ? extends Edge> edgeStyle) {
		this.edgeStyle = (KMLObjectStyle<Graph, Edge>) edgeStyle;
	}

	public KMLObjectDescriptor<Vertex> getVertexDescriptor() {
		return vertexDescriptor;
	}

	@SuppressWarnings("unchecked")
	public void setVertexDescriptor(KMLObjectDescriptor<? extends Vertex> vertexDescriptor) {
		this.vertexDescriptor = (KMLObjectDescriptor<Vertex>) vertexDescriptor;
	}

	public KMLObjectDescriptor<Edge> getEdgeDescriptor() {
		return edgeDescriptor;
	}

	@SuppressWarnings("unchecked")
	public void setEdgeDescriptor(KMLObjectDescriptor<? extends Edge> edgeDescriptor) {
		this.edgeDescriptor = (KMLObjectDescriptor<Edge>) edgeDescriptor;
	}

	public CoordinateTransformation getCoordinateTransformation() {
		return transformation;
	}
	
	public void setCoordinateTransformation(CoordinateTransformation transformation) {
		this.transformation = transformation;
	}
	
	public LinkType getVertexIconLink() {
		return vertexIconLink;
	}
	
	public void write(SpatialSparseGraph graph, String filename) throws IOException {
		KMZWriter kmzWriter = new KMZWriter(filename);
		kmzWriter.addNonKMLFile(MatsimResource.getAsInputStream("icon18.png"), "node.png");
		
		DocumentType document = objectFactory.createDocumentType();
		
		FolderType graphFolder = objectFactory.createFolderType();
		graphFolder.setName("Social Network");
		/*
		 * write vertices
		 */
		if(drawVertices) {
			if(vertexStyle == null)
				vertexStyle = new DefaultVertexStyle();
			/*
			 * get vertex styles
			 */
			for(StyleType styleType : vertexStyle.getObjectStyle(graph)) {
				document.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
			}
			/*
			 * create vertex folder
			 */
			FolderType vertexFolder = objectFactory.createFolderType();
			vertexFolder.setName("Vertices");
			
			for(SpatialSparseVertex v : graph.getVertices()) {
				/*
				 * create a point geometry
				 */
				PointType point = objectFactory.createPointType();
				Coord coord = v.getCoordinate();
				if(transformation != null)
					coord = transformation.transform(v.getCoordinate());
				point.getCoordinates().add(String.format("%1$s,%2$s", Double.toString(coord.getX()), Double.toString(coord.getY())));
				/*
				 * create placemark
				 */
				PlacemarkType placemark = objectFactory.createPlacemarkType();
				
				placemark.setAbstractGeometryGroup(objectFactory.createPoint(point));
				placemark.setStyleUrl(vertexStyle.getObjectSytleId(v));
				if(vertexDescriptor != null) {
					placemark.setDescription(vertexDescriptor.getDescription(v));
					if(drawNames)
						placemark.setName(vertexDescriptor.getName(v));
				}
				/*
				 * add placemark to vertex folder
				 */
				vertexFolder.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemark));
			}
			
			graphFolder.getAbstractFeatureGroup().add(objectFactory.createFolder(vertexFolder));
		}
		
		if (drawEdges) {
			if(edgeStyle == null)
				edgeStyle = new DefaultEdgeStyle();
			/*
			 * get edge styles
			 */
			for (StyleType styleType : edgeStyle.getObjectStyle(graph)) {
				document.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
			}
			/*
			 * create edge folder
			 */
			FolderType edgeFolder = objectFactory.createFolderType();
			edgeFolder.setName("Edges");
			
			for (SpatialSparseEdge e : graph.getEdges()) {
				/*
				 * create a line geometry
				 */
				LineStringType lineString = objectFactory.createLineStringType();
				SpatialSparseVertex v1 = e.getVertices().getFirst();
				SpatialSparseVertex v2 = e.getVertices().getSecond();
				Coord c1 = v1.getCoordinate();
				Coord c2 = v2.getCoordinate();
				if (transformation != null) {
					c1 = transformation.transform(c1);
					c2 = transformation.transform(c2);
				}
				lineString.getCoordinates().add(makeCoordinateString(c1));
				lineString.getCoordinates().add(makeCoordinateString(c2));
				/*
				 * create a placemark
				 */
				PlacemarkType placemark = objectFactory.createPlacemarkType();
				
				placemark.setAbstractGeometryGroup(objectFactory.createLineString(lineString));
				placemark.setStyleUrl(edgeStyle.getObjectSytleId(e));
				if(edgeDescriptor != null) {
					placemark.setDescription(edgeDescriptor.getDescription(e));
					if(drawNames)
						placemark.setName(edgeDescriptor.getName(e));
				}
				/*
				 * add placemark to edge folder
				 */
				edgeFolder.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemark));
			}
			
			graphFolder.getAbstractFeatureGroup().add(objectFactory.createFolder(edgeFolder));
		}
		
		document.getAbstractFeatureGroup().add(objectFactory.createFolder(graphFolder));
		
		KmlType kml = objectFactory.createKmlType();
		kml.setAbstractFeatureGroup(objectFactory.createDocument(document));
		
		kmzWriter.writeMainKml(kml);
		kmzWriter.close();
	}
	
	protected String makeCoordinateString(Coord coord) {
		StringBuffer buffer = new StringBuffer(50);
		buffer.append(Double.toString(coord.getX()));
		buffer.append(COMMA);
		buffer.append(Double.toString(coord.getY()));
		return buffer.toString();
	}
	
	protected class DefaultVertexStyle implements KMLObjectStyle<Graph, Vertex> {

		private static final String DEFAULT_VERTEX_STYLE_ID = "defaultVertexStyle";
		
		public List<StyleType> getObjectStyle(Graph graph) {
			StyleType styleType = objectFactory.createStyleType();
			styleType.setId(DEFAULT_VERTEX_STYLE_ID);
			
			IconStyleType iconStyle = objectFactory.createIconStyleType();
			iconStyle.setIcon(vertexIconLink);
			iconStyle.setScale(0.5);
			
			styleType.setIconStyle(iconStyle);
			
			List<StyleType> styleTypes = new ArrayList<StyleType>(1);
			styleTypes.add(styleType);
			return styleTypes;
		}

		public String getObjectSytleId(Vertex object) {
			return DEFAULT_VERTEX_STYLE_ID;
		}
		
	}
	
	protected class DefaultEdgeStyle implements KMLObjectStyle<Graph, Edge> {
	
		private static final String DEFAULT_EDGE_STYLE_ID = "defaultEdgeStyle";
		
		public List<StyleType> getObjectStyle(Graph graph) {
			LineStyleType lineStyle = objectFactory.createLineStyleType();
			Color c = Color.WHITE;
			lineStyle.setColor(new byte[]{(byte)c.getAlpha(), (byte)c.getBlue(), (byte)c.getGreen(), (byte)c.getRed()});
			lineStyle.setWidth(1.0);
			
			StyleType type = objectFactory.createStyleType();
			type.setId(DEFAULT_EDGE_STYLE_ID);
			type.setLineStyle(lineStyle);
			
			List<StyleType> styleTypes = new ArrayList<StyleType>(1);
			styleTypes.add(type);
			return styleTypes;
		}

		public String getObjectSytleId(Edge object) {
			return DEFAULT_EDGE_STYLE_ID;
		}
		
	}
}
