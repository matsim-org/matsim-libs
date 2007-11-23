/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLinkSetI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.interfaces.networks.basicNet;

import org.matsim.utils.identifiers.IdentifiedSetI;

/**
 * Represents a set of <code>BasicLinkI</code> instances.
 */
public interface BasicLinkSetI extends IdentifiedSetI {

}

/* GENERICS-Variante
public interface BasicLinkSetI<T extends IdentifiedI> extends IdentifiedSetI<T> {

}
*/