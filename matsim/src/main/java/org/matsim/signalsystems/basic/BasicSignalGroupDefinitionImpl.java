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

package org.matsim.signalsystems.basic;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.control.SignalSystemController;

/**
 * @author dgrether
 */
public class BasicSignalGroupDefinitionImpl implements BasicSignalGroupDefinition {
  
	private final Id id;
	private Id lightSignalSystemDefinitionId;
	private List<Id> laneIds;
	private List<Id> toLinkIds;
	private final Id linkRefId;
	
	private org.matsim.signalsystems.control.SignalSystemController signalSystemControler = null;

	public BasicSignalGroupDefinitionImpl(Id linkRefId, Id id) {
		this.linkRefId = linkRefId;
		this.id = id;
	}

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#setSignalSystemDefinitionId(org.matsim.core.basic.v01.IdImpl)
	 */
	public void setSignalSystemDefinitionId(Id id) {
		this.lightSignalSystemDefinitionId = id;
	}

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#addLaneId(org.matsim.api.core.v01.Id)
	 */
	public void addLaneId(Id laneId) {
		if (this.laneIds == null)
			this.laneIds = new ArrayList<Id>();
		this.laneIds.add(laneId);
	}
	
	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#getLinkRefId()
	 */
	public Id getLinkRefId() {
		return linkRefId;
	}

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#addToLinkId(org.matsim.api.core.v01.Id)
	 */
	public void addToLinkId(Id linkId) {
		if (this.toLinkIds == null)
			this.toLinkIds = new ArrayList<Id>();
		this.toLinkIds.add(linkId);
	}

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#getId()
	 */
	public Id getId() {
		return id;
	}

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#getSignalSystemDefinitionId()
	 */
	public Id getSignalSystemDefinitionId() {
		return lightSignalSystemDefinitionId;
	}

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#getLaneIds()
	 */
	public List<Id> getLaneIds() {
		return laneIds;
	}

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#getToLinkIds()
	 */
	public List<Id> getToLinkIds() {
		return toLinkIds;
	}

	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#setResponsibleLSAControler(org.matsim.signalsystems.control.SignalSystemController)
	 */
	public void setResponsibleLSAControler(SignalSystemController signalSystemControler) {
		this.signalSystemControler = signalSystemControler;		
	}
	
	/**
	 * @see org.matsim.signalsystems.basic.BasicSignalGroupDefinition#isGreen()
	 */
	public boolean isGreen(double time){
		return this.signalSystemControler.givenSignalGroupIsGreen(time, this);
	}
	
}
