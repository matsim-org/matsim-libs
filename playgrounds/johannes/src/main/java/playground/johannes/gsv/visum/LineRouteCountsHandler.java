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
package playground.johannes.gsv.visum;

import gnu.trove.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import playground.johannes.gsv.visum.NetFileReader.TableHandler;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class LineRouteCountsHandler extends TableHandler {
	
	private static final Logger logger = Logger.getLogger(LineRouteCountsHandler.class);

	public static final String NODE_KEY = "KNOTNR";
	
	public static final String COUNTS_KEY = "SUMAKTIVE:BENUTZENDEFZPELEMENTE\\SUMAKTIVE:FAHRPLANFAHRTELEMENTE\\BESETZ";
	
	public static final String LINE_KEY = "LINNAME";
	
	public static final String ROUTE_KEY = "LINROUTENAME";
	
	public static final String DCODE_KEY = "RICHTUNGCODE";
	
	public static final String INDEX_KEY = "INDEX";
	
	private final TObjectDoubleHashMap<Link> counts;
	
	private final Set<CountsContainer> counts2;
	
	private String lastLine;
	
	private String lastRoute;
	
	private String lastDCode;
	
	private int lastIndex = 1;
	
	private Node fromNode;
	
	double lastCountValue;
	
	private final Network network;
	
	private final IdGenerator idGenerator;
	
	int unmatchedLinks = 0;
	int total = 0;
	
	public LineRouteCountsHandler(Network network) {
		this.network = network;
		counts = new TObjectDoubleHashMap<Link>(network.getLinks().size());
		counts2 = new LinkedHashSet<LineRouteCountsHandler.CountsContainer>(network.getLinks().size());
		idGenerator = new PrefixIdGenerator("rail.");
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.visum.NetFileReader.TableHandler#handleRow(java.util.Map)
	 */
	@Override
	public void handleRow(Map<String, String> record) {
		int index = Integer.parseInt(record.get(INDEX_KEY));
		String line = record.get(LINE_KEY);
		String route = record.get(ROUTE_KEY);
		String dcode = record.get(DCODE_KEY);
		Node toNode = network.getNodes().get(idGenerator.generateId(record.get(NODE_KEY), Node.class));
		
		
		if(index - 1 == lastIndex) {
			
			if(!line.equals(lastLine))
				throw new RuntimeException("Line name does not match.");
			
			if(!route.equals(lastRoute))
				throw new RuntimeException("Route name does not match.");
			
			if(!dcode.equals(lastDCode))
				throw new RuntimeException("Direction does not match.");
			
			if (fromNode != null && toNode != null) {
				total++;
				Link link = NetworkUtils.getConnectingLink(fromNode, toNode);

				if (link == null) {
					link = NetworkUtils.getConnectingLink(toNode, fromNode);
					if (link == null) {
						// throw new RuntimeException("Link not found.");
//						logger.warn("Link not found, ignoring count.");
						unmatchedLinks++;
					}
					else {
						logger.warn("Link not found, yet using return link.");
						
					}
				}

				if (link != null) {
					counts.put(link, lastCountValue);
					CountsContainer container = new CountsContainer();
					container.trainId = line;
					container.linkeId = link.getId().toString();
					container.count = lastCountValue;
					counts2.add(container);
				}

			} else {
				logger.warn("Node not found, ignoring count.");
			}
			
		}
		
		lastIndex = index;
		lastLine = line;
		lastRoute = route;
		lastDCode = dcode;
		fromNode = toNode;
		
		String str = record.get(COUNTS_KEY);
		if(str != null && !str.isEmpty())
			lastCountValue = Double.parseDouble(str);	
		else
			lastCountValue = 0;
	}

	public TObjectDoubleHashMap<Link> getCounts() {
		return counts;
	}
	
	public Set<CountsContainer> getCounts2() {
		logger.warn(String.format("%s of %s unmatched.", unmatchedLinks, total));
		return counts2;
	}
	
	public static class CountsContainer {
		
		public String trainId;
		
		public String linkeId;
		
		public double count;
	}
}
