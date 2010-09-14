/* *********************************************************************** *
 * project: org.matsim.*
 * GeneratePathSets.java
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
package playground.balmermi.routeset2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;

public class GeneratePathSets {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(GeneratePathSets.class);

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	private static final Map<Id,Tuple<Node,Node>> parseODs(String inputFileName, NetworkImpl network) throws IOException {
		Map<Id,Tuple<Node,Node>> ods = new TreeMap<Id,Tuple<Node,Node>>();
		int lineCnt = 0;
		FileReader fr = new FileReader(inputFileName);
		BufferedReader in = new BufferedReader(fr);

		// Skip header
		String currLine = in.readLine(); lineCnt++;
		while ((currLine = in.readLine()) != null) {
			String[] entries = currLine.split("\t", -1);
			// IDSEGMENT  StartNode  EndNode
			// 0          1          2
			Id id = new IdImpl(entries[0].trim());
			Node origin = network.getNodes().get(new IdImpl(entries[1].trim()));
			Node destination = network.getNodes().get(new IdImpl(entries[2].trim()));
			if ((origin == null) || (destination == null)) { throw new RuntimeException("line "+lineCnt+": O and/or D not found in the network"); }
			ods.put(id,new Tuple<Node,Node>(origin,destination));
			// progress report
			if (lineCnt % 100000 == 0) { log.debug("line "+lineCnt); }
			lineCnt++;
		}
		in.close();
		fr.close();
		log.debug("# lines read: " + lineCnt);
		log.debug("# OD pairs: " + ods.size());
		return ods;
	}

	private static final void writePathSets(String outputFileName, Map<Id,Tuple<Node,Node>> ods, PathSetGenerator gen) throws IOException {
		FileWriter fw = new FileWriter(outputFileName);
		BufferedWriter out = new BufferedWriter(fw);
		out.write("# Routesets\n");
		out.write("# SEG_ID\tFROM_NODE\tTO_NODE\tROUTE(linklist)...\t-1\tLEASTCOSTROUTE(0,1)\t-1\n");
		out.flush();

		for (Map.Entry<Id, Tuple<Node,Node>> entry : ods.entrySet()) {
			Id id = entry.getKey();
			Tuple<Node,Node> od = entry.getValue();
			// generate paths

			log.debug("----------------------------------------------------------------------");
			log.debug("generating path sets for segment id="+id+", O="+od.getFirst().getId()+" and D="+od.getSecond().getId()+"...");
			if (gen.setODPair(od.getFirst(),od.getSecond())) {
				Tuple<Path,List<Path>> paths = gen.getPaths();
				log.debug("done.");
				// write least cost path
				out.write(id.toString()+"\t"+od.getFirst().getId()+"\t"+od.getSecond().getId());
				for (Link l : paths.getFirst().links) { out.write("\t"+l.getId()); }
				out.write("\t-1\t1\t-1\n");
				// write other paths
				for (Path path : paths.getSecond()) {
					out.write(id.toString()+"\t"+od.getFirst().getId()+"\t"+od.getSecond().getId());
					for (Link l : path.links) { out.write("\t"+l.getId()); }
					out.write("\t-1\t0\t-1\n");
				}
				out.flush();
			}
			else {
				log.warn("triple id="+id+", O="+od.getFirst().getId()+" and D="+od.getSecond().getId()+" is omitted.");
			}
			Gbl.printMemoryUsage();
			log.debug("----------------------------------------------------------------------");
		}

		out.close();
		fw.close();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws IOException {
		if (args.length != 6) {
			log.error("Usage: GeneratePathSets nofPaths variantionFactor timeout inputNetworkFile inputODFile outputPathSetFile");
			log.error("       nofPaths:          the number of paths generated per od pair (int >= 0)");
			log.error("       variantionFactor:  degree of variation in the generated path set (double >= 1.0)");
			log.error("       timeout:           maximum calc time of one OD pair in milliseconds (1000 <= long <= 604800000) [1 sec..1 week]");
			log.error("       inputNetworkFile:  matsim input XML network file (String)");
			log.error("       inputODFile:       input id|origin|destination tab seperated table (String)");
			log.error("       outputPathSetFile: output path set file (String)");
			log.error("----------------");
			log.error("2009, matsim.org");
			throw new RuntimeException("incorrect number of arguments");
		}

		Gbl.printSystemInfo();

		int nofPaths = Integer.parseInt(args[0]);
		double variantionFactor = Double.parseDouble(args[1]);
		long timeout = Long.parseLong(args[2]);
		String inputNetworkFile = args[3];
		String inputODFile = args[4];
		String outputPathSetFile = args[5];

		log.info("nofPaths:          "+nofPaths);
		log.info("variantionFactor:  "+variantionFactor);
		log.info("timeout:           "+timeout);
		log.info("inputNetworkFile:  "+inputNetworkFile);
		log.info("inputODFile:       "+inputODFile);
		log.info("outputPathSetFile: "+outputPathSetFile);

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);

		Gbl.printMemoryUsage();

		PathSetGenerator gen = new PathSetGenerator(network);
		gen.setPathSetSize(nofPaths);
		gen.setVariationFactor(variantionFactor);
		gen.setTimeout(timeout);
//		gen.printL2SMapping();

		Map<Id,Tuple<Node,Node>> ods = GeneratePathSets.parseODs(inputODFile,network);

		Gbl.printMemoryUsage();

		GeneratePathSets.writePathSets(outputPathSetFile,ods,gen);
	}
}
