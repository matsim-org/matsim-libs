/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.api.core.v01;

import java.util.Map;

import org.matsim.core.api.internal.MatsimExtensionPoint;

/**
 * An interface to customize any class in <tt>org.matsim</tt> for
 * experiments only.
 * 
 * <p>In an experiment---implemented in the <tt>playground</tt>---it can be
 * useful to add additional information, objects or complex structures to
 * MATSim core classes. Therefore, it is allowed to implement this interface
 * for any class given in <tt>org.matsim</tt> and assign additional information
 * from the <tt>playground</tt> only.</p>
 * 
 * <p><b>BUT:</b> is is strictly prohibited to call methods defined in this interface
 * from any point inside the MATSim core (<tt>org.matsim</tt>).</p>
 * 
 * @author balmermi
 *
 * @deprecated use {@link org.matsim.utils.objectattributes.attributable.Attributable instead}
 */
@Deprecated
public interface Customizable extends MatsimExtensionPoint {

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * returns a container for adding arbitrary additional information to
	 * a MATSim core object.
	 * 
	 * <p>IMPORTANT NOTE: This container is meant to use for experiments only.
	 * Therefore, do NOT call this method from the core (<tt>org.matsim</tt>).</p>

	 * @return map for storing custom attributes
	 */
	public Map<String,Object> getCustomAttributes();
}
