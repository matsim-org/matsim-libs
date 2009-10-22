/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.core.api.internal;

/**
 * <p> essentially a marker interface </p>
 * 
 * <p>
 * The first level of elements below top level containers should NOT have a back pointer ...
 * ... since, at that level, one should be able to get the relevant container from
 * the context.
 * (This becomes clear if you think about a nodeId/linkId given by person.)
 * </p>
 * 
 * @author nagel
 *
 */
public interface MatsimToplevelContainer {
	
	MatsimFactory getFactory() ;

}
