/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.wagonSim.production;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.Connection;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.ProductionNode;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.RbNode;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.RcpDeliveryType;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.RcpNode;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.SatNode;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author balmermi @ Senozon AG
 * @since 2013-09-19
 */
public class ProductionParser {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private final ProductionDataContainer dataContainer;
	
	static final String RB_NODE_POSTFIX = "_RB";
	static final String RCP_NODE_POSTFIX = "_RCP";
	static final String SAT_NODE_POSTFIX = "_SAT";

	private static final Logger log = Logger.getLogger(ProductionParser.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public ProductionParser(ProductionDataContainer dataContainer) {
		this.dataContainer = dataContainer;
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	private static final String RB_VERSCHUBKNOTEN = "Verschubknoten";
	private static final String RB_MAXZUGBILDUNGEN = "MaxZugbildungen";
	private static final String RB_MAXWAGENUMSTELLUNGEN = "MaxWagenumstellungen";
	private static final String RB_WAGENUMSTELLZEIT = "Wagenumstellzeit";

	public final void parseRbFile(String rbFile) throws IOException {
		log.info("--- START parsing rbFile ---");
		BufferedReader br = IOUtils.getBufferedReader(rbFile);
		int currRow = 0;
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine(); currRow++;
		String [] header = curr_line.split(";");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(header[i].trim(),i); }

		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");

			String name = Utils.removeSurroundingQuotes(row[lookup.get(RB_VERSCHUBKNOTEN)].trim())+RB_NODE_POSTFIX;
			int maxTrainCreation = Integer.parseInt(Utils.removeSurroundingQuotes(row[lookup.get(RB_MAXZUGBILDUNGEN)].trim()));
			int maxWagonShuntings = Integer.parseInt(Utils.removeSurroundingQuotes(row[lookup.get(RB_MAXWAGENUMSTELLUNGEN)].trim()));
			int shuntingTime = Integer.parseInt(Utils.removeSurroundingQuotes(row[lookup.get(RB_WAGENUMSTELLZEIT)].trim()));
			
			if (dataContainer.productionNodes.containsKey(Id.create(name, Node.class))) {
				throw new RuntimeException("row "+currRow+": Production node '"+name+"' already exists. Bailing out.");
			}
			RbNode rbNode = new RbNode(name);
			dataContainer.productionNodes.put(rbNode.id,rbNode);
			rbNode.maxTrainCreation = maxTrainCreation;
			rbNode.maxWagonShuntings = maxWagonShuntings;
			rbNode.shuntingTime = shuntingTime;
		}
//		dataContainer.printProductionNodes();
		log.info("--- END   parsing rbFile ---");
	}

	//////////////////////////////////////////////////////////////////////
	
	private static final String ID_VERSANDTYP = "id_Versandtyp";
	private static final String BEZEICHNUNG = "Bezeichnung";
	private static final String VERSANDTYP = "Versandtyp";
	private static final String STUNDE = "Stunde";
	private static final String ANTEIL = "Anteil";
	
	public final void parseDeliveryAndTimeVariationFiles(String deliveryTypeFile, String timeVariationFile) throws IOException {
		log.info("--- START parsing deliveryTypeFile ---");
		BufferedReader br = IOUtils.getBufferedReader(deliveryTypeFile);
		int currRow = 0;
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine(); currRow++;
		String [] header = curr_line.split(";");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(header[i].trim(),i); }

		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");

			int deliveryTypeId = Integer.parseInt(Utils.removeSurroundingQuotes(row[lookup.get(ID_VERSANDTYP)].trim()));
			String desc = Utils.removeSurroundingQuotes(row[lookup.get(BEZEICHNUNG)].trim());
			
			if (dataContainer.rcpDeliveryTypes.containsKey(deliveryTypeId)) { throw new RuntimeException("row "+currRow+": delivery type '"+deliveryTypeId+"' already exists. Bailing out."); }
			RcpDeliveryType rcpDeliveryType = new RcpDeliveryType(deliveryTypeId,desc);
			dataContainer.rcpDeliveryTypes.put(rcpDeliveryType.id,rcpDeliveryType);
		}
//		dataContainer.printRcpDeliveryTypes();
		log.info("--- END   parsing deliveryTypeFile ---");
		
		log.info("--- START parsing timeVariationFile ---");
		br = IOUtils.getBufferedReader(timeVariationFile);
		currRow = 0;
		curr_line =null;

		// read header an build lookup
		curr_line = br.readLine(); currRow++;
		header = curr_line.split(";");
		lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(header[i].trim(),i); }

		// parse rows and store nodes
		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");

			String deliveryTypeId = Utils.removeSurroundingQuotes(row[lookup.get(VERSANDTYP)].trim());
			int hour = Integer.parseInt(Utils.removeSurroundingQuotes(row[lookup.get(STUNDE)].trim()));
			double fraction = Double.parseDouble(Utils.removeSurroundingQuotes(row[lookup.get(ANTEIL)].trim()));

			RcpDeliveryType rcpDeliveryType = dataContainer.rcpDeliveryTypes.get(deliveryTypeId);
			if (!dataContainer.rcpDeliveryTypes.containsKey(deliveryTypeId)) { throw new RuntimeException("row "+currRow+": delivery type '"+deliveryTypeId+"' does not exist. Bailing out."); }
			rcpDeliveryType.hourlyDistribution[hour] = fraction;
		}
//		dataContainer.printRcpDeliveryTypes();
		log.info("--- END   parsing timeVariationFile ---");
	}
	
	//////////////////////////////////////////////////////////////////////

	private static final String RCP_BEDIENKNOTEN = "bedienknoten";
	private static final String RCP_VERSCHUBKNOTEN = "verschubknoten";
	private static final String RCP_VERSCHUBKNOTEN_EMPFANG = "verschubknoten_empfang";
	private static final String RCP_GRENZE = "grenze";
	private static final String RCP_VERSANDTYP = "versandtyp";
	
	public final void parseRcpFile(String rcpFile) throws IOException {
		log.info("--- START parsing rcpFile ---");
		BufferedReader br = IOUtils.getBufferedReader(rcpFile);
		int currRow = 0;
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine(); currRow++;
		String [] header = curr_line.split(";");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(header[i].trim(),i); }

		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");

			String name = Utils.removeSurroundingQuotes(row[lookup.get(RCP_BEDIENKNOTEN)].trim())+RCP_NODE_POSTFIX;
			
			Id<Node> rbNodeId = Id.create(Utils.removeSurroundingQuotes(row[lookup.get(RCP_VERSCHUBKNOTEN)].trim())+RB_NODE_POSTFIX, Node.class);
			ProductionNode rbNode = dataContainer.productionNodes.get(rbNodeId);
			if (rbNode == null) { throw new RuntimeException("row "+currRow+": "+RCP_VERSCHUBKNOTEN+"="+rbNodeId+" does not exist. Bailing out."); }
			if (!(rbNode instanceof RbNode)) { throw new RuntimeException("row "+currRow+": "+RCP_VERSCHUBKNOTEN+"="+rbNodeId+" is not of type RB. Bailing out."); }
			
			Id<Node> receptionNodeId = Id.create(Utils.removeSurroundingQuotes(row[lookup.get(RCP_VERSCHUBKNOTEN_EMPFANG)].trim())+RB_NODE_POSTFIX, Node.class);
			ProductionNode receptionNode = dataContainer.productionNodes.get(receptionNodeId);
			if (receptionNode == null) { throw new RuntimeException("row "+currRow+": "+RCP_VERSCHUBKNOTEN_EMPFANG+"="+receptionNodeId+" does not exist. Bailing out."); }
			
			int border = Integer.parseInt(Utils.removeSurroundingQuotes(row[lookup.get(RCP_GRENZE)].trim()));
			boolean isBorder = false;
			if (border == 1) { isBorder = true; }
			
			String deliveryTypeId = Utils.removeSurroundingQuotes(row[lookup.get(RCP_VERSANDTYP)].trim());
			RcpDeliveryType rcpDeliveryType = dataContainer.rcpDeliveryTypes.get(deliveryTypeId);
			if (!dataContainer.rcpDeliveryTypes.containsKey(deliveryTypeId)) { throw new RuntimeException("row "+currRow+": delivery type '"+deliveryTypeId+"' does not exist. Bailing out."); }
			
			if (dataContainer.productionNodes.containsKey(Id.create(name, Node.class))) { throw new RuntimeException("row "+currRow+": Production node '"+name+"' already exists. Bailing out."); }
			RcpNode rcpNode = new RcpNode(name);
			dataContainer.productionNodes.put(rcpNode.id,rcpNode);
			rcpNode.parentNode = rbNode;
			rbNode.siblingNodes.add(rcpNode);
			rcpNode.parentReceptionNode = receptionNode;
			rcpNode.isBorder = isBorder;
			rcpNode.deliveryType = rcpDeliveryType;
		}
//		dataContainer.printProductionNodes();
		log.info("--- END   parsing rcpFile ---");
	}
	
	//////////////////////////////////////////////////////////////////////

	private static final String SAT_ABFERTIGUNGSSTELLE = "Abfertigungsstelle";
	private static final String SAT_BEDIENKNOTEN = "Bedienknoten";
	private static final String SAT_BEDIENKNOTEN_EMPFANG = "Bedienknoten_Empfang";
	private static final String SAT_MINDESTBEDIENUNG = "Mindestbedienung";
	
	public final void parseSatFile(String satFile) throws IOException {
		log.info("--- START parsing satFile ---");
		BufferedReader br = IOUtils.getBufferedReader(satFile);
		int currRow = 0;
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine(); currRow++;
		String [] header = curr_line.split(";");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(header[i].trim(),i); }

		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");

			String name = Utils.removeSurroundingQuotes(row[lookup.get(SAT_ABFERTIGUNGSSTELLE)].trim())+SAT_NODE_POSTFIX;
			
			Id<Node> rcpNodeId = Id.create(Utils.removeSurroundingQuotes(row[lookup.get(SAT_BEDIENKNOTEN)].trim())+RCP_NODE_POSTFIX, Node.class);
			ProductionNode rcpNode = dataContainer.productionNodes.get(rcpNodeId);
			if (rcpNode == null) { throw new RuntimeException("row "+currRow+": "+SAT_BEDIENKNOTEN+"="+rcpNode+" does not exist. Bailing out."); }
			if (!(rcpNode instanceof RcpNode)) { throw new RuntimeException("row "+currRow+": "+SAT_BEDIENKNOTEN+"="+rcpNode+" is not of type RCP. Bailing out."); }
			
			Id<Node> receptionNodeId = Id.create(Utils.removeSurroundingQuotes(row[lookup.get(SAT_BEDIENKNOTEN_EMPFANG)].trim())+RCP_NODE_POSTFIX, Node.class);
			ProductionNode receptionNode = dataContainer.productionNodes.get(receptionNodeId);
			if (receptionNode == null) { throw new RuntimeException("row "+currRow+": "+SAT_BEDIENKNOTEN_EMPFANG+"="+receptionNodeId+" does not exist. Bailing out."); }
			
			double minService = Double.parseDouble(Utils.removeSurroundingQuotes(row[lookup.get(SAT_MINDESTBEDIENUNG)].trim()));
			
			if (dataContainer.productionNodes.containsKey(Id.create(name, Node.class))) { throw new RuntimeException("row "+currRow+": Production node '"+name+"' already exists. Bailing out."); }
			SatNode satNode = new SatNode(name);
			dataContainer.productionNodes.put(satNode.id,satNode);
			satNode.parentNode = rcpNode;
			rcpNode.siblingNodes.add(satNode);
			satNode.parentReceptionNode = receptionNode;
			satNode.minService = minService;
		}
//		dataContainer.printProductionNodes();
		log.info("--- END   parsing satFile ---");
	}
	
	//////////////////////////////////////////////////////////////////////

	private static final String ROUTE_VONKNOTEN = "VonKnoten";
	private static final String ROUTE_NACHKNOTEN = "NachKnoten";
	private static final String ROUTE_ZWISCHENKNOTENNR = "ZwischenKnotenNr";
	private static final String ROUTE_ZWISCHENKNOTEN = "Zwischenknoten";
	
	public final void parseRouteFile(String routeFile) throws IOException {
		log.info("--- START parsing routeFile ---");
		BufferedReader br = IOUtils.getBufferedReader(routeFile);
		int currRow = 0;
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine(); currRow++;
		String [] header = curr_line.split(";");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(header[i].trim(),i); }

		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");

			String from = Utils.removeSurroundingQuotes(row[lookup.get(ROUTE_VONKNOTEN)].trim());
			String to = Utils.removeSurroundingQuotes(row[lookup.get(ROUTE_NACHKNOTEN)].trim());
			int index = Integer.parseInt(Utils.removeSurroundingQuotes(row[lookup.get(ROUTE_ZWISCHENKNOTENNR)].trim()));
			String via = Utils.removeSurroundingQuotes(row[lookup.get(ROUTE_ZWISCHENKNOTEN)].trim());

			Id<Node> fromId = Id.create(from+RB_NODE_POSTFIX, Node.class);
			ProductionNode fromNode = dataContainer.productionNodes.get(fromId);
			if (fromNode == null) { throw new RuntimeException("row "+currRow+": from node id="+from+" is not an RB node. Bailing out."); }

			Id<Node> toId = Id.create(to+RB_NODE_POSTFIX, Node.class);
			ProductionNode toNode = dataContainer.productionNodes.get(toId);
			if (toNode == null) { throw new RuntimeException("row "+currRow+": to node id="+to+" is not an RB node. Bailing out."); }

			Id<Node> viaId = Id.create(via+RB_NODE_POSTFIX, Node.class);
			ProductionNode viaNode = dataContainer.productionNodes.get(viaId);
			if (viaNode == null) { throw new RuntimeException("row "+currRow+": via node id="+via+" is not an RB node. Bailing out."); }

			Id<Connection> routeId = Id.create(fromNode.id.toString()+"---"+toNode.id.toString(), Connection.class);
			Connection route = dataContainer.connections.get(routeId);
			if (route == null) {
				if (index != 1) { throw new RuntimeException("row "+currRow+": file is not sorted according to the via node index. Bailing out."); }
				route = new Connection(routeId,fromNode,toNode);
				dataContainer.connections.put(route.id,route);
				route.viaNodes.add(viaNode);
			}
			else {
				if (index != route.viaNodes.size()+1) { throw new RuntimeException("row "+currRow+": file is not sorted according to the via node index. Bailing out."); }
				route.viaNodes.add(viaNode);
			}
		}
		for (Connection c : dataContainer.connections.values()) {
			if (!c.viaNodes.get(c.viaNodes.size()-1).id.equals(c.toNode.id)) { throw new RuntimeException("in route file it is expected that the last via node is identical to the toNode. This is not the case for route="+c.toString()); }
		}
		
		dataContainer.printConnections();
		log.info("--- END   parsing routeFile ---");
	}
}
