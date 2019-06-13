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

package org.matsim.core.mobsim.qsim.qnetsimengine;

abstract class NetElementActivationRegistry {
	// yy In my language intuition, a NetElementActivator (previous name) is something that activates something else.  
	// Here, however, this something that is is activated _by_ something else.  What is a better name?  ActiveNetElementsRegister? 

	abstract void registerNodeAsActive(final QNodeI node);
	
	abstract int getNumberOfSimulatedNodes();
	
	abstract void registerLinkAsActive(final QLinkI link);

	abstract int getNumberOfSimulatedLinks();
} 