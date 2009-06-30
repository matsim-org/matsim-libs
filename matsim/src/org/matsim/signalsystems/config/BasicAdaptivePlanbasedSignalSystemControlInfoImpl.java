/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAdaptivePlanbasedSignalSystemControlInfoImpl
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

import java.util.List;

import org.matsim.api.basic.v01.Id;



public class BasicAdaptivePlanbasedSignalSystemControlInfoImpl extends BasicPlanBasedSignalSystemControlInfoImpl implements
BasicAdaptivePlanBasedSignalSystemControlInfo {

	BasicAdaptiveSignalSystemControlInfo delegate;

	BasicAdaptivePlanbasedSignalSystemControlInfoImpl(){
		this.delegate = new BasicAdaptiveSignalSystemControlInfoImpl();
	}

	public void addSignalGroupId(Id id) {
		delegate.addSignalGroupId(id);
	}

	public String getAdaptiveControlerClass() {
		return delegate.getAdaptiveControlerClass();
	}

	public List<Id> getSignalGroupIds() {
		return delegate.getSignalGroupIds();
	}

	public void setAdaptiveControlerClass(String adaptiveControler) {
		delegate.setAdaptiveControlerClass(adaptiveControler);
	}
	
	
	
}
