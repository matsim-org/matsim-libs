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

import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialGraph;
import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialGraphMLWriter;
import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialVertex;
import playground.wrashid.tryouts.performance.SatawalLockTest1;
import samples.preview_new_graphdraw.impl.GraphLayoutPanelMouseListener.NoEventPolicy;

/**
 * @author illenberger
 *
 */
public class TXT2GraphML {
	
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TXT2GraphML.class);

	private static final String TAB = "\t";
	
	private static CoordinateTransformation transform = new WGS84toCH1903LV03();
	
	public static void main(String[] args) throws IOException {
		SampledSpatialGraphBuilder builder = new SampledSpatialGraphBuilder();
		SampledSpatialGraph graph = builder.createGraph();
		/*
		 * read ego table
		 */
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		
		String line = reader.readLine();
		String[] tokens = line.split(TAB);
		int idIdx = getIndex("Laufnr.", tokens);
		int statusIdx = getIndex("Tatsächlicher Teilnahmestatus FB", tokens);
		int longIdx = getIndex("long", tokens);
		int latIdx = getIndex("lat", tokens);
		
		if(idIdx < 0 || longIdx < 0 || latIdx < 0 || statusIdx < 0)
			throw new IllegalArgumentException("Header not found!");
		
		TIntObjectHashMap<SampledSpatialVertex> vertexIds = new TIntObjectHashMap<SampledSpatialVertex>();
		
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
						SampledSpatialVertex v = builder.addVertex(graph, c);
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
		while((line = reader.readLine()) != null) {
			tokens = line.split(TAB);
			
			String egoIdStr = tokens[egoIdIdx];
			if(!egoIdStr.equalsIgnoreCase("Testzugang")) {
			String[] tokens2 = egoIdStr.split(" ");
//			try {
			int egoId = Integer.parseInt(tokens2[tokens2.length - 1]);
			SampledSpatialVertex ego = vertexIds.get(egoId);
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

					SampledSpatialVertex alter = vertexIds.get(id);
					if (alter == null) {
						Coord c = getCoord(tokens, longIdx, latIdx);
						if (c != null) {
							alter = builder.addVertex(graph, c);
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
		logger.info(String.format("Built %1$s alters. %2$s alters named at least twice. %3$s doubled edges.", altercount, alterDoubled, doubleedges));
		logger.info(String.format("Dropped %1$s alters because ego not found.", egoNotFound));
		logger.info(String.format("Dropped %1$s alters becuase no coordinates are avaiable.", nocoord));
		/*
		 * Dump graph.
		 */
		SampledSpatialGraphMLWriter writer = new SampledSpatialGraphMLWriter();
		writer.write(graph, args[2]);
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
			return null;
		}
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
}
