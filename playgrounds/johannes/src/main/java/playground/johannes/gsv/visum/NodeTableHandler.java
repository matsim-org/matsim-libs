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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import playground.johannes.gsv.visum.NetFileReader.TableHandler;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class NodeTableHandler extends TableHandler {

	public final static String ID_KEY = "NR";
	
	public final static String XCOORD = "XKOORD";
	
	public final static String YCOORD = "YKOORD";
	
	private final Network network;
	
	private final CoordinateTransformation transform;
	
	private IdGenerator idGenerator;
	
	public NodeTableHandler(Network network) {
		this(network, new IdentityTransformation());
	}
	
	public NodeTableHandler(Network network, CoordinateTransformation transform) {
		this.network = network;
		this.transform = transform;
	}
	
	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}
	
	@Override
	public void handleRow(Map<String, String> record) {
		Id<Node> id;
		if(idGenerator == null)
			id = Id.create(record.get(ID_KEY), Node.class);
		else
			id = idGenerator.generateId(record.get(ID_KEY), Node.class);

		Coord c = new Coord(Double.parseDouble(record.get(XCOORD)), Double.parseDouble(record.get(YCOORD)));
		Node node = network.getFactory().createNode(id, transform.transform(c));
		network.addNode(node);
	}

}
