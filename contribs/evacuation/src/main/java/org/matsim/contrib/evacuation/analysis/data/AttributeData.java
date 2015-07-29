/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.analysis.data;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

public class AttributeData<T> {

	private HashMap<Id<?>, T> attributeData;

	public AttributeData() {
		this.attributeData = new HashMap<>();
	}

	public HashMap<Id<?>, T> getAttributeData() {
		return attributeData;
	}

	public void setAttributeData(HashMap<Id<?>, T> attributeData) {
		this.attributeData = attributeData;
	}

	public void setAttribute(Id<?> id, T data) {
		attributeData.put(id, data);
	}

	public <U> T getAttribute(Id<U> id) {
		return attributeData.get(id);
	}

}
