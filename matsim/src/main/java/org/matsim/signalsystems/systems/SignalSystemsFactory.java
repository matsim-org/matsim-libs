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

package org.matsim.signalsystems.systems;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * Builder for Objects to be used within BasicSignalSystems container
 * Even if more a Factory this class is called builder to be consistent with other
 * MATSim container content builders which are in fact builder due to their more
 * complex build behavior. 
 * @author dgrether
 */
public class SignalSystemsFactory implements MatsimFactory {
	/**
	 * The constructor is only visible in the package because instances should 
	 * be retrieved from the container, i.e. an instance of BasicSignalSystems
	 * @see org.matsim.signalsystems.systems.SignalSystems#getFactory()
	 */
	SignalSystemsFactory(){}
	
	public SignalSystemDefinition createSignalSystemDefinition(
			Id signalSystemId) {
		return new SignalSystemDefinitionImpl(signalSystemId);
	}

	public SignalGroupDefinition createSignalGroupDefinition(
			Id linkRefId, Id signalGroupId) {
		return new SignalGroupDefinitionImpl(linkRefId, signalGroupId);
	}
	
}
