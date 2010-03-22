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


public class QLanesNetworkFactory implements QNetworkFactory<QNode, QLink> {

  private QNetworkFactory<QNode, QLink> delegate;

  public QLanesNetworkFactory(QNetworkFactory<QNode, QLink> delegate){
    this.delegate = delegate;
  }

  @Override
  public QLinkLanesImpl newQueueLink(Link link, QSimEngine engine,
      QNode queueNode) {
    return new QLinkLanesImpl(link, engine, queueNode);
  }

  @Override
  public QNode newQueueNode(Node node, QNetwork queueNetwork) {
    return this.delegate.newQueueNode(node, queueNetwork);
  }

}
