/* *********************************************************************** *
 * project: org.matsim.*
 * VisumNodesRowHandler.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mzilske.bvg09.VisumNetworkRowHandler;

public class VisumNodesRowHandler implements VisumNetworkRowHandler {

	private NetworkImpl network;

	public VisumNodesRowHandler(NetworkImpl network) {
		this.network = network;
	}

	@Override
	public void handleRow(Map<String, String> row) {
		Id id = new IdImpl(row.get("NR"));
		Coord coord = new CoordImpl(Double.parseDouble(row.get("XKOORD")
				.replace(',', '.')), Double.parseDouble(row.get("YKOORD")
				.replace(',', '.')));
		network.createAndAddNode(id, coord);
	}

}
