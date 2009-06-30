/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAdaptiveSignalSystemControlInfo
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


/**
 * @author dgrether
 *
 */
public interface BasicAdaptiveSignalSystemControlInfo extends BasicSignalSystemControlInfo{

	void setAdaptiveControlerClass(String adaptiveControler);

	void addSignalGroupId(Id id);
	
	List<Id> getSignalGroupIds();

	String getAdaptiveControlerClass();
	
	
}
