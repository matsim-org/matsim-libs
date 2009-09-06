/* *********************************************************************** *
 * project: org.matsim.*
 * BasicNetworkBuilder
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
package org.matsim.api.basic.v01.network;

import org.matsim.api.basic.v01.Id;


/**
 * Builder for network elements
 * @author dgrether
 * @deprecated use version in org.matsim.api.core
 */
@Deprecated // use version in org.matsim.api.core
public interface BasicNetworkBuilder {
	/**
	 * creates a node
	 * @param id
	 * @return
	 * @deprecated use version in org.matsim.core
	 */
	@Deprecated // use version in org.matsim.api.core
	public BasicNode createNode(final Id id);

	/** @deprecated use version in org.matsim.api.core */
	@Deprecated // use version in org.matsim.api.core
	public BasicLink createLink(final Id id, final Id fromNodeId, final Id toNodeId);
	
}
