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
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;


/**
 * <p> The current design idea of this is as follows:<ul>
 * <li> It is now possible to inject this.  See {@link QSimModule} for how the default is set up.   It can also be overridden in the usual way.
 * <li> For a specific example, see {@link DefaultQNetworkFactory}.
 * </ul></p>
 *
 * @author dgrether, nagel
 *
 * @see DefaultQNetworkFactory
 * @see ConfigurableQNetworkFactory
 *
 */
public interface QNetworkFactory extends MatsimFactory {

	/**
	 * <p>I need info from the mobsim.  However, as long as the factory is injected, it cannot get to "the" mobsim; at best, it
	 * can get an instance of the mobsim which is, however, not the same mobsim it will be working with.  So "initializeFactory"
	 * is called by the mobsim to provide some info about itself (e.g. agentCounter).
	 * </p><p>
	 * This could be solved using custom scopes.  They do, however, for the time being only exist in the guice inject framework, not
	 * in jakarta.inject, and thus we do not want to use them (yet?).
	 * </p><p>
	 * (yy I think that the above has been changed with the separate QSimComponents, and thus one could attempt to fix it.  kai, jul'22)
	 * </p>
	 */
	void initializeFactory( AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface simEngine1 ) ;

	QNodeI createNetsimNode(Node node);

	QLinkI createNetsimLink(Link link, QNodeI queueNode);

}
