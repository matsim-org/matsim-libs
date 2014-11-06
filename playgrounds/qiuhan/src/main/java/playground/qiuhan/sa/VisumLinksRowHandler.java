/* *********************************************************************** *
 * project: org.matsim.*
 * VisumLinksRowHandle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.qiuhan.sa;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.EdgeType;

public class VisumLinksRowHandler implements VisumNetworkRowHandler {

	private final NetworkImpl network;
	private final VisumNetwork visumNetwork;

	public VisumLinksRowHandler(NetworkImpl network, VisumNetwork visumNetwork) {
		this.network = network;
		this.visumNetwork = visumNetwork;
	}

	@Override
	public void handleRow(Map<String, String> row) {
		String nr = row.get("NR");
		Id<Link> id = Id.create(nr, Link.class);
		Id<Node> fromNodeId = Id.create(row.get("VONKNOTNR"), Node.class);
		Id<Node> toNodeId = Id.create(row.get("NACHKNOTNR"), Node.class);
		Node fromNode = network.getNodes().get(fromNodeId);
		Node toNode = network.getNodes().get(toNodeId);
		Link lastEdge = network.getLinks().get(id);
		if (lastEdge != null) {
			if (lastEdge.getFromNode().getId().equals(toNodeId)
					&& lastEdge.getToNode().getId().equals(fromNodeId)) {
				id = Id.create(nr + 'R', Link.class);
			} else {
				throw new RuntimeException("Duplicate edge.");
			}
		}
		double length = Double.parseDouble(row.get("LAENGE").replace(',', '.')) * 1000;
		// double freespeed = 0.0;
		String edgeTypeIdString = row.get("TYPNR");
		Id<EdgeType> edgeTypeId = Id.create(edgeTypeIdString, EdgeType.class);

		EdgeType edgeType = visumNetwork.edgeTypes.get(edgeTypeId);
		// double capacity = getCapacity(edgeTypeId);

		String VSYSSET_String = row.get("VSYSSET");
		String[] vsyss = VSYSSET_String.split(",");
		Set<String> modes = new TreeSet<String>();
		String mode = null;
		for (String vsys : vsyss) {
			if (vsys.equals("B")) {
				mode = "bus";

			} else if (vsys.equals("F")) {
				mode = TransportMode.walk;

			} else if (vsys.equals("K")) {
				mode = "bus";//kurzstrecke

			} else if (vsys.equals("L")) {
				mode = "other";//LKW

			} else if (vsys.equals("P")) {
				mode = TransportMode.car;

			} else if (vsys.equals("R")) {
				mode = TransportMode.bike;

			} else if (vsys.equals("T")) {
				mode = "tram";

			} else if (vsys.equals("U")) {
				mode = "train";

			} else if (vsys.equals("S")) {
				mode = "train";

			} else if (vsys.equals("W")) {
				mode = "bus";

			} else if (vsys.equals("Z")) {
				mode = "train";// regionalZug

			}
			modes.add(mode);
		}

		double capacity = Double.parseDouble(row.get("KAPIV")
		// edgeType.kapIV
				);

		// // kick out all irrelevant edge types
		// if (isEdgeTypeRelevant(edgeTypeId)) {
		// // take all edges in detailed area
		// if(isEdgeInDetailedArea(fromNode, featuresInShape)){
		//
		// if(innerCity30to40KmhIdsNeedingVmaxChange.contains(edgeTypeIdString)){
		// freespeed = getFreespeedTravelTime(edgeTypeId) / 2;
		// }
		// if(innerCity45to60KmhIdsNeedingVmaxChange.contains(edgeTypeIdString)){
		// freespeed = getFreespeedTravelTime(edgeTypeId) / 1.5;
		// }
		// else{
		// freespeed = getFreespeedTravelTime(edgeTypeId);
		// }
		// network.createAndAddLink(id, fromNode, toNode, length, freespeed,
		// capacity, 1, null, edgeTypeIdString);
		// usedIds.add(edgeTypeIdString);
		// }
		// // kick out all edges in periphery that are irrelevant only there
		// else {
		// if(isEdgeTypeRelevantForPeriphery(edgeTypeId)){
		// freespeed = getFreespeedTravelTime(edgeTypeId);

		double freespeed = Double.parseDouble(row.get("V0IV")
		// edgeType.v0IV
				) / 3.6;

		network.createAndAddLink(id, fromNode, toNode, length, freespeed,
				capacity, 1, null, edgeTypeIdString);

		this.network.getLinks().get(id).setAllowedModes(modes);
		// usedIds.add(edgeTypeIdString);
		// }
		// }

		// }
	}
}
