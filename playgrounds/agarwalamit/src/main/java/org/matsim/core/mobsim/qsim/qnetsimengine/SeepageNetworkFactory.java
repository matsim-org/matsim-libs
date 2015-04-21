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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory.QueueWithBufferType;

/**
 * Design thoughts:<ul>
 * <li> It would probably be much better to have this in a separate package.  But this means to move a lot of scopes from
 * "package" to protected.  Worse, the interfaces are not sorted out.  So I remain here for the time being.  kai, jan'11
 */
public final class SeepageNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI> {
	
	private QueueWithBufferType type;

	public SeepageNetworkFactory(){
		this( QueueWithBufferType.standard ) ;
	}
	public SeepageNetworkFactory(QueueWithBufferType queueWithBufferType){
		type = queueWithBufferType ;
	}

	@Override
	public QLinkInternalI createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
		switch( type ){
		case standard:
			return new QLinkImpl( link, network, toQueueNode, new PassingVehicleQ()) ;
		case amit:
			LaneFactory laneFactory = new LaneFactory(){
				@Override
				public QLaneI createLane(QLinkImpl qLinkImpl) {
					AAQueueWithBuffer.Builder builder = new AAQueueWithBuffer.Builder(qLinkImpl) ;
					return builder.build() ;
				}};
			return new QLinkImpl( link, network, toQueueNode, laneFactory ) ;
//		case seep:
//			return new SeepQLinkImpl(link, network, toQueueNode, new PassingVehicleQ()); 
			// seepage can be used from core now.
		default:
			throw new RuntimeException("not implemented") ;
		}
	}

	@Override
	public QNode createNetsimNode(final Node node, QNetwork network) {
		return new QNode(node, network);
	}

}
