/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.P2.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.utils.io.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class CoopLogger2Shp {
	
	private static final Logger log = Logger.getLogger(CoopLogger2Shp.class);
	
	private Network net;
	private String coopLoggerFile;
	private int shpEveryIter;
	private String outDir;
	private Map<Id, Integer> coopOnLinkCnt;
	
	private static final String DIR = "D:/VSP/net/ils/roeder/11x6/";
	private static final String NETFILE = DIR + "network.xml.gz";
	private static final String COOPLOGGERFILE = DIR + "DebugReplanning/startEnd.pCoopLogger.txt";
	private static final String OUTDIR = DIR + "DebugReplanning/startEnd.pCoopLogger.shp/";
	
	public static void main(String[] args){
		CoopLogger2Shp c2shp = new CoopLogger2Shp(NETFILE, COOPLOGGERFILE, 100, OUTDIR);
		c2shp.run();
	}
	public CoopLogger2Shp(String netFile, String coopLoggerFile, int shpEveryXXiter, String outDir){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(netFile);
		this.net = sc.getNetwork();
		this.coopLoggerFile = coopLoggerFile;
		this.shpEveryIter = shpEveryXXiter;
		this.outDir = outDir;
		if(!(new File(outDir).exists())){
			new File(outDir).mkdirs();
		}
	}
	
	private Map<String, SortedMap<Integer, Coord>> lineStrings = null;
	private Map<String, SortedMap<String, Object>> attribs = null;
	public void run(){
		
		DaShapeWriter.writeLinks2Shape(this.outDir + "links.shp", this.net.getLinks(), null);
		DaShapeWriter.writeNodes2Shape(this.outDir + "nodes.shp", this.net.getNodes());
		int currentIteration = 0;
		
		String line;
		
		try {
			log.info("start reading content of " + this.coopLoggerFile);
			BufferedReader reader = IOUtils.getBufferedReader(this.coopLoggerFile);
//			// first line is the header
			reader.readLine();
			line = reader.readLine();
			Integer iteration = null;
//			do{
//				if(!(line == null)){
//					String[] columns = line.split("\t");
//					iteration = Integer.valueOf(columns[0]);
//					if(iteration % this.shpEveryIter == 0){
//						if(lineStrings == null){
//							currentIteration = iteration;
//						}
//						if(iteration == currentIteration){
//							writeLine(columns, currentIteration);
//						}
//						
//					}
//				}
//				line = reader.readLine();
//			}while(!(line == null));
			//----
			do{
				if(!(line == null)){
					String[] columns = line.split("\t");
					iteration = Integer.valueOf(columns[0]);
					if(iteration % this.shpEveryIter == 0){
						if(lineStrings == null){
							currentIteration = iteration;
							this.offset = 0.5;
							lineStrings = new HashMap<String, SortedMap<Integer,Coord>>();
							attribs = new HashMap<String, SortedMap<String,Object>>();
						}
						if(iteration == currentIteration){
							addLine(columns, lineStrings, attribs);
						}else{
							DaShapeWriter.writeDefaultLineString2Shape(this.outDir + "it." + currentIteration + ".shp", "it." + currentIteration, lineStrings, attribs);
							lineStrings = null;
							attribs = null;
							this.coopOnLinkCnt = null;
						}
					}else if(lineStrings!=null){
						DaShapeWriter.writeDefaultLineString2Shape(this.outDir + "it." + currentIteration + ".shp", "it." + currentIteration, lineStrings, attribs);
						lineStrings = null;
						attribs = null;
						this.coopOnLinkCnt = null;
					}
					
					line = reader.readLine();
				}
			}while(!(line == null));
			DaShapeWriter.writeDefaultLineString2Shape(this.outDir + "it." + currentIteration + ".shp", "it." + currentIteration, lineStrings, attribs);
			reader.close();
			log.info("finished...");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param columns
	 * @param currentIteration
	 */
	private void writeLine(String[] columns, int currentIteration) {
		Map<String, SortedMap<Integer, Coord>> lineStrings = new HashMap<String, SortedMap<Integer,Coord>>();
		Map<String, SortedMap<String, Object>> attribs = new HashMap<String, SortedMap<String,Object>>();

		String prefix = columns[1] + "_";
		String l =  columns[9].replace("[", "");
		l = l.replace("]", "");
		String[] links = l.split(", ");
		
		SortedMap<String, Object> tempAttrib;
		tempAttrib = new TreeMap<String, Object>();
		tempAttrib.put("iter", columns[0]);
		tempAttrib.put("coop", columns[1]);
		tempAttrib.put("veh", columns[2]);
		tempAttrib.put("pax", columns[3]);
		tempAttrib.put("score", columns[4]);
		tempAttrib.put("budget", columns[5]);
		tempAttrib.put("start", columns[6]);
		tempAttrib.put("end", columns[7]);
		tempAttrib.put("stopsToBeServed", columns[8]);
		tempAttrib.put("usedLinks", columns[9]);
		
		Id id;
		Link link;
		int i = 0;
		SortedMap<Integer, Coord> tempString;
		
		for(String ll : links){
			tempString = new TreeMap<Integer, Coord>();
			id = new IdImpl(ll);
			link = this.net.getLinks().get(id);
			tempString.put(0, link.getFromNode().getCoord());
			tempString.put(1, link.getToNode().getCoord());
			lineStrings.put(prefix + i, tempString);
			attribs.put(prefix + i, tempAttrib);
			i++;
			this.offset += 0.5;
		}
		
		File f = new File(this.outDir + "it." + currentIteration);
		if(!f.exists()){
			f.mkdirs();
		}
		DaShapeWriter.writeDefaultLineString2Shape(f.getAbsolutePath() + "/" + prefix + currentIteration + ".shp", prefix + "it." + currentIteration, lineStrings, attribs);
		
	}
	private void write(int currentIteration){
		DaShapeWriter.writeDefaultLineString2Shape(this.outDir + "it." + currentIteration + "/it." + currentIteration + ".shp", 
				"it." + currentIteration, lineStrings, attribs);
		lineStrings = null;
		attribs = null;
		this.coopOnLinkCnt = null;
	}

	private double offset;
	/**
	 * @param columns
	 * @param lineStrings
	 * @param attribs
	 */
	private void addLine(String[] columns,
			Map<String, SortedMap<Integer, Coord>> lineStrings,
			Map<String, SortedMap<String, Object>> attribs) {
		this.coopOnLinkCnt = new HashMap<Id, Integer>();
		
		String prefix = columns[1] + "_";
		String l =  columns[9].replace("[", "");
		l = l.replace("]", "");
		String[] links = l.split(", ");
		
		SortedMap<String, Object> tempAttrib;
		tempAttrib = new TreeMap<String, Object>();
		tempAttrib.put("iter", columns[0]);
		tempAttrib.put("coop", columns[1]);
		tempAttrib.put("veh", columns[2]);
		tempAttrib.put("pax", columns[3]);
		tempAttrib.put("score", columns[4]);
		tempAttrib.put("budget", columns[5]);
		tempAttrib.put("start", columns[6]);
		tempAttrib.put("end", columns[7]);
		tempAttrib.put("stopsToBeServed", columns[8]);
		
		Id id;
		Link link;
		int i = 0;
		SortedMap<Integer, Coord> tempString;
		Coord offset;
		
		for(String ll : links){
			tempString = new TreeMap<Integer, Coord>();
			id = new IdImpl(ll);
			link = this.net.getLinks().get(id);
			offset = CoordUtils.minus(
					link.getToNode().getCoord(), 
					link.getFromNode().getCoord());
			offset = CoordUtils.rotateToRight(offset);
			offset = CoordUtils.scalarMult(this.offset/CoordUtils.length(offset), offset);
			tempString.put(0, CoordUtils.plus(offset, link.getFromNode().getCoord()));
			tempString.put(1, CoordUtils.plus(offset, link.getToNode().getCoord()));
			lineStrings.put(prefix + i, tempString);
			attribs.put(prefix + i, tempAttrib);
			i++;
			this.offset += 0.5;
		}
	}

}
