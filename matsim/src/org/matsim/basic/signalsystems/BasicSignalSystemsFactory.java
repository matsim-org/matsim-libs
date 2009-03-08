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

import org.matsim.basic.network.BasicLaneImpl;
import org.matsim.basic.network.BasicLanesToLinkAssignment;
import org.matsim.basic.network.BasicLanesToLinkAssignmentImpl;
import org.matsim.interfaces.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicSignalSystemsFactory {
	
	public BasicLanesToLinkAssignment createLanesToLinkAssignment(Id id) {
		return new BasicLanesToLinkAssignmentImpl(id);
	}

	public BasicLaneImpl createLane(Id id) {
		return new BasicLaneImpl(id);
	}

	public BasicSignalSystemDefinition createLightSignalSystemDefinition(
			Id id) {
		return new BasicSignalSystemDefinitionImpl(id);
	}

	public BasicSignalGroupDefinition createLightSignalGroupDefinition(
			Id linkRefId, Id id) {
		return new BasicSignalGroupDefinitionImpl(linkRefId, id);
	}
	
}
