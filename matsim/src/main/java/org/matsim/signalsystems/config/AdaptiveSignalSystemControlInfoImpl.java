/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAdaptiveSignalSystemControlInfoImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.signalsystems.config;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class AdaptiveSignalSystemControlInfoImpl implements
		AdaptiveSignalSystemControlInfo {

	private List<Id> signalGroupIds;
	private String adaptiveControlerClass;
	
	public void addSignalGroupId(Id id) {
		if (this.signalGroupIds == null){
			this.signalGroupIds = new ArrayList<Id>();
		}
		this.signalGroupIds.add(id);
	}

	public String getAdaptiveControlerClass() {
		return this.adaptiveControlerClass;
	}

	public List<Id> getSignalGroupIds() {
		return this.signalGroupIds;
	}

	public void setAdaptiveControlerClass(String adaptiveControler) {
		this.adaptiveControlerClass = adaptiveControler;
	}

}
