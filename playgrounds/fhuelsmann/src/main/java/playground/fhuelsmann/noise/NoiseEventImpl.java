/* *********************************************************************** *
 * project: org.matsim.*
 * NoiseTool.java
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
package playground.fhuelsmann.noise;

import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.EventImpl;

public class NoiseEventImpl extends EventImpl implements NoiseEvent {
	private final Id linkId;
	private final Map<String, Double> L_mE;

	public NoiseEventImpl(double time, Id linkId, Map<String, Double> L_mE) {
		super(time);
		this.linkId = linkId;
		this.L_mE = L_mE;
	}

	@Override
	public Id getLinkId() {
		// TODO Auto-generated method stub
		return linkId;
	}

	@Override
	public Map<String, Double> getL_mE() {
		return L_mE;
	}

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return NoiseEvent.EVENT_TYPE;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put(ATTRIBUTE_LINK_ID, this.linkId.toString());
		for (Entry<String, Double> entry : L_mE.entrySet()) {
			String timePeriode = entry.getKey();
			Double value = entry.getValue();
			attributes.put(timePeriode, value.toString());
		}
		return attributes;
	}

}