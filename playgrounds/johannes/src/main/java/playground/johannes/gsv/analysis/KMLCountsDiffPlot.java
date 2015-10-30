/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.analysis;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import net.opengis.kml._2.*;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.Colorizable;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.NumericAttributeColorizer;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class KMLCountsDiffPlot {

	private final ObjectFactory kmlFactory = new ObjectFactory();

	public void write(VolumesAnalyzer analyzer,
			TObjectDoubleHashMap<Link> counts, double factor, String file, Network network) {

		TObjectDoubleHashMap<Link> values = calcValues(network, analyzer, counts, factor);
		writeKML(values, file, network);
	}

	private TObjectDoubleHashMap<Link> calcValues(Network network,
			VolumesAnalyzer analyzer, TObjectDoubleHashMap<Link> counts,
			double factor) {
		TObjectDoubleHashMap<Link> diffValues = new TObjectDoubleHashMap<Link>();

		for (Link link : network.getLinks().values()) {
			if(link.getId().toString().contains("rail.")) {
			int[] tmp = analyzer.getVolumesForLink(link.getId());
			int sim = 0;
			if (tmp != null)
				sim = tmp[0];

			double obs = counts.get(link);

			sim *= factor;

			double diff = obs - sim;

			diffValues.put(link, diff);
			}
		}

		return diffValues;
	}

	private void writeKML(TObjectDoubleHashMap<Link> values, String file,
			Network network) {

		/*
		 * Open a KMZ writer.
		 */
		KMZWriter writer = new KMZWriter(file);
		// for (KMZWriterListener listener : writerListeners)
		// listener.openWriter(writer);
		/*
		 * Find the coordinate transformation.
		 */

		/*
		 * Create a KML document.
		 */
		DocumentType kmlDocument = kmlFactory.createDocumentType();
		/*
		 * Write each partition in a separate folder.
		 */
		// for (Set<? extends SpatialVertex> partition :
		// kmlPartitition.getPartitions(graph)) {
		FolderType kmlFolder = createFolder(network, values, kmlDocument);
		// kmlPartitition.addDetail(kmlFolder, partition);
		kmlDocument.getAbstractFeatureGroup().add(
				kmlFactory.createFolder(kmlFolder));
		// }
		/*
		 * Write the KML document.
		 */
		KmlType kmlType = kmlFactory.createKmlType();
		kmlType.setAbstractFeatureGroup(kmlFactory.createDocument(kmlDocument));
		writer.writeMainKml(kmlType);

		/*
		 * Close the KMZ writer.
		 */
		// for (KMZWriterListener listener : writerListeners)
		// listener.closeWriter(writer);

		writer.close();
	}

	private FolderType createFolder(Network network,
			TObjectDoubleHashMap<Link> values, DocumentType kmlDocument) {
		MathTransform transform = null;
		CoordinateReferenceSystem sourceCRS = CRSUtils.getCRS(31467);
		CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
		try {
			transform = CRS.findMathTransform(sourceCRS, targetCRS);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * Create a folder.
		 */
		FolderType kmlFolder = kmlFactory.createFolderType();
		/*
		 * Get all style types that will be used in this folder.
		 */
		Map<Object, StyleType> styleMap = new HashMap<Object, StyleType>();
		Set<StyleType> styleSet = new HashSet<StyleType>();

		double valueArray[] = values.getValues();
		for (int i = 0; i < valueArray.length; i++) {
			valueArray[i] = Math.abs(valueArray[i]);
		}

		LinearDiscretizer discretizer = new LinearDiscretizer(valueArray, 20);
		Colorizable colorizable = new Colorizer(values);

		TDoubleObjectHashMap<StyleType> tmpStyles = new TDoubleObjectHashMap<StyleType>();
		for (Object obj : values.keys()) {
			Link link = (Link) obj;
			double diff = values.get(link);
			double val = discretizer.discretize(diff);
			double bin = discretizer.index(diff);

			StyleType kmlStyle = tmpStyles.get(val);
			if (kmlStyle == null) {
				Color c = colorizable.getColor(link);

				LineStyleType kmlLineStyle = kmlFactory.createLineStyleType();
				kmlLineStyle.setColor(new byte[] { (byte) c.getAlpha(),
						(byte) c.getBlue(), (byte) c.getGreen(),
						(byte) c.getRed() });
				kmlLineStyle.setWidth(bin);

				kmlStyle = kmlFactory.createStyleType();
				kmlStyle.setId("link." + val);
				kmlStyle.setLineStyle(kmlLineStyle);

				styleSet.add(kmlStyle);
				tmpStyles.put(val, kmlStyle);
			}
			
			styleMap.put(link, kmlStyle);
		}
		// StyleType kmlStyle = kmlVertexStyle.getStyle(v);
		// styleSet.add(kmlStyle);
		// styleMap.put(v, kmlStyle);
		//
		// for (SpatialEdge e : v.getEdges()) {
		// kmlStyle = kmlEdgeStyle.getStyle(e);
		// styleSet.add(kmlStyle);
		// styleMap.put(e, kmlStyle);
		// }
		// }
		/*
		 * Add all style types to the selector group.
		 */
		for (StyleType kmlStyle : styleSet) {
			kmlFolder.getAbstractStyleSelectorGroup().add(
					kmlFactory.createStyle(kmlStyle));
		}
		/*
		 * Add vertices and edges.
		 */
		int i = 0;
		for (Object obj : values.keys()) {
			Link link = (Link) obj;
			if(link.getLength() > 1000) {
			LineStringType kmlLineString = kmlFactory.createLineStringType();

			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			double[] points = new double[] { fromNode.getCoord().getX(),
					fromNode.getCoord().getY(), toNode.getCoord().getX(),
					toNode.getCoord().getY() };

			try {
				transform.transform(points, 0, points, 0, 2);
			} catch (TransformException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			kmlLineString.getCoordinates().add(
					makeCoordinateString(points[0], points[1]));
			kmlLineString.getCoordinates().add(
					makeCoordinateString(points[2], points[3]));

			PlacemarkType kmlPlacemark = kmlFactory.createPlacemarkType();
			kmlPlacemark.setAbstractGeometryGroup(kmlFactory
					.createLineString(kmlLineString));
			kmlPlacemark.setStyleUrl(styleMap.get(link).getId());
			// kmlPlacemark.setDescription(String.format(format, args));

			kmlFolder.getAbstractFeatureGroup().add(
					kmlFactory.createPlacemark(kmlPlacemark));
			
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

	private static class Colorizer extends NumericAttributeColorizer {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.matsim.contrib.socnetgen.socialnetworks.graph.spatial.io.
		 * NumericAttributeColorizer#getValue(java.lang.Object)
		 */
		@Override
		protected double getValue(Object object) {
			return Math.abs(super.getValue(object));
		}

		public Colorizer(TObjectDoubleHashMap<?> values) {
			super(values);
		}

	}
}
