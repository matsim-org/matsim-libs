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

import gnu.trove.map.hash.TDoubleObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import net.opengis.kml._2.*;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.Colorizable;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.NumericAttributeColorizer;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;



/**
 * @author johannes
 *
 */
public class KMLRailCountsWriter {

	private final ObjectFactory kmlFactory = new ObjectFactory();
	
	public void write(RailCounts simCounts, RailCounts obsCounts, Network network, TransitSchedule schedule, TransitLineAttributes lineAttribs, String file, double scale) {
		
		Set<String> tSystems = new HashSet<String>();
		for(TransitLine line : schedule.getTransitLines().values()) {
			String tSys = lineAttribs.getTransportSystem(line.getId().toString());
			tSystems.add(tSys);
		}
		
		Map<Id, LinkData> linkData = collectData(network, simCounts, obsCounts, tSystems, scale);
		writeKML(linkData, file, network);
	}
	
	private Map<Id, LinkData> collectData(Network network, RailCounts simCounts, RailCounts obsCounts, Set<String> tSystems, double scale) {
		Map<Id, LinkData> linkData = new HashMap<Id, KMLRailCountsWriter.LinkData>();
		
		
		for(Link link : network.getLinks().values()) {
			if(link.getAllowedModes().contains("pt")) {
				LinkData data = new LinkData();
				
				data.simTotal = simCounts.counts(link.getId()) * scale;
				data.obsTotal = obsCounts.counts(link.getId());
				
				Id[] lines = obsCounts.lines(link.getId()); // assumes same lines in sim and obs
				
				if(lines != null) {
				data.lines = new HashMap<String, Tuple<Double,Double>>();
				for(Id line : lines) {
					Tuple<Double, Double> tuple = new Tuple<Double, Double>(simCounts.counts(link.getId(), line) * scale, obsCounts.counts(link.getId(), line));
					data.lines.put(line.toString(), tuple);
				}
				}
				
				data.tSystems = new HashMap<String, Tuple<Double,Double>>();
				for(String tSys : tSystems) {
					Tuple<Double, Double> tuple = new Tuple<Double, Double>(simCounts.counts(link.getId(), tSys) * scale, obsCounts.counts(link.getId(), tSys));
					data.tSystems.put(tSys, tuple);
				}
				
				linkData.put(link.getId(), data);
			}
		}
		
		return linkData;
		
	}
	
	private void writeKML(Map<Id, LinkData> linkData, String file, Network network) {
		KMZWriter writer = new KMZWriter(file);
		DocumentType kmlDocument = kmlFactory.createDocumentType();

		MathTransform transform = null;
		CoordinateReferenceSystem sourceCRS = CRSUtils.getCRS(31467);
		CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
		try {
			transform = CRS.findMathTransform(sourceCRS, targetCRS);
		} catch (FactoryException e) {
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

		TObjectDoubleHashMap<Id> values = new TObjectDoubleHashMap<Id>(linkData.size());
		double valueArray[] = new double[linkData.size()];
		int i = 0;
		for (Entry<Id, LinkData> entry : linkData.entrySet()) {
			double diff = entry.getValue().obsTotal - entry.getValue().simTotal;
			valueArray[i] = Math.abs(diff);
			values.put(entry.getKey(), Math.abs(diff));
			i++;
		}

		LinearDiscretizer discretizer = new LinearDiscretizer(valueArray, 20);
		Colorizable colorizable = new Colorizer(values);

		TDoubleObjectHashMap<StyleType> tmpStyles = new TDoubleObjectHashMap<StyleType>();
		for (Object obj : values.keys()) {
			Id linkId = (Id) obj;
			double diff = values.get(linkId);
			double val = discretizer.discretize(diff);
			double bin = discretizer.index(diff);

			StyleType kmlStyle = tmpStyles.get(val);
			if (kmlStyle == null) {
				Color c = colorizable.getColor(linkId);

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
			
			styleMap.put(linkId, kmlStyle);
		}
		
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
		i = 0;
		for (Entry<Id, LinkData> entry : linkData.entrySet()) {
			Id linkId = entry.getKey();
			Link link = network.getLinks().get(linkId);
//			if(link.getLength() > 1000) {
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
			kmlLineString.getCoordinates().add(makeCoordinateString(points[0], points[1]));
			kmlLineString.getCoordinates().add(makeCoordinateString(points[2], points[3]));

			PlacemarkType kmlPlacemark = kmlFactory.createPlacemarkType();
			kmlPlacemark.setAbstractGeometryGroup(kmlFactory.createLineString(kmlLineString));
			kmlPlacemark.setStyleUrl(styleMap.get(linkId).getId());
			kmlPlacemark.setDescription(getDescription(entry.getValue()));

			kmlFolder.getAbstractFeatureGroup().add(kmlFactory.createPlacemark(kmlPlacemark));
			
//			}

		}

		
		kmlDocument.getAbstractFeatureGroup().add(kmlFactory.createFolder(kmlFolder));
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
	
	private String getDescription(LinkData data) {
		StringBuilder builder = new StringBuilder(500);
		
		builder.append("\tsim\tobs\n");
		builder.append("total\t");
		builder.append(String.valueOf(data.simTotal));
		builder.append("\t");
		builder.append(String.valueOf(data.obsTotal));
		builder.append("\n");
		
		for(Entry<String, Tuple<Double, Double>> entry : data.tSystems.entrySet()) {
			builder.append(entry.getKey());
			builder.append("\t");
			builder.append(String.valueOf(entry.getValue().getFirst()));
			builder.append("\t");
			builder.append(String.valueOf(entry.getValue().getSecond()));
			builder.append("\n");
		}
		
		if(data.lines != null) {
		for(Entry<String, Tuple<Double, Double>> entry : data.lines.entrySet()) {
			builder.append(entry.getKey());
			builder.append("\t");
			builder.append(String.valueOf(entry.getValue().getFirst()));
			builder.append("\t");
			builder.append(String.valueOf(entry.getValue().getSecond()));
			builder.append("\n");
		}
		}
		return builder.toString();
	}
	
	private String makeCoordinateString(double x, double y) {
		StringBuffer buffer = new StringBuffer(50);
		buffer.append(Double.toString(x));
		buffer.append(",");
		buffer.append(Double.toString(y));
		
		return buffer.toString();
	}
	
	private static class LinkData {
		
		private double simTotal;
	
		private double obsTotal;
		
		private Map<String, Tuple<Double, Double>> lines;
		
		private Map<String, Tuple<Double, Double>> tSystems;
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
