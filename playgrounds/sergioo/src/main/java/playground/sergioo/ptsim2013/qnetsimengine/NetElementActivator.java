/* *********************************************************************** *
 * project: org.matsim.*
 * NetElementActivator.java
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

package playground.sergioo.ptsim2013.qnetsimengine;

import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;

public abstract class NetElementActivator {

	protected abstract void activateNode(final QNode node);
	
	abstract int getNumberOfSimulatedNodes();
	
	protected abstract void activateLink(final NetsimLink link);

	abstract int getNumberOfSimulatedLinks();
} 