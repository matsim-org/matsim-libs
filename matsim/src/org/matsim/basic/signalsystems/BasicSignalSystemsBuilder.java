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

package org.matsim.basic.signalsystems;

import org.matsim.api.basic.v01.Id;

/**
 * Builder for Objects to be used within BasicSignalSystems container
 * Even if more a Factory t
 * his class is called builder to be consistent with ohter
 * MATSim container content builders which are in fact builder due to their more
 * complex build behavior. 
 * @author dgrether
 */
public class BasicSignalSystemsBuilder {
	
	BasicSignalSystemsBuilder(){}
	
	public BasicSignalSystemDefinition createLightSignalSystemDefinition(
			Id id) {
		return new BasicSignalSystemDefinitionImpl(id);
	}

	public BasicSignalGroupDefinition createLightSignalGroupDefinition(
			Id linkRefId, Id id) {
		return new BasicSignalGroupDefinitionImpl(linkRefId, id);
	}
	
}
