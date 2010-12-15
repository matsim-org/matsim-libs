package playground.ciarif.roadpricing;

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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



import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id; 
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;

public class MyNetwork extends NetworkImpl {
	
	
	private final static Logger log = Logger.getLogger(MyNetwork.class);
	
	
	 public MyNetwork () {
			this.factory = new NetworkFactoryImpl(this);
	}
	

	@Override
	public void addNode(final Node nn) {

		if ( nn instanceof NodeImpl ) {
			super.addNode( nn ) ;
		} else {
			Id id = nn.getId() ;
			Node node = this.nodes.get(id);
			if (node != null) {
				if (node == nn) {
					log.warn("Trying to add a node a second time to the network. node id = " + id.toString());
					return;
				}
				throw new IllegalArgumentException("There exists already a node with id = " + id.toString() +
						".\nExisting node: " + node + "\nNode to be added: " + node +
						".\nNode is not added to the network.");
			}
			dosomething();
			this.nodes.put(id, nn);
		}

	}

	@Override
	public void addLink(final Link link) {
		Link testLink = getLinks().get(link.getId());
		if (testLink != null) {
			if (testLink == link) {
				log.warn("Trying to add a link a second time to the network. link id = " + link.getId().toString());
				return;
			}
			throw new IllegalArgumentException("There exists already a link with id = " + link.getId().toString() +
					".\nExisting link: " + testLink + "\nLink to be added: " + link +
					".\nLink is not added to the network.");
		}
		this.getLinks().put(link.getId(), link);
	}
	
	private void dosomething() {
		throw new UnsupportedOperationException() ;
	}
}