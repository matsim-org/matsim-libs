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

package org.matsim.core.scenario;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Customizable;

/**
 * Standard implementation of the <tt>Customizable</tt> interface.
 * 
 * <p>In an experiment---implemented in the <tt>playground</tt>---it can be
 * useful to add additional information, objects or complex structures to
 * MATSim core classes. Therefore, it is allowed to extend 
 * any class given in <tt>org.matsim</tt> and assign additional information
 * from the <tt>playground</tt> only.</p>
 * 
 * <p><b>BUT:</b> is is strictly prohibited to call methods defined and implemented
 * in this class from any point inside the MATSim core (<tt>org.matsim</tt>).</p>
 * 
 * @see Customizable
 * 
 * @author balmermi
 */
final class CustomizableImpl implements Customizable {
	
	CustomizableImpl() {} 

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	/**
	 * Container for custom attributes
	 */
	private Map<String,Object> custom_attributes = null;

	//////////////////////////////////////////////////////////////////////
	// interface implementation
	//////////////////////////////////////////////////////////////////////

	/**
	 * returns a container for adding arbitrary additional information to
	 * a MATSim core object. The Container will be created on demand.
	 * 
	 * <p>IMPORTANT NOTE: This container is meant to use for experiments only.
	 * Therefore, do NOT call this method from the core (<tt>org.matsim</tt>).</p>
	 * 
	 * @see Customizable
	 * @return map for storing custom attributes
	 */
	@Override
	public final Map<String,Object> getCustomAttributes() {
		if (this.custom_attributes == null) {
			this.custom_attributes = new HashMap<>();
		}
		return this.custom_attributes;
	}
}
