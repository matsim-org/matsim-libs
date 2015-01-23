/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleWriterHandlerImpl_v0.java
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

package playground.southafrica.projects.complexNetworks.pathDependence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;

public class DigicorePathDependentNetworkWriterHandlerImpl_v2 implements
		DigicorePathDependentNetworkWriterHandler {

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {
		/* Don't think a separator will make the file more readable. */
	}
	
	@Override
	public void startNetwork(PathDependentNetwork network, BufferedWriter out)
			throws IOException {
		out.write("\n<digicoreNetwork");
		if(network.getDescription() != null){
			out.write(" desc=\"" + network.getDescription() + "\"");
		}
		out.write(">\n\n");
	}

	@Override
	public void endNetwork(BufferedWriter out) throws IOException {
		out.write("</digicoreNetwork>");
	}

	@Override
	public void startNode(PathDependentNode node, BufferedWriter out)
			throws IOException {
		out.write("\t<node");
		out.write(" id=\"" + node.getId().toString() + "\"");
		out.write(String.format(" x=\"%.2f\" y=\"%.2f\">\n",
				node.getCoord().getX(), 
				node.getCoord().getY() ) );
	}

	@Override
	public void endNode(PathDependentNode node, BufferedWriter out) throws IOException {
		out.write("\t</node>\n\n");
	}

	@Override
	public void startPreceding(Id<Node> id, BufferedWriter out) throws IOException {
		out.write("\t\t<preceding");
		out.write(" id=\"" + id.toString());
		out.write("\">\n");
	}

	@Override
	public void endPreceding(BufferedWriter out) throws IOException {
		out.write("\t\t</preceding>\n");
	}

	@Override
	public void startFollowing(Map<Id<Node>, Double> following, BufferedWriter out)
			throws IOException {
		for(Id<Node> id : following.keySet()){
			out.write("\t\t\t<following");
			out.write(" id=\"" + id.toString() + "\"");
			out.write(" weight=\"" + String.format("%.2f", following.get(id)));
			out.write("\"/>\n");
		}
	}

	@Override
	public void endFollowing(BufferedWriter out) throws IOException {
		/* Do nothing. */
	}

	@Override
	public void startStartTime(Map<String, Integer> starttime,
			BufferedWriter out) throws IOException {
		/* Convert the hour strings to sortable values */
		Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
		for(String s : starttime.keySet()){
			map.put(Integer.parseInt(s), starttime.get(s));
		}
		out.write("\n");
		out.write("\t\t<!--  Source node: activity chain start times. -->\n");
		for(int i : map.keySet()){
			out.write("\t\t<starttime");
			out.write(" hour=\"" + String.valueOf(i) + "\"");
			out.write(" count=\"" + String.valueOf(map.get(i)) + "\"/>\n");
		}
	}

	@Override
	public void endStartTime(BufferedWriter out) throws IOException {
		/* Do nothing. */
	}

	@Override
	public void startActivities(Map<String, Integer> activities,
			BufferedWriter out) throws IOException {
		/* Convert the activity strings to sortable values */
		Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
		for(String s : activities.keySet()){
			map.put(Integer.parseInt(s), activities.get(s));
		}

		out.write("\n");
		out.write("\t\t<!--  Source node: number of activities per chain. -->\n");
		for(int i : map.keySet()){
			out.write("\t\t<activities");
			out.write(" number=\"" + String.valueOf(i) + "\"");
			out.write(" count=\"" + String.valueOf(map.get(i)) + "\"/>\n");
		}
	}

	@Override
	public void endActivities(BufferedWriter out) throws IOException {
		/* Do nothing. */
	}

}

