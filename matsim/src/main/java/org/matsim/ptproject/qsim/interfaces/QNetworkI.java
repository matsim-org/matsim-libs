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

package org.matsim.ptproject.qsim.interfaces;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.ptproject.qsim.netsimengine.QLinkInternalI;
import org.matsim.ptproject.qsim.netsimengine.QNode;
import org.matsim.ptproject.qsim.netsimengine.QSimEngineImpl;

/**
 * @author nagel
 *
 */
public interface QNetworkI {

	Network getNetwork();

	Map<Id, QLinkInternalI> getLinks();
	// yyyy this should arguable be getQLinks() or getMobsimLinks().  Esthetically less pleasing, but imho easier to use.  kai, may'10
	
	Map<Id,QNode> getNodes() ;

	/**
	 * Convenience method for getLinks().get( id ).  May be renamed 
	 */
	public QLinkInternalI getQLink(final Id id) ;

	/**
	 * Convenience method for getNodes().get( id ).  May be renamed 
	 */
	public QNode getQNode(final Id id) ;

	
	void initialize(QSimEngine netEngine);
	// yyyy not sure if this should/needs to be exposed.  kai, aug'10

}