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

package playground.kai.devmtg.mynetwork1;

import org.apache.log4j.Logger;
import org.matsim.core.network.NetworkImpl;

public class MyNetwork extends NetworkImpl {

	private final static Logger log = Logger.getLogger(MyNetwork.class);

//	@Override
//	public void addNode(final Node nn) {
//
//		if ( nn instanceof NodeImpl ) {
//			super.addNode( nn ) ;
//		} else {
//			Id id = nn.getId() ;
//			Node node = this.nodes.get(id);
//			if (node != null) {
//				if (node == nn) {
//					log.warn("Trying to add a node a second time to the network. node id = " + id.toString());
//					return;
//				}
//				throw new IllegalArgumentException("There exists already a node with id = " + id.toString() +
//						".\nExisting node: " + node + "\nNode to be added: " + node +
//						".\nNode is not added to the network.");
//			}
//			dosometghin();
//			this.nodes.put(id, nn);
//		}
//
//	}

	private void dosometghin() {
		throw new UnsupportedOperationException() ;
	}
}
