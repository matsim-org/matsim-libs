/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.api.experimental.network;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNetworkBuilder;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;


/**
 * @author dgrether
 *
 */
public interface NetworkBuilder extends BasicNetworkBuilder {

	public NodeImpl createNode(final Id id);
	
	public LinkImpl createLink(final Id id, final Id fromNodeId, final Id toNodeId);
	
	
}
