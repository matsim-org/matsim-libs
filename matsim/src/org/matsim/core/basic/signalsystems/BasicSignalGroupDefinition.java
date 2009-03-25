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
package org.matsim.core.basic.signalsystems;

import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.signalsystems.control.SignalSystemControler;
/**
 * 
 * @author dgrether
 *
 */
public interface BasicSignalGroupDefinition {

	public void setLightSignalSystemDefinitionId(IdImpl id);

	public void addLaneId(Id laneId);

	public Id getLinkRefId();

	public void addToLinkId(Id linkId);

	public Id getId();

	public Id getLightSignalSystemDefinitionId();

	public List<Id> getLaneIds();

	public List<Id> getToLinkIds();

	public void setResponsibleLSAControler(
			SignalSystemControler signalSystemControler);

	public boolean isGreen();

}