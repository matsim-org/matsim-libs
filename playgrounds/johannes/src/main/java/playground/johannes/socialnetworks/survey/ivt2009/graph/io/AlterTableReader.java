/* *********************************************************************** *
 * project: org.matsim.*
 * AlterTableReader.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author illenberger
 *
 */
public class AlterTableReader {
	
	private static final Logger logger = Logger.getLogger(AlterTableReader.class);

	private Map<String, VertexRecord> vertexList;
	
	private List<Tuple<VertexRecord, VertexRecord>> edgeList;
	
	private int dummyIdCounter = 1000000;
	
	public class VertexRecord {
		
		public String id;
		
		public String egoSQLId;
		
		public String alterKey;
		
		public boolean isEgo() {
			if(alterKey == null)
				return true;
			else
				return false;
		}
	}
	
	public AlterTableReader(List<String> files) throws IOException {
		vertexList = new HashMap<String, VertexRecord>();
		edgeList = new LinkedList<Tuple<VertexRecord,VertexRecord>>();
		
		for(String file : files) {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null) {
				if(!ignoreLine(line)) {
				String[] tokens = line.split("\t");
				String egoSQLId = tokens[0];
				String egoId = tokens[1].substring(tokens[1].lastIndexOf(" ") + 1);
								
				String alterKey = tokens[2];
				
				String alterId;
				if(tokens.length < 11)
					alterId = createDummyId();
				else if(tokens[10].equalsIgnoreCase(""))
					alterId = createDummyId();
				else if(tokens[10].equalsIgnoreCase("NICHT ANSCHREIBEN"))
					alterId = createDummyId();
				else
					alterId = tokens[10];
				/*
				 * find ego
				 */
				VertexRecord ego = vertexList.get(egoId);
				if(ego == null) {
					ego = new VertexRecord();
					ego.id = egoId;
					ego.egoSQLId = egoSQLId;
					
					vertexList.put(egoId, ego);
				} else {
					/*
					 * overwrite old values in case an alter turned into an ego;
					 */
					if(!ego.isEgo())
						logger.info(String.format("Alter %1$s turned into ego.", egoId));
					
					ego.id = egoId;
					ego.egoSQLId = egoSQLId;
					ego.alterKey = null;
				}
				/*
				 * find alter
				 */
				VertexRecord alter = vertexList.get(alterId);
				
				if(alter == null) {
					alter = new VertexRecord();
					alter.id = alterId;
					alter.egoSQLId = ego.egoSQLId;
					alter.alterKey = alterKey;
					
					vertexList.put(alterId, alter);
				}
				/*
				 * add edge
				 */
				Tuple<VertexRecord, VertexRecord> edge = new Tuple<VertexRecord, VertexRecord>(ego, alter);
				edgeList.add(edge);
			}
			}
		}
		
		int numEgos = 0;
		int numAlters = 0;
		for(Entry<String, VertexRecord> record : vertexList.entrySet()) {
			if(record.getValue().isEgo())
				numEgos++;
			else
				numAlters++;
		}
		logger.info(String.format("Parsed %1$s egos and %2$s alters.", numEgos, numAlters));
	}
	
	private boolean ignoreLine(String line) {
		if(line.indexOf("Testzugang") > -1)
			return true;
		else
			return false;
	}
	private String createDummyId(){
		dummyIdCounter++;
		return String.valueOf(dummyIdCounter);
		
	}
	public Map<String, VertexRecord> getVertices() {
		return vertexList;
	}
	
	public List<Tuple<VertexRecord, VertexRecord>> getEdges() {
		return edgeList;
	}
}
