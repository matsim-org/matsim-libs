/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.complexNetworks.pathDependence;

import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

public class DigicorePathDependentNetworkReader_v2 extends MatsimXmlParser {
	private final static String NETWORK = "digicoreNetwork";
	private final static String NODE = "node";
	private final static String PRECEDING = "preceding";
	private final static String FOLLOWING = "following";
	private final static String STARTNODE = "startnode";
	
	/* Attributes. */
	private final static String ATTR_DESCR = "desc";
	private final static String ATTR_NODE_ID = "id";
	private final static String ATTR_X = "x";
	private final static String ATTR_Y = "y";
	private final static String ATTR_PRECEDING_ID = "id";
	private final static String ATTR_FOLLOWING_ID = "id";
	private final static String ATTR_WEIGHT = "weight";
	private final static String ATTR_STARTHOUR = "hour";
	private final static String ATTR_STARTACTIVITIES = "activities";
	private final static String ATTR_STARTCOUNT = "count";
	
	private PathDependentNetwork network = null;
	
	private Id<Node> currentNodeId = null;
	private Id<Node> currentPrecedingId = null;
	
	private final Counter counter = new Counter("  vertices # ");
	
	
	/**
	 * Executes the path-dependent network reader.
	 * @param args
	 */
	public static void main(String[] args){
		String input = args[0];
		DigicorePathDependentNetworkReader_v2 nr = new DigicorePathDependentNetworkReader_v2();
		nr.parse(input);
		nr.network.writeNetworkStatisticsToConsole();
	}
	
	
	public DigicorePathDependentNetworkReader_v2() {

	}
	
	public PathDependentNetwork getPathDependentNetwork(){
		return this.network;
	}
	

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(NETWORK.equals(name)){
			network = new PathDependentNetwork();
			String descr = atts.getValue(ATTR_DESCR);
			if(descr != null){
				network.setDescription(descr);
			}
		} else if(NODE.equals(name)){
			currentNodeId = Id.create(atts.getValue(ATTR_NODE_ID), Node.class);
			String x = atts.getValue(ATTR_X);
			String y = atts.getValue(ATTR_Y);
			network.addNewPathDependentNode(currentNodeId, new Coord(Double.parseDouble(x), Double.parseDouble(y)));
		} else if(PRECEDING.equals(name)){
			currentPrecedingId = Id.create(atts.getValue(ATTR_PRECEDING_ID), Node.class);
		} else if (FOLLOWING.equals(name)){
			Id<Node> nextId = Id.create(atts.getValue(ATTR_FOLLOWING_ID), Node.class);
			double weight = Double.parseDouble(atts.getValue(ATTR_WEIGHT));
			this.network.setPathDependentEdgeWeight(currentPrecedingId, currentNodeId, nextId, weight);
		} else if (STARTNODE.equals(name)){
			String hour = atts.getValue(ATTR_STARTHOUR);
			String activities = atts.getValue(ATTR_STARTACTIVITIES);
			int count = Integer.parseInt(atts.getValue(ATTR_STARTCOUNT));
			this.network.getPathDependentNode(currentNodeId).getStartNodeMap().put(hour + "," + activities, count);
		} else{
			throw new RuntimeException(this + "[tag=" + name + " not known or supported]");
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(NETWORK.equals(name)){
			/* Do nothing, but print the counter. */
			counter.printCounter();
		} else if(NODE.equals(name)){
			currentNodeId = null;
			counter.incCounter();
		} else if(PRECEDING.equals(name)){
			currentPrecedingId = null;
		} else if (FOLLOWING.equals(name)){
			/* Do nothing. */
		} else if (STARTNODE.equals(name)){
			/* Do nothing. */
		} else{
			throw new RuntimeException(this + "[tag=" + name + " not known or supported]");
		}
	}
	
	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		// Currently checks for version v1 & v2.
		if ("digicorePathDependentNetwork_v1.dtd".equals(doctype)) {
		} else if ("digicorePathDependentNetwork_v2.dtd".equals(doctype)){
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}


}
