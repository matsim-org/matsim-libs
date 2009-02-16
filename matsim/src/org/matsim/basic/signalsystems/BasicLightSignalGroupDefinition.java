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

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.signalsystems.control.SignalSystemControler;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;

/**
 * @author dgrether
 */
public class BasicLightSignalGroupDefinition {
  
	private final Id id;
	private Id lightSignalSystemDefinitionId;
	private List<Id> laneIds;
	private List<Id> toLinkIds;
	private final Id linkRefId;
	
	private org.matsim.basic.signalsystems.control.SignalSystemControler signalSystemControler = null;

	public BasicLightSignalGroupDefinition(Id linkRefId, Id id) {
		this.linkRefId = linkRefId;
		this.id = id;
	}

	public void setLightSignalSystemDefinitionId(IdImpl id) {
		this.lightSignalSystemDefinitionId = id;
	}

	public void addLaneId(Id laneId) {
		if (this.laneIds == null)
			this.laneIds = new ArrayList<Id>();
		this.laneIds.add(laneId);
	}
	
	public Id getLinkRefId() {
		return linkRefId;
	}

	public void addToLinkId(Id linkId) {
		if (this.toLinkIds == null)
			this.toLinkIds = new ArrayList<Id>();
		this.toLinkIds.add(linkId);
	}

	public Id getId() {
		return id;
	}

	public Id getLightSignalSystemDefinitionId() {
		return lightSignalSystemDefinitionId;
	}

	public List<Id> getLaneIds() {
		return laneIds;
	}

	public List<Id> getToLinkIds() {
		return toLinkIds;
	}

	public void setResponsibleLSAControler(SignalSystemControler signalSystemControler) {
		this.signalSystemControler = signalSystemControler;		
	}
	
	public boolean isGreen(){
		return this.signalSystemControler.givenSignalGroupIsGreen(this);
	}
	
}
