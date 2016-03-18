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

package org.matsim.core.mobsim.qsim.qnetsimengine;


import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;


/**
 * @author dgrether
 */
public abstract class QNetworkFactory implements MatsimFactory {

	/**
	 * I need info from the mobsim.  However, as long as the factory is injected, it cannot get to "the" mobsim; at best, it
	 * can get an instance of the mobsim which is, however, not the same mobsim it will be working with.  So "initializeFactory"
	 * is called by the mobsim to provide some info about itself (e.g. agentCounter).
	 * <p/>
	 * This should make the "QNetwork" argument in the creational methods obsolete (which is serving a bit the same purpose).
	 * @param mobsimTimer TODO
	 * @param netsimEngine TODO
	 */
	abstract void initializeFactory( AgentCounter agentCounter, MobsimTimer mobsimTimer, QNetsimEngine netsimEngine ) ;

	abstract QNode createNetsimNode(Node node, QNetwork network);

	abstract QLinkI createNetsimLink(Link link, QNode queueNode);

}
