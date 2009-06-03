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
package playground.johannes.socialnet.io;

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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vis.kml.KMZWriter;

import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialTie;

/**
 * @author illenberger
 *
 */
public class SNKMLWriter<P extends BasicPerson<?>> {
	
	private static final String COMMA = ",";
	
	private CoordinateTransformation transformation;
	
	private boolean drawVertices = true;
	
	private boolean drawEdges = true;
	
	private SNKMLObjectStyle<Ego<P>, P> vertexStyle;
	
	private SNKMLObjectStyle<SocialTie, P> edgeStyle;
	
	private SNKMLObjectDescriptor<Ego<P>> vertexDescriptor;
	
	private SNKMLObjectDescriptor<SocialTie> edgeDescriptor;
	
	private ObjectFactory objectFactory;
	
	private LinkType vertexIconLink;
	
	public SNKMLWriter() {
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

	public SNKMLObjectStyle<Ego<P>, P> getVertexStyle() {
		return vertexStyle;
	}

	public void setVertexStyle(SNKMLObjectStyle<Ego<P>, P> vertexStyle) {
		this.vertexStyle = vertexStyle;
	}

	public SNKMLObjectStyle<SocialTie, P> getEdgeStyle() {
		return edgeStyle;
	}

	public void setEdgeStyle(SNKMLObjectStyle<SocialTie, P> edgeStyle) {
		this.edgeStyle = edgeStyle;
	}

	public SNKMLObjectDescriptor<Ego<P>> getVertexDescriptor() {
		return vertexDescriptor;
	}

	public void setVertexDescriptor(SNKMLObjectDescriptor<Ego<P>> vertexDescriptor) {
		this.vertexDescriptor = vertexDescriptor;
	}

	public SNKMLObjectDescriptor<SocialTie> getEdgeDescriptor() {
		return edgeDescriptor;
	}

	public void setEdgeDescriptor(SNKMLObjectDescriptor<SocialTie> edgeDescriptor) {
		this.edgeDescriptor = edgeDescriptor;
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
	
	public void write(SocialNetwork<P> socialnet, String filename) throws IOException {
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
			for(StyleType styleType : vertexStyle.getObjectStyle(socialnet)) {
				document.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
			}
			/*
			 * create vertex folder
			 */
			FolderType vertexFolder = objectFactory.createFolderType();
			vertexFolder.setName("Vertices");
			
			for(Ego<P> e : socialnet.getVertices()) {
				/*
				 * create a point geometry
				 */
				PointType point = objectFactory.createPointType();
				Coord coord = e.getCoordinate();
				if(transformation != null)
					coord = transformation.transform(e.getCoordinate());
				point.getCoordinates().add(String.format("%1$s,%2$s", Double.toString(coord.getX()), Double.toString(coord.getY())));
				/*
				 * create placemark
				 */
				PlacemarkType placemark = objectFactory.createPlacemarkType();
				
				placemark.setAbstractGeometryGroup(objectFactory.createPoint(point));
				placemark.setStyleUrl(vertexStyle.getObjectSytleId(e));
				if(vertexDescriptor != null) {
					placemark.setDescription(vertexDescriptor.getDescription(e));
					placemark.setName(vertexDescriptor.getName(e));
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
			for (StyleType styleType : edgeStyle.getObjectStyle(socialnet)) {
				document.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(styleType));
			}
			/*
			 * create edge folder
			 */
			FolderType edgeFolder = objectFactory.createFolderType();
			edgeFolder.setName("Edges");
			
			for (SocialTie tie : socialnet.getEdges()) {
				/*
				 * create a line geometry
				 */
				LineStringType lineString = objectFactory.createLineStringType();
				Ego<?> e1 = tie.getVertices().getFirst();
				Ego<?> e2 = tie.getVertices().getSecond();
				Coord c1 = e1.getCoordinate();
				Coord c2 = e2.getCoordinate();
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
				placemark.setStyleUrl(edgeStyle.getObjectSytleId(tie));
				if(edgeDescriptor != null) {
					placemark.setDescription(edgeDescriptor.getDescription(tie));
					placemark.setName(edgeDescriptor.getName(tie));
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
	
	private String makeCoordinateString(Coord coord) {
		StringBuffer buffer = new StringBuffer(50);
		buffer.append(Double.toString(coord.getX()));
		buffer.append(COMMA);
		buffer.append(Double.toString(coord.getY()));
		return buffer.toString();
	}
	
	private class DefaultVertexStyle implements SNKMLObjectStyle<Ego<P>, P> {

		private static final String DEFAULT_VERTEX_STYLE_ID = "defaultVertexStyle";
		
		public List<StyleType> getObjectStyle(SocialNetwork<P> socialnet) {
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

		public String getObjectSytleId(Ego<P> object) {
			return DEFAULT_VERTEX_STYLE_ID;
		}
		
	}
	
	private class DefaultEdgeStyle implements SNKMLObjectStyle<SocialTie, P> {
	
		private static final String DEFAULT_EDGE_STYLE_ID = "defaultEdgeStyle";
		
		public List<StyleType> getObjectStyle(SocialNetwork<P> socialnet) {
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

		public String getObjectSytleId(SocialTie object) {
			return DEFAULT_EDGE_STYLE_ID;
		}
		
	}
}
