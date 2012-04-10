/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import utilities.misc.DynamicDataXMLFileIO;

/**
 * Enables cadyts to persist the cost offsets to file.
 */
public class PtBseLinkCostOffsetsXMLFileIO extends DynamicDataXMLFileIO<TransitStopFacility> {

	private static final long serialVersionUID = 1L;
	private final TransitSchedule schedule;

	public PtBseLinkCostOffsetsXMLFileIO(final TransitSchedule schedule) {
		super();
		this.schedule = schedule;
	}

	@Override
	protected TransitStopFacility attrValue2key(final String stopId) {
//		System.out.println("-----attrValue2key------:\t" + stopId);
		TransitStopFacility stop = this.schedule.getFacilities().get(new IdImpl(stopId));
//		System.out.println("stop:\t" + stop);
		return stop;
	}

	@Override
	protected String key2attrValue(final TransitStopFacility key) {
		return key.getId().toString();
	}

}
