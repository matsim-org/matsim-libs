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
package org.matsim.ptproject.qsim.lanes;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.ptproject.qsim.QLaneNode;
import org.matsim.ptproject.qsim.QLinkLanesImpl;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.ptproject.qsim.QueueNetworkFactory;
import org.matsim.ptproject.qsim.QueueNode;


public class QLanesNetworkFactory implements QueueNetworkFactory<QueueNode, QueueLink> {

  private QueueNetworkFactory<QueueNode, QueueLink> delegate;

  public QLanesNetworkFactory(QueueNetworkFactory<QueueNode, QueueLink> delegate){
    this.delegate = delegate;
  }
  
  @Override
  public QLinkLanesImpl newQueueLink(Link link, QueueNetwork queueNetwork,
      QueueNode queueNode) {
    return new QLinkLanesImpl(link, queueNetwork, queueNode);
  }

  @Override
  public QueueNode newQueueNode(Node node, QueueNetwork queueNetwork) {
    return new QLaneNode(node, queueNetwork);
  }

}
