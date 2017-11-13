/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphKMLWriter.java
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.io;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import net.opengis.kml.v_2_2_0.DocumentType;
import net.opengis.kml.v_2_2_0.FolderType;
import net.opengis.kml.v_2_2_0.KmlType;
import net.opengis.kml.v_2_2_0.LineStringType;
import net.opengis.kml.v_2_2_0.LineStyleType;
import net.opengis.kml.v_2_2_0.ObjectFactory;
import net.opengis.kml.v_2_2_0.PlacemarkType;
import net.opengis.kml.v_2_2_0.PointType;
import net.opengis.kml.v_2_2_0.StyleType;

/**
 * A class to encode spatial graphs into a KML format. The encode graph will be
 * packed with additional resources into a KMZ file.
 * 
 * @author jillenberger
 * 
 */
public class SpatialGraphKMLWriter {

	private final ObjectFactory kmlFactory = new ObjectFactory();

	private MathTransform transform;

	private List<KMZWriterListener> writerListeners = new ArrayList<KMZWriterListener>();

	private KMLPartitions kmlPartitition;

	private boolean drawVertices = true;

	private boolean drawEdges = true;

	private KMLObjectStyle kmlVertexStyle;

	private KMLObjectStyle kmlEdgeStyle;

	private KMLObjectDetail kmlVertexDetail;

	private KMLObjectDetail kmlEdgeDetail;

	/**
	 * Adds a KMZWriterListener to the writer. Listeners will be called
	 * insertion-ordered.
	 * 
	 * @param listerner
	 *            a KMZWriterListener
	 */
	public void addKMZWriterListener(KMZWriterListener listener) {
		writerListeners.add(listener);
	}

	/**
	 * Returns the KMLPartitions object used with this writer.
	 * 
	 * @return the KMLPartitions object
	 */
	public KMLPartitions getKmlPartitition() {
		return kmlPartitition;
	}

	/**
	 * Sets the KMLPartitions object to use with this writer. The writer will
	 * create one folder for each partition in the main KML document. The
	 * default KMLPartitions object returns the whole graph as one partition.
	 * 
	 * @param kmlPartitition
	 *            a KMLParatitions object
	 */
	public void setKmlPartitition(KMLPartitions kmlPartitition) {
		this.kmlPartitition = kmlPartitition;
	}

	/**
	 * Returns if this writer draws vertices. The default is <tt>true</tt>.
	 * 
	 * @return <tt>true</tt> if this writer draws vertices, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isDrawVertices() {
		return drawVertices;
	}

	/**
	 * Sets whether this writer draws vertices.
	 * 
	 * @param drawVertices
	 *            <tt>true</tt> if this writer should draw vertices,
	 *            <tt>false</tt> otherwise.
	 */
	public void setDrawVertices(boolean drawVertices) {
		this.drawVertices = drawVertices;
	}

	/**
	 * Returns if this writer draws edges. The default is <tt>true</tt>.
	 * 
	 * @return <tt>true</tt> if this writer draws edges, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean isDrawEdges() {
		return drawEdges;
	}

	/**
	 * Sets whether this writer draws edges.
	 * 
	 * @param drawVertices
	 *            <tt>true</tt> if this writer should draw edges, <tt>false</tt>
	 *            otherwise.
	 */
	public void setDrawEdges(boolean drawEdges) {
		this.drawEdges = drawEdges;
	}

	/**
	 * Returns the style object that is used to draw vertices. The default is
	 * {@link KMLIconVertexStyle}.
	 * 
	 * @return the style object that is used to draw vertices.
	 */
	public KMLObjectStyle getKmlVertexStyle() {
		return kmlVertexStyle;
	}

	/**
	 * Sets the style object that is used to draw vertices.
	 * 
	 * @param kmlVertexStyle
	 *            a style object
	 */
	public void setKmlVertexStyle(KMLObjectStyle kmlVertexStyle) {
		this.kmlVertexStyle = kmlVertexStyle;
	}

	/**
	 * Returns the style object that is used to draw edges. The default object
	 * draws edges as white line strings.
	 * 
	 * @return the style object that is used to draw edges.
	 */
	public KMLObjectStyle getKmlEdgeStyle() {
		return kmlEdgeStyle;
	}

	/**
	 * Sets the style object that is used to draw edges.
	 * 
	 * @param kmlEdgeStyle
	 *            a style object
	 */
	public void setKmlEdgeStyle(KMLObjectStyle kmlEdgeStyle) {
		this.kmlEdgeStyle = kmlEdgeStyle;
	}

	/**
	 * Returns the object that appends further attributes to the placemark
	 * representing a vertex.
	 * 
	 * @return the object that appends further attributes to the placemark
	 *         representing a vertex.
	 */
	public KMLObjectDetail getKmlVertexDetail() {
		return kmlVertexDetail;
	}

	/**
	 * Sets the object that appends further attributes to the placemark
	 * representing a vertex. The default is <tt>null</tt>.
	 * 
	 * @param kmlVertexDetail
	 *            KMLObjectDetail object, or <tt>null</tt> if no further
	 *            attributes should be added to the placemark.
	 */
	public void setKmlVertexDetail(KMLObjectDetail kmlVertexDetail) {
		this.kmlVertexDetail = kmlVertexDetail;
	}

	/**
	 * Returns the object that appends further attributes to the placemark
	 * representing an edge.
	 * 
	 * @return the object that appends further attributes to the placemark
	 *         representing an edge.
	 */
	public KMLObjectDetail getKmlEdgeDetail() {
		return kmlEdgeDetail;
	}

	/**
	 * Sets the object that appends further attributes to the placemark
	 * representing an edge. The default is <tt>null</tt>.
	 * 
	 * @param kmlEdgeDetail
	 *            KMLObjectDetail object, or <tt>null</tt> if no further
	 *            attributes should be added to the placemark.
	 */
	public void setKmlEdgeDetail(KMLObjectDetail kmlEdgeDetail) {
		this.kmlEdgeDetail = kmlEdgeDetail;
	}

	/**
	 * Writes a spatial graph into a KMZ file.
	 * 
	 * @param graph
	 *            a spatial graph
	 * @param filename
	 *            the path to the KMZ file
	 */
	public void write(SpatialGraph graph, String filename) {
		if (kmlPartitition == null)
			kmlPartitition = new DefaultKMLPartition();
		if (kmlVertexStyle == null) {
			kmlVertexStyle = new KMLIconVertexStyle(graph);
			this.addKMZWriterListener((KMZWriterListener) kmlVertexStyle);
		}
		if (kmlEdgeStyle == null)
			kmlEdgeStyle = new DefaultEdgeStyle();
		/*
		 * Open a KMZ writer.
		 */
		KMZWriter writer = new KMZWriter(filename);
		for (KMZWriterListener listener : writerListeners)
			listener.openWriter(writer);
		/*
		 * Find the coordinate transformation.
		 */
		CoordinateReferenceSystem sourceCRS = graph.getCoordinateReferenceSysten();
		CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
		try {
			transform = CRS.findMathTransform(sourceCRS, targetCRS);
			/*
			 * Create a KML document.
			 */
			DocumentType kmlDocument = kmlFactory.createDocumentType();
			/*
			 * Write each partition in a separate folder.
			 */
			for (Set<? extends SpatialVertex> partition : kmlPartitition.getPartitions(graph)) {
				FolderType kmlFolder = createFolder(partition, kmlDocument);
				kmlPartitition.addDetail(kmlFolder, partition);
				kmlDocument.getAbstractFeatureGroup().add(kmlFactory.createFolder(kmlFolder));
			}
			/*
			 * Write the KML document.
			 */
			KmlType kmlType = kmlFactory.createKmlType();
			kmlType.setAbstractFeatureGroup(kmlFactory.createDocument(kmlDocument));
			writer.writeMainKml(kmlType);
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		/*
		 * Close the KMZ writer.
		 */
		for (KMZWriterListener listener : writerListeners)
			listener.closeWriter(writer);

		writer.close();
	}

	private FolderType createFolder(Set<? extends SpatialVertex> vertices, DocumentType kmlDocument) {
		/*
		 * Create a folder.
		 */
		FolderType kmlFolder = kmlFactory.createFolderType();
		/*
		 * Get all style types that will be used in this folder.
		 */
		Map<Object, StyleType> styleMap = new HashMap<Object, StyleType>();
		Set<StyleType> styleSet = new HashSet<StyleType>();

		for (SpatialVertex v : vertices) {
			StyleType kmlStyle = kmlVertexStyle.getStyle(v);
			styleSet.add(kmlStyle);
			styleMap.put(v, kmlStyle);

			for (SpatialEdge e : v.getEdges()) {
				kmlStyle = kmlEdgeStyle.getStyle(e);
				styleSet.add(kmlStyle);
				styleMap.put(e, kmlStyle);
			}
		}
		/*
		 * Add all style types to the selector group.
		 */
		for (StyleType kmlStyle : styleSet) {
			kmlFolder.getAbstractStyleSelectorGroup().add(kmlFactory.createStyle(kmlStyle));
		}
		/*
		 * Add vertices and edges.
		 */
		Set<SpatialEdge> processedEdges = new HashSet<SpatialEdge>();

		for (SpatialVertex v : vertices) {
			if(v.getPoint() != null) {
			try {
				if (drawVertices) {
					/*
					 * Encode the vertex.
					 */
					double[] points = new double[] { v.getPoint().getCoordinate().x, v.getPoint().getCoordinate().y };
					transform.transform(points, 0, points, 0, 1);
					PointType kmlPoint = kmlFactory.createPointType();
					kmlPoint.getCoordinates().add(makeCoordinateString(points[0], points[1]));

					PlacemarkType kmlPlacemark = kmlFactory.createPlacemarkType();
					kmlPlacemark.setAbstractGeometryGroup(kmlFactory.createPoint(kmlPoint));
					kmlPlacemark.setStyleUrl(styleMap.get(v).getId());
					if (kmlVertexDetail != null)
						kmlVertexDetail.addDetail(kmlPlacemark, v);

					kmlFolder.getAbstractFeatureGroup().add(kmlFactory.createPlacemark(kmlPlacemark));
				}
				/*
				 * Encode the edges.
				 */
				for (SpatialEdge e : v.getEdges()) {
					if (drawEdges) {
						if (processedEdges.add(e)) {
							LineStringType kmlLineString = kmlFactory.createLineStringType();
							SpatialVertex v2 = e.getOpposite(v);
							if(v2.getPoint() != null) {
							double[] points = new double[] { v.getPoint().getCoordinate().x,
									v.getPoint().getCoordinate().y, v2.getPoint().getCoordinate().x,
									v2.getPoint().getCoordinate().y };
							transform.transform(points, 0, points, 0, 2);
							kmlLineString.getCoordinates().add(makeCoordinateString(points[0], points[1]));
							kmlLineString.getCoordinates().add(makeCoordinateString(points[2], points[3]));

							PlacemarkType kmlPlacemark = kmlFactory.createPlacemarkType();
							kmlPlacemark.setAbstractGeometryGroup(kmlFactory.createLineString(kmlLineString));
							kmlPlacemark.setStyleUrl(styleMap.get(e).getId());
							if (kmlEdgeDetail != null)
								kmlEdgeDetail.addDetail(kmlPlacemark, e);

							kmlFolder.getAbstractFeatureGroup().add(kmlFactory.createPlacemark(kmlPlacemark));
							}
						}
					}
				}
			} catch (TransformException e) {
				e.printStackTrace();
			}
			}
		}

		return kmlFolder;
	}

	private String makeCoordinateString(double x, double y) {
		StringBuffer buffer = new StringBuffer(50);
		buffer.append(Double.toString(x));
		buffer.append(",");
		buffer.append(Double.toString(y));
		return buffer.toString();
	}

	private static class DefaultKMLPartition implements KMLPartitions {

		private SpatialGraph graph;

		@Override
		public void addDetail(FolderType kmlFolder, Set<? extends SpatialVertex> partition) {
			kmlFolder.setName("Graph");
			kmlFolder.setDescription(String.format("%1$s vertices, %2$s edges", graph.getVertices().size(), graph
					.getEdges().size()));
		}

		@Override
		public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
			this.graph = graph;
			List<Set<? extends SpatialVertex>> partitions = new ArrayList<Set<? extends SpatialVertex>>(1);
			partitions.add(graph.getVertices());
			return partitions;
		}

	}

	private class DefaultEdgeStyle implements KMLObjectStyle {

		private final static String STYLE_ID = "defaultEdgeStyle";

		private final StyleType kmlStyle;

		public DefaultEdgeStyle() {
			LineStyleType kmlLineStyle = kmlFactory.createLineStyleType();
			Color c = Color.WHITE;
			kmlLineStyle.setColor(new byte[] { (byte) c.getAlpha(), (byte) c.getBlue(), (byte) c.getGreen(),
					(byte) c.getRed() });
			kmlLineStyle.setWidth(1.0);

			kmlStyle = kmlFactory.createStyleType();
			kmlStyle.setId(STYLE_ID);
			kmlStyle.setLineStyle(kmlLineStyle);
		}

		@Override
		public StyleType getStyle(Object object) {
			return kmlStyle;
		}

	}

}
