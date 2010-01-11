/* *********************************************************************** *
 * project: org.matsim.*
 * TXT2GraphML.java
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
package playground.johannes.socialnetworks.survey.ivt2009;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexDescriptor;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraphBuilder;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseGraph;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialSparseVertex;
import playground.johannes.socialnetworks.snowball2.spatial.io.SampledSpatialGraphMLWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * @author illenberger
 *
 */
public class TXT2GraphML {
	
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TXT2GraphML.class);

	private static final String TAB = "\t";
	
	private static CoordinateTransformation transform = new WGS84toCH1903LV03();
	
	private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 21781);
	
	public static void main(String[] args) throws IOException {
		SampledSpatialGraphBuilder builder = new SampledSpatialGraphBuilder(CRSUtils.getCRS(21781));
		SampledSpatialSparseGraph graph = builder.createGraph();
		/*
		 * read ego table
		 */
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		
		String line = reader.readLine();
		String[] tokens = line.split(TAB);
		int idIdx = getIndex("Laufnr.", tokens);
		int statusIdx = getIndex("Tats�chlicher Teilnahmestatus FB", tokens);
		int longIdx = getIndex("long", tokens);
		int latIdx = getIndex("lat", tokens);
		
		if(idIdx < 0 || longIdx < 0 || latIdx < 0 || statusIdx < 0)
			throw new IllegalArgumentException("Header not found!");
		
		TIntObjectHashMap<SampledSpatialSparseVertex> vertexIds = new TIntObjectHashMap<SampledSpatialSparseVertex>();
		
		int egocount = 0;
		while((line = reader.readLine()) != null) {
			tokens = line.split(TAB);
			String statusStr = tokens[statusIdx];
			if (!statusStr.equalsIgnoreCase("#NV")) {
				int status = Integer.parseInt(statusStr);
				/*
				 * check if ego is sampled
				 */
				if (status == 1) {
					int id = Integer.parseInt(tokens[idIdx]);

					Coord c = getCoord(tokens, longIdx, latIdx);
					if (c != null) {
						SampledSpatialSparseVertex v = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(c.getX(), c.getY())));
						v.sample(0);
						if(vertexIds.put(id, v) != null)
							System.err.println("Overwriting ego with id " + id);
						egocount++;
					}
				}
			}
		}
		logger.info(String.format("Built %1$s egos.", egocount));
		/*
		 * read alter table
		 */
		reader = new BufferedReader(new FileReader(args[1]));
		line = reader.readLine();
		tokens = line.split(TAB);
		idIdx = getIndex("AlterLaufnummer", tokens);
		int egoIdIdx = getIndex("Excel", tokens);
		longIdx = getIndex("long", tokens);
		latIdx = getIndex("lat", tokens);
		
		int altercount = 0;
		int alterDoubled = 0;
		int doubleedges = 0;
//		int alterdropped = 0;
		int nocoord = 0;
		int alterIdCounter = 100000;
		int egoNotFound = 0;
		
		TIntIntHashMap egoDegree = new TIntIntHashMap();
		TIntObjectIterator<SampledSpatialSparseVertex> it = vertexIds.iterator();
		for(int i = 0; i< vertexIds.size(); i++) {
			it.advance();
			egoDegree.put(it.key(), 0);
		}
		
		while((line = reader.readLine()) != null) {
			tokens = line.split(TAB);
			
			String egoIdStr = tokens[egoIdIdx];
			if(!egoIdStr.equalsIgnoreCase("Testzugang")) {
			String[] tokens2 = egoIdStr.split(" ");
//			try {
			int egoId = Integer.parseInt(tokens2[tokens2.length - 1]);
			SampledSpatialSparseVertex ego = vertexIds.get(egoId);
			if(ego == null) {
				logger.warn(String.format("Ego with id %1$s not found!", egoId));
				egoNotFound++;
			} else {
				String idStr = tokens[idIdx];
				if (!idStr.equalsIgnoreCase("#NV")) {// && !idStr.equalsIgnoreCase("")) {
					int id;
					if(idStr.equalsIgnoreCase("") || idStr.equalsIgnoreCase("NICHT ANSCHREIBEN"))
						id = ++alterIdCounter;
					else
						id = Integer.parseInt(idStr);

					SampledSpatialSparseVertex alter = vertexIds.get(id);
					if (alter == null) {
						Coord c = getCoord(tokens, longIdx, latIdx);
						if (c != null) {
							alter = builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(c.getX(), c.getY())));
							if(vertexIds.put(id, alter) != null)
								System.err.println("Overwriting alter with id " + id);
							altercount++;
						} else {
							nocoord++;
						}
					} else {
						alterDoubled++;
					}
					if (alter != null) {
						if(builder.addEdge(graph, ego, alter) == null)
							doubleedges++;
						else
							egoDegree.adjustOrPutValue(egoId, 1, 1);
					}
//					} else {
//						alterdropped++;
//					}
				}
			}
//			} catch (NumberFormatException e) {
//				alterdropped++;
//			}
			}
		}
		
		TIntIntIterator it2 = egoDegree.iterator();
		for(int i = 0; i < egoDegree.size(); i++) {
			it2.advance();
			if(it2.value() == 0) {
				logger.warn(String.format("Ego with id %1$s named no contacts. Marking as not sampled...", it2.key()));
				vertexIds.get(it2.key()).sample(-1);
			}
		}
		
		logger.info(String.format("Built %1$s alters. %2$s alters named at least twice. %3$s doubled edges.", altercount, alterDoubled, doubleedges));
		logger.info(String.format("Dropped %1$s alters because ego not found.", egoNotFound));
		logger.info(String.format("Dropped %1$s alters becuase no coordinates are avaiable.", nocoord));
		/*
		 * Dump graph.
		 */
		SampledSpatialGraphMLWriter writer = new SampledSpatialGraphMLWriter();
		writer.write(graph, args[2]);
		
		KMLWriter kmlwriter = new KMLWriter();
		kmlwriter.setCoordinateTransformation(new CH1903LV03toWGS84());
		kmlwriter.setDrawEdges(false);
		kmlwriter.setVertexStyle(new KMLSnowballVertexStyle(kmlwriter.getVertexIconLink()));
//		writer.setVertexStyle(new KMLDegreeStyle(writer.getVertexIconLink()));
//		writer.setVertexDescriptor(new KMLSnowballDescriptor());
		kmlwriter.setVertexDescriptor(new KMLVertexDescriptor(graph));
		kmlwriter.write(graph, args[2] + ".kmz");
	}
	
	private static int getIndex(String header, String[] tokens) {
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equalsIgnoreCase(header))
				return i;
		}
		
		return -1;
	}
	
	private static Coord getCoord(String[] tokens, int longIdx, int latIdx) {
		try {
		String latStr = tokens[latIdx];
		String longStr = tokens[longIdx];
		if(latStr.length() > 0 && longStr.length() > 0) {
			double latitude = Double.parseDouble(latStr);
			double longitude = Double.parseDouble(longStr);
			
			return transform.transform(new CoordImpl(longitude, latitude));
		} else {
			logger.warn("No coordinates available!");
			return new CoordImpl(0, 0);
		}
		} catch (ArrayIndexOutOfBoundsException e) {
			return new CoordImpl(0, 0);
		}
	}
}
