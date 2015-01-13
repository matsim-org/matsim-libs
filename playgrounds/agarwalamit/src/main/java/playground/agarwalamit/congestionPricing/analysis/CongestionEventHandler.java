/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.congestionPricing.analysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;

import playground.ikaddoura.internalizationCar.MarginalCongestionEvent;
import playground.ikaddoura.internalizationCar.MarginalCongestionEventHandler;

/**
 * @author amit
 */

public class CongestionEventHandler implements MarginalCongestionEventHandler, PersonMoneyEventHandler {

	private List<MarginalCongestionEvent> events = new ArrayList<MarginalCongestionEvent>();

	@Override
	public void reset(int iteration) {
		events.clear();
	}

	@Override
	public void handleEvent(MarginalCongestionEvent event) {
		events.add(event);
		System.out.println(event.toString());
	}

	public List<MarginalCongestionEvent> getCongestionEventsAsList(){
		return this.events;
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		System.out.println(event.toString());
	}
}
