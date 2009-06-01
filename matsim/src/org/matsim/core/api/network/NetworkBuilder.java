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
package org.matsim.core.api.network;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNetworkBuilder;


/**
 * @author dgrether
 *
 */
public interface NetworkBuilder extends BasicNetworkBuilder {

	public Node createNode(final Id id);
	
	public Link createLink(final Id id, final Id fromNodeId, final Id toNodeId);
	
	
}
