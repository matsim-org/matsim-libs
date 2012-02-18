/* *********************************************************************** *
 * project: kai
 * MyHybridNode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Node;

/**
 * @author nagel
 */
public class KaiHybridNode extends QNode {

	 KaiHybridNode(Node n, QNetwork network) {
		super(n, network);
	}
	
	@Override
	/*package*/ void clearLinkBuffer(final AbstractQLink inLink, final double now){
		if (inLink instanceof QLinkImpl){
			while (!inLink.bufferIsEmpty()) {
				QVehicle veh = inLink.getFirstFromBuffer();
				if (!moveVehicleFromQLinkOverNode(veh, inLink, now)) {
					break;
				}
			}
			
		} else {
			while (!inLink.bufferIsEmpty()) {
				QVehicle veh = inLink.getFirstFromBuffer();
				if (!moveVehicleFromOtherLinkOverNode(veh, inLink, now)) {
					break;
				}
			}
		}
	}

	private boolean moveVehicleFromOtherLinkOverNode(QVehicle veh, AbstractQLink inLink, double now) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	private boolean moveVehicleFromQLinkOverNode(QVehicle veh, AbstractQLink inLink, double now) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

}
