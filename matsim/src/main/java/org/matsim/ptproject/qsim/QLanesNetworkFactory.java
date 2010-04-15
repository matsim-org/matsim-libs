/* *********************************************************************** *
 * project: org.matsim.*
 * QLanesNetworkFactory
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

package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.lanes.LaneDefinitions;


public class QLanesNetworkFactory implements QNetworkFactory<QNode, QLink> {

  private QNetworkFactory<QNode, QLink> delegate;
	private LaneDefinitions laneDefinitions;

  public QLanesNetworkFactory(QNetworkFactory<QNode, QLink> delegate, LaneDefinitions laneDefintions){
    this.delegate = delegate;
    this.laneDefinitions = laneDefintions;
  }

  @Override
  public QLink newQueueLink(Link link, QSimEngine engine,
      QNode queueNode) {
  	if (this.laneDefinitions.getLanesToLinkAssignments().containsKey(link.getId())){
  		return new QLinkLanesImpl(link, engine, queueNode);
  	}
  	else {
  		return this.delegate.newQueueLink(link, engine, queueNode);
  	}
  }

  @Override
  public QNode newQueueNode(Node node, QSimEngine simEngine) {
    return this.delegate.newQueueNode(node, simEngine);
  }

}
