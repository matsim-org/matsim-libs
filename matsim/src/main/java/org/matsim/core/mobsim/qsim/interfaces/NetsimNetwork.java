/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.interfaces;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimNetworkObject;
import org.matsim.vis.snapshotwriters.VisNetwork;

/**
 * @author nagel
 *
 */
public interface NetsimNetwork extends VisNetwork, MatsimNetworkObject {
	// yyyy "extends VisNetwork" possibly a temporary fix

	@Override
	Network getNetwork();

	Map<Id<Link>, ? extends NetsimLink> getNetsimLinks();
	// yyyy this should arguable be getQLinks() or getMobsimLinks().  Esthetically less pleasing, but imho easier to use.  kai, may'10

	Map<Id<Node>, ? extends NetsimNode> getNetsimNodes() ;

	/**
	 * Convenience method for getLinks().get( id ).  May be renamed
	 */
	public NetsimLink getNetsimLink(final Id<Link> id) ;

	/**
	 * Convenience method for getNodes().get( id ).  May be renamed
	 */
	public NetsimNode getNetsimNode(final Id<Node> id) ;

}