/* *********************************************************************** *
 * project: org.matsim.*
 * DgKSNetMatsimNetFacade
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
package playground.dgrether.koehlerstrehlersignal.conversion;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;


/**
 * @author dgrether
 *
 */
public class DgKSNet2MatsimNet  {

	private static final Logger log = Logger.getLogger(DgKSNet2MatsimNet.class);
	
	public Network convertNetwork(DgKSNetwork ksNet){
		Network network = NetworkUtils.createNetwork();
		log.info("Converting streets...");
		for (DgStreet street : ksNet.getStreets().values()){
			this.convertStreet(street, network);
		}
		for (DgCrossing crossing : ksNet.getCrossings().values()){
			for (DgStreet street : crossing.getLights().values()){
				this.convertStreet(street, network);	
			}
		}
		return network;
	}
	
	private void convertStreet(DgStreet street, Network net){
		DgCrossingNode fromNode = street.getFromNode();
		DgCrossingNode toNode = street.getToNode();
		if (fromNode.getId().equals(toNode.getId())){
			log.warn("found street with toNode == fromNode...");
			return;
		}
		Node from = net.getNodes().get(fromNode.getId());
		if (from == null) {
			from = net.getFactory().createNode(Id.create(fromNode.getId(), Node.class), fromNode.getCoordinate());
			net.addNode(from);
		}
		Node to = net.getNodes().get(toNode.getId());
		if (to == null) {
			to = net.getFactory().createNode(Id.create(toNode.getId(), Node.class), toNode.getCoordinate());
			net.addNode(to);
		}
		Link link = net.getFactory().createLink(Id.create(street.getId(), Link.class), from, to);
		link.setCapacity(street.getCapacity());
		// warning: street contains link cost instead of link length and freespeed
		// to do not loose this information in the shapefile, we use a default freespeed of 1 m/s and set the link length to the link cost.
		link.setFreespeed(1.0); // default of 1 m/s to get correct link cost
		link.setLength(street.getCost());
		net.addLink(link);
	}
	
}
